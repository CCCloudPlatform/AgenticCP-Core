package com.agenticcp.core.domain.cloud.adapter.outbound.aws.vm;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsClientConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.common.context.TenantContextHolder;
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
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AWS VM 조회 어댑터
 * 
 * <p>Discovery 작업은 Execute Around 패턴을 사용하여 JIT(Just-In-Time) 세션을 획득합니다.
 * 어댑터 내부에서 세션을 획득하고 클라이언트를 생성/해제합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsVmDiscoveryAdapter implements VmDiscoveryPort, ProviderScoped {

    private static final String DEFAULT_ACCOUNT_SCOPE = "default";
    
    private final AwsClientConfig awsClientConfig;
    private final AccountCredentialManagementPort credentialPort;
    private final AwsVmMapper mapper;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.AWS;
    }

    // ==================== Execute Around Pattern ====================

    /**
     * Execute Around 패턴: JIT 세션 획득 → 클라이언트 생성 → 작업 실행 → 클라이언트 해제
     * 
     * @param operation EC2 클라이언트를 사용하는 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    private <T> T executeWithEc2Client(Function<Ec2Client, T> operation) {
        String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
        CloudSessionCredential session = credentialPort.getSession(tenantKey, DEFAULT_ACCOUNT_SCOPE, ProviderType.AWS);
        Ec2Client client = awsClientConfig.createEc2Client(session, null);
        
        try {
            return operation.apply(client);
        } finally {
            client.close();
        }
    }

    // ==================== Discovery Operations ====================

    @Override
    public Page<CloudResource> listInstances(VmQuery query) {
        log.debug("[AwsVmDiscoveryAdapter] Listing VM instances with query: {}", query);
        
        return executeWithEc2Client(client -> {
            try {
                DescribeInstancesRequest request = mapper.toDescribeInstancesRequest(query);
                DescribeInstancesResponse response = client.describeInstances(request);

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
        });
    }

    @Override
    public Optional<CloudResource> getInstance(String instanceId) {
        log.debug("[AwsVmDiscoveryAdapter] Getting VM instance: {}", instanceId);
        
        return executeWithEc2Client(client -> {
            try {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build();

                DescribeInstancesResponse response = client.describeInstances(request);

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
        });
    }

    @Override
    public String getInstanceStatus(String instanceId) {
        log.debug("[AwsVmDiscoveryAdapter] Getting status for VM instance: {}", instanceId);
        
        return executeWithEc2Client(client -> {
            try {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .instanceIds(instanceId)
                        .build();

                DescribeInstancesResponse response = client.describeInstances(request);

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
        });
    }

    @Override
    public boolean waitForInstanceStatus(String instanceId, String status, int timeoutSeconds) {
        log.debug("[AwsVmDiscoveryAdapter] Waiting for VM instance {} to reach status: {} (timeout: {}s)",
                instanceId, status, timeoutSeconds);
        
        return executeWithEc2Client(client -> {
            try {
                DescribeInstanceStatusRequest request = DescribeInstanceStatusRequest.builder()
                        .instanceIds(instanceId)
                        .build();

                client.describeInstanceStatus(request);

                log.info("[AwsVmDiscoveryAdapter] VM instance {} status check completed", instanceId);
                return true;

            } catch (Throwable t) {
                log.error("[AwsVmDiscoveryAdapter] Failed to wait for VM instance status: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }
}
