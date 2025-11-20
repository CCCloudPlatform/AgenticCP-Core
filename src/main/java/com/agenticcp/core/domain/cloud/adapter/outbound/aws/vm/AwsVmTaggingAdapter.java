package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
import java.util.List;
import java.util.Map;
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
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmTaggingAdapter implements VmTaggingPort, ProviderScoped {

    private final Ec2Client ec2Client;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public void addTags(String instanceId, Map<String, String> tags) {
        try {
            log.debug("[AwsVmTaggingAdapter] Adding tags to VM instance: {} - tags: {}", instanceId, tags);

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

            ec2Client.createTags(request);
            log.info("[AwsVmTaggingAdapter] Successfully added tags to VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmTaggingAdapter] Failed to add tags to VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void removeTags(String instanceId, Map<String, String> tagKeys) {
        try {
            log.debug("[AwsVmTaggingAdapter] Removing tags from VM instance: {} - tag keys: {}", instanceId, tagKeys.keySet());

            DeleteTagsRequest request = DeleteTagsRequest.builder()
                    .resources(instanceId)
                    .tags(tagKeys.keySet().stream()
                            .map(key -> Tag.builder().key(key).build())
                            .collect(Collectors.toList()))
                    .build();

            ec2Client.deleteTags(request);
            log.info("[AwsVmTaggingAdapter] Successfully removed tags from VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmTaggingAdapter] Failed to remove tags from VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Map<String, String> getTags(String instanceId) {
        try {
            log.debug("[AwsVmTaggingAdapter] Getting tags for VM instance: {}", instanceId);

            DescribeTagsRequest request = DescribeTagsRequest.builder()
                    .filters(Filter.builder()
                            .name("resource-id")
                            .values(instanceId)
                            .build())
                    .build();

            DescribeTagsResponse response = ec2Client.describeTags(request);

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
    }
}

