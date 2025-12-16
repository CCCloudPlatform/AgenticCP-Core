package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.TagDescription;

/**
 * AWS VM 태그 어댑터
 * 
 * <p>JIT 세션 관리 패턴을 따릅니다:
 * - Service 레벨에서 세션을 획득하여 파라미터로 전달받음
 * - 전달받은 세션으로 EC2 클라이언트 생성
 * - Execute Around 패턴으로 클라이언트 생명주기 관리</p>
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmTaggingAdapter implements VmTaggingPort, ProviderScoped {

    private final AwsClientConfig awsClientConfig;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    // ==================== Execute Around Pattern ====================

    /**
     * Execute Around 패턴: 세션 기반 클라이언트 생성 → 작업 실행 → 클라이언트 해제
     * 
     * @param session 세션 자격증명 (Service에서 전달받음)
     * @param operation EC2 클라이언트를 사용하는 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    private <T> T executeWithEc2Client(CloudSessionCredential session, Function<Ec2Client, T> operation) {
        Ec2Client client = awsClientConfig.createEc2Client(session, null);
        
        try {
            return operation.apply(client);
        } finally {
            client.close();
        }
    }

    /**
     * 반환값이 없는 작업을 위한 Execute Around 패턴
     * 
     * @param session 세션 자격증명
     * @param operation EC2 클라이언트를 사용하는 작업
     */
    private void executeWithEc2ClientVoid(CloudSessionCredential session, java.util.function.Consumer<Ec2Client> operation) {
        Ec2Client client = awsClientConfig.createEc2Client(session, null);
        
        try {
            operation.accept(client);
        } finally {
            client.close();
        }
    }

    // ==================== Tagging Operations ====================

    @Override
    public void addTags(String instanceId, Map<String, String> tags, CloudSessionCredential session) {
        log.debug("[AwsVmTaggingAdapter] Adding tags to VM instance: {} - tags: {}", instanceId, tags);
        
        executeWithEc2ClientVoid(session, client -> {
            try {
                List<Tag> tagList = tags.entrySet().stream()
                        .map(entry -> Tag.builder()
                                .key(entry.getKey())
                                .value(entry.getValue())
                                .build())
                        .collect(Collectors.toList());

                CreateTagsRequest request = CreateTagsRequest.builder()
                        .resources(instanceId)
                        .tags(tagList)
                        .build();

                client.createTags(request);
                log.info("[AwsVmTaggingAdapter] Successfully added tags to VM instance: {}", instanceId);

            } catch (Throwable t) {
                log.error("[AwsVmTaggingAdapter] Failed to add tags to VM instance: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }

    @Override
    public void removeTags(String instanceId, Map<String, String> tagKeys, CloudSessionCredential session) {
        log.debug("[AwsVmTaggingAdapter] Removing tags from VM instance: {} - tag keys: {}", instanceId, tagKeys.keySet());
        
        executeWithEc2ClientVoid(session, client -> {
            try {
                DeleteTagsRequest request = DeleteTagsRequest.builder()
                        .resources(instanceId)
                        .tags(tagKeys.keySet().stream()
                                .map(key -> Tag.builder().key(key).build())
                                .collect(Collectors.toList()))
                        .build();

                client.deleteTags(request);
                log.info("[AwsVmTaggingAdapter] Successfully removed tags from VM instance: {}", instanceId);

            } catch (Throwable t) {
                log.error("[AwsVmTaggingAdapter] Failed to remove tags from VM instance: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }

    @Override
    public Map<String, String> getTags(String instanceId, CloudSessionCredential session) {
        log.debug("[AwsVmTaggingAdapter] Getting tags for VM instance: {}", instanceId);
        
        return executeWithEc2Client(session, client -> {
            try {
                DescribeTagsRequest request = DescribeTagsRequest.builder()
                        .filters(Filter.builder()
                                .name("resource-id")
                                .values(instanceId)
                                .build())
                        .build();

                DescribeTagsResponse response = client.describeTags(request);

                Map<String, String> tags = response.tags().stream()
                        .collect(Collectors.toMap(
                                TagDescription::key,
                                TagDescription::value
                        ));

                log.debug("[AwsVmTaggingAdapter] Found {} tags for VM instance: {}", tags.size(), instanceId);
                return tags;

            } catch (Throwable t) {
                log.error("[AwsVmTaggingAdapter] Failed to get tags for VM instance: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }
}
