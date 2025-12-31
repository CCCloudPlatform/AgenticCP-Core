package com.agenticcp.core.domain.cloud.adapter.outbound.aws.rds;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsRdsConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsQuery;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsDiscoveryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AWS RDS 조회 어댑터
 *
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsRdsDiscoveryAdapter implements RdbmsDiscoveryPort, ProviderScoped {

    private final AwsRdsConfig awsRdsConfig;
    private final AwsRdsMapper mapper;

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    /**
     *
     * @param session 세션 자격증명
     * @param operation RDS 클라이언트를 사용하는 작업
     * @param <T> 반환 타입
     * @return 작업 결과
     */
    private <T> T executeWithRdsClient(CloudSessionCredential session, String region, Function<RdsClient, T> operation) {
        RdsClient client = awsRdsConfig.createRdsClient(session, region);
        
        try {
            return operation.apply(client);
        } finally {
            client.close();
        }
    }

    @Override
    public Page<CloudResource> listRdbmsInstances(RdbmsQuery query, CloudSessionCredential session) {
        log.debug("[AwsRdsDiscoveryAdapter] Listing RDBMS instances with query: {}", query);
        
        String region = query.getRegions() != null && !query.getRegions().isEmpty()
            ? query.getRegions().iterator().next()
            : null;
        
        return executeWithRdsClient(session, region, client -> {
            try {
                DescribeDbInstancesRequest request = buildDescribeRequest(query);
                DescribeDbInstancesResponse response = client.describeDBInstances(request);

                List<CloudResource> resources = response.dbInstances().stream()
                        .filter(dbInstance -> matchesQuery(dbInstance, query))
                        .map(dbInstance -> mapper.toCloudResource(
                            dbInstance, 
                            query.getProviderType(), 
                            "RDS", // 기본 서비스 키
                            region
                        ))
                        .collect(Collectors.toList());

                // 페이징 처리
                Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), resources.size());
                List<CloudResource> pagedResources = start < resources.size() 
                    ? resources.subList(start, end) 
                    : List.of();

                log.debug("[AwsRdsDiscoveryAdapter] Found {} RDBMS instances", resources.size());
                return new PageImpl<>(pagedResources, pageable, resources.size());

            } catch (Throwable t) {
                log.error("[AwsRdsDiscoveryAdapter] Failed to list RDBMS instances", t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }

    @Override
    public Optional<CloudResource> getRdbmsInstance(String instanceId, CloudSessionCredential session) {
        log.debug("[AwsRdsDiscoveryAdapter] Getting RDBMS instance: {}", instanceId);
        
        return executeWithRdsClient(session, null, client -> {
            try {
                DescribeDbInstancesRequest request = buildDescribeRequestById(instanceId);
                DescribeDbInstancesResponse response = client.describeDBInstances(request);

                Optional<CloudResource> result = response.dbInstances().stream()
                        .findFirst()
                        .map(dbInstance -> {
                            String region = extractRegionFromAvailabilityZone(dbInstance.availabilityZone());
                            return mapper.toCloudResource(
                                dbInstance, 
                                CloudProvider.ProviderType.AWS, 
                                "RDS", 
                                region
                            );
                        });

                log.debug("[AwsRdsDiscoveryAdapter] Instance {} found: {}", instanceId, result.isPresent());
                return result;

            } catch (Throwable t) {
                log.error("[AwsRdsDiscoveryAdapter] Failed to get RDBMS instance: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }

    @Override
    public String getInstanceStatus(String instanceId, CloudSessionCredential session) {
        log.debug("[AwsRdsDiscoveryAdapter] Getting status for RDBMS instance: {}", instanceId);
        
        return executeWithRdsClient(session, null, client -> {
            try {
                DescribeDbInstancesRequest request = buildDescribeRequestById(instanceId);
                DescribeDbInstancesResponse response = client.describeDBInstances(request);

                String status = response.dbInstances().stream()
                        .findFirst()
                        .map(DBInstance::dbInstanceStatus)
                        .orElse("unknown");

                log.debug("[AwsRdsDiscoveryAdapter] RDBMS instance {} status: {}", instanceId, status);
                return status;

            } catch (Throwable t) {
                log.error("[AwsRdsDiscoveryAdapter] Failed to get status for RDBMS instance: {}", instanceId, t);
                throw CloudErrorTranslator.translate(t);
            }
        });
    }

    /**
     * DBInstance가 쿼리 조건과 일치하는지 확인합니다.
     */
    private boolean matchesQuery(DBInstance dbInstance, RdbmsQuery query) {
        // instanceName 필터
        if (query.getInstanceName() != null) {
            if (!dbInstance.dbInstanceIdentifier().equals(query.getInstanceName())) {
                return false;
            }
        }
        
        // engine 필터
        if (query.getEngine() != null) {
            if (!dbInstance.engine().equalsIgnoreCase(query.getEngine())) {
                return false;
            }
        }
        
        // instanceSize 필터
        if (query.getInstanceSize() != null) {
            if (!dbInstance.dbInstanceClass().equals(query.getInstanceSize())) {
                return false;
            }
        }
        
        // status 필터
        if (query.getStatus() != null) {
            if (!dbInstance.dbInstanceStatus().equalsIgnoreCase(query.getStatus())) {
                return false;
            }
        }
        
        // tags 필터 (간단한 구현, 향후 개선 가능)
        // TODO: 태그 필터링 로직 추가
        
        return true;
    }
    
    /**
     * Availability Zone에서 리전을 추출합니다.
     * 예: "us-east-1a" -> "us-east-1"
     */
    private String extractRegionFromAvailabilityZone(String availabilityZone) {
        if (availabilityZone == null || availabilityZone.isEmpty()) {
            return null;
        }
        
        if (availabilityZone.length() > 1) {
            return availabilityZone.substring(0, availabilityZone.length() - 1);
        }
        
        return availabilityZone;
    }
    
    /**
     * RdbmsQuery를 AWS DescribeDbInstancesRequest로 변환합니다.
     */
    private DescribeDbInstancesRequest buildDescribeRequest(RdbmsQuery query) {
        log.debug("[AwsRdsDiscoveryAdapter] Building DescribeDbInstancesRequest from query");
        
        DescribeDbInstancesRequest.Builder builder = DescribeDbInstancesRequest.builder();
        
        if (query.getInstanceName() != null) {
            builder.dbInstanceIdentifier(query.getInstanceName());
        }
        
        return builder.build();
    }
    
    /**
     * 인스턴스 ID로 AWS DescribeDbInstancesRequest를 생성합니다.
     */
    private DescribeDbInstancesRequest buildDescribeRequestById(String instanceId) {
        log.debug("[AwsRdsDiscoveryAdapter] Building DescribeDbInstancesRequest for instanceId: {}", instanceId);
        
        return DescribeDbInstancesRequest.builder()
                .dbInstanceIdentifier(instanceId)
                .build();
    }
}
