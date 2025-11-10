package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.port.outbound.aws.VmManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS VM 인스턴스 관리 어댑터
 * 
 * VmManagementPort 인터페이스를 구현하여 AWS VM 인스턴스의
 * 조회, 생성, 수정, 삭제, 생명주기 관리 기능을 제공합니다.
 * 
 * 핵사고날 아키텍처의 어댑터 계층에 해당하며, AWS SDK와 도메인 계층을 연결합니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmManagementAdapter implements VmManagementPort, ProviderScoped {

    private final Ec2Client ec2Client;
    private final AwsVmMapper mapper;

    // ==================== 인스턴스 조회 ====================

    @Override
    public Page<CloudResource> listInstances(VmQuery query) {
        try {
            log.debug("[AwsVmManagementAdapter] Listing VM instances with query: {}", query);
            
            DescribeInstancesRequest request = mapper.toDescribeInstancesRequest(query);
            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            List<CloudResource> resources = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .map(mapper::toCloudResource)
                .collect(Collectors.toList());

            // 페이징 처리
            Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), resources.size());
            
            List<CloudResource> pagedResources = resources.subList(start, end);
            
            log.debug("[AwsVmManagementAdapter] Found {} VM instances", resources.size());
            return new PageImpl<>(pagedResources, pageable, resources.size());

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to list VM instances", t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Optional<CloudResource> getInstance(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Getting VM instance: {}", instanceId);
            
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            
            Optional<CloudResource> result = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .findFirst()
                .map(mapper::toCloudResource);
            
            log.debug("[AwsVmManagementAdapter] Instance {} found: {}", instanceId, result.isPresent());
            return result;

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to get VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== 인스턴스 생성 ====================

    @Override
    public String createInstance(VmCreateRequest request) {
        try {
            log.debug("[AwsVmManagementAdapter] Creating VM instance with request: {}", request);
            
            RunInstancesRequest awsRequest = mapper.toRunInstancesRequest(request);
            RunInstancesResponse response = ec2Client.runInstances(awsRequest);
            
            String instanceId = response.instances().get(0).instanceId();
            log.info("[AwsVmManagementAdapter] Successfully created VM instance: {}", instanceId);
            
            return instanceId;

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to create VM instance", t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    @Override
    public void startInstance(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Starting VM instance: {}", instanceId);
            
            StartInstancesRequest request = StartInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            ec2Client.startInstances(request);
            log.info("[AwsVmManagementAdapter] Successfully started VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to start VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void stopInstance(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Stopping VM instance: {}", instanceId);
            
            StopInstancesRequest request = StopInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            ec2Client.stopInstances(request);
            log.info("[AwsVmManagementAdapter] Successfully stopped VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to stop VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void rebootInstance(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Rebooting VM instance: {}", instanceId);
            
            RebootInstancesRequest request = RebootInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            ec2Client.rebootInstances(request);
            log.info("[AwsVmManagementAdapter] Successfully rebooted VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to reboot VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void terminateInstance(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Terminating VM instance: {}", instanceId);
            
            TerminateInstancesRequest request = TerminateInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            ec2Client.terminateInstances(request);
            log.info("[AwsVmManagementAdapter] Successfully terminated VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to terminate VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void deleteInstance(VmDeleteRequest request) {
        try {
            log.debug("[AwsVmManagementAdapter] Deleting VM instance: {}", request.getInstanceId());
            
            TerminateInstancesRequest awsRequest = mapper.toTerminateInstancesRequest(request);
            ec2Client.terminateInstances(awsRequest);
            
            log.info("[AwsVmManagementAdapter] Successfully deleted VM instance: {}", request.getInstanceId());

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to delete VM instance: {}", request.getInstanceId(), t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== 인스턴스 수정 ====================

    @Override
    public void updateInstance(VmUpdateRequest request) {
        try {
            log.debug("[AwsVmManagementAdapter] Updating VM instance: {}", request.getInstanceId());
            
            ModifyInstanceAttributeRequest awsRequest = mapper.toModifyInstanceAttributeRequest(request);
            ec2Client.modifyInstanceAttribute(awsRequest);
            
            log.info("[AwsVmManagementAdapter] Successfully updated VM instance: {}", request.getInstanceId());

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to update VM instance: {}", request.getInstanceId(), t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== 태그 관리 ====================

    @Override
    public void addTags(String instanceId, Map<String, String> tags) {
        try {
            log.debug("[AwsVmManagementAdapter] Adding tags to VM instance: {} - tags: {}", instanceId, tags);
            
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
            log.info("[AwsVmManagementAdapter] Successfully added tags to VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to add tags to VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public void removeTags(String instanceId, Map<String, String> tagKeys) {
        try {
            log.debug("[AwsVmManagementAdapter] Removing tags from VM instance: {} - tag keys: {}", instanceId, tagKeys.keySet());
            
            List<String> keysToRemove = tagKeys.keySet().stream()
                .collect(Collectors.toList());
            
            DeleteTagsRequest request = DeleteTagsRequest.builder()
                .resources(instanceId)
                .tags(keysToRemove.stream()
                    .map(key -> Tag.builder().key(key).build())
                    .collect(Collectors.toList()))
                .build();
            
            ec2Client.deleteTags(request);
            log.info("[AwsVmManagementAdapter] Successfully removed tags from VM instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to remove tags from VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Map<String, String> getTags(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Getting tags for VM instance: {}", instanceId);
            
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
            
            log.debug("[AwsVmManagementAdapter] Found {} tags for VM instance: {}", tags.size(), instanceId);
            return tags;

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to get tags for VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== 인스턴스 상태 관리 ====================

    @Override
    public boolean waitForInstanceStatus(String instanceId, String status, int timeoutSeconds) {
        try {
            log.debug("[AwsVmManagementAdapter] Waiting for VM instance {} to reach status: {} (timeout: {}s)", 
                instanceId, status, timeoutSeconds);
            
            DescribeInstanceStatusRequest request = DescribeInstanceStatusRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            // TODO: 실제 대기 로직 구현 (폴링 또는 이벤트 기반)
            // 현재는 단순히 상태 확인만 수행
            ec2Client.describeInstanceStatus(request);
            
            log.info("[AwsVmManagementAdapter] VM instance {} status check completed", instanceId);
            return true; // 임시로 항상 성공 반환

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to wait for VM instance status: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public String getInstanceStatus(String instanceId) {
        try {
            log.debug("[AwsVmManagementAdapter] Getting status for VM instance: {}", instanceId);
            
            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                .instanceIds(instanceId)
                .build();
            
            DescribeInstancesResponse response = ec2Client.describeInstances(request);
            
            String status = response.reservations().stream()
                .flatMap(reservation -> reservation.instances().stream())
                .findFirst()
                .map(instance -> instance.state().nameAsString())
                .orElse("unknown");
            
            log.debug("[AwsVmManagementAdapter] VM instance {} status: {}", instanceId, status);
            return status;

        } catch (Throwable t) {
            log.error("[AwsVmManagementAdapter] Failed to get status for VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    // ==================== ProviderScoped 구현 ====================

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }
}
