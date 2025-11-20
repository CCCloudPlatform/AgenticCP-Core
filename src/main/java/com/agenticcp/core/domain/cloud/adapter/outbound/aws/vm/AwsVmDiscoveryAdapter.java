package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstanceStatusRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AWS VM 조회 어댑터
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmDiscoveryAdapter implements VmDiscoveryPort, ProviderScoped {

    private final Ec2Client ec2Client;
    private final AwsVmMapper mapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    @Override
    public Page<CloudResource> listInstances(VmQuery query) {
        try {
            log.debug("[AwsVmDiscoveryAdapter] Listing VM instances with query: {}", query);

            DescribeInstancesRequest request = mapper.toDescribeInstancesRequest(query);
            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            List<CloudResource> resources = response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .map(mapper::toCloudResource)
                    .collect(Collectors.toList());

            Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), resources.size());
            List<CloudResource> pagedResources = resources.subList(start, end);

            log.debug("[AwsVmDiscoveryAdapter] Found {} VM instances", resources.size());
            return new PageImpl<>(pagedResources, pageable, resources.size());

        } catch (Throwable t) {
            log.error("[AwsVmDiscoveryAdapter] Failed to list VM instances", t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public Optional<CloudResource> getInstance(String instanceId) {
        try {
            log.debug("[AwsVmDiscoveryAdapter] Getting VM instance: {}", instanceId);

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            Optional<CloudResource> result = response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .findFirst()
                    .map(mapper::toCloudResource);

            log.debug("[AwsVmDiscoveryAdapter] Instance {} found: {}", instanceId, result.isPresent());
            return result;

        } catch (Throwable t) {
            log.error("[AwsVmDiscoveryAdapter] Failed to get VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public String getInstanceStatus(String instanceId) {
        try {
            log.debug("[AwsVmDiscoveryAdapter] Getting status for VM instance: {}", instanceId);

            DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            DescribeInstancesResponse response = ec2Client.describeInstances(request);

            String status = response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .findFirst()
                    .map(instance -> instance.state().nameAsString())
                    .orElse("unknown");

            log.debug("[AwsVmDiscoveryAdapter] VM instance {} status: {}", instanceId, status);
            return status;

        } catch (Throwable t) {
            log.error("[AwsVmDiscoveryAdapter] Failed to get status for VM instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }

    @Override
    public boolean waitForInstanceStatus(String instanceId, String status, int timeoutSeconds) {
        try {
            log.debug("[AwsVmDiscoveryAdapter] Waiting for VM instance {} to reach status: {} (timeout: {}s)",
                    instanceId, status, timeoutSeconds);

            DescribeInstanceStatusRequest request = DescribeInstanceStatusRequest.builder()
                    .instanceIds(instanceId)
                    .build();

            ec2Client.describeInstanceStatus(request);

            log.info("[AwsVmDiscoveryAdapter] VM instance {} status check completed", instanceId);
            return true;

        } catch (Throwable t) {
            log.error("[AwsVmDiscoveryAdapter] Failed to wait for VM instance status: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        }
    }
}
