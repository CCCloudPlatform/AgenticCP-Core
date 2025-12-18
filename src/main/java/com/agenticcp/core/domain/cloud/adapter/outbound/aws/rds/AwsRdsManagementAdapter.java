package com.agenticcp.core.domain.cloud.adapter.outbound.aws.rds;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsRdsConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsCreateCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsDeleteCommand;
import com.agenticcp.core.domain.cloud.port.model.rdbms.RdbmsUpdateCommand;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsManagementPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * AWS RDS 관리 어댑터
 * 
 * RDBMS 인스턴스의 생성, 수정, 삭제 기능을 제공합니다.
 * 모든 Management 작업은 세션 자격증명을 사용하여 요청별로 RDS 클라이언트를 생성합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsRdsManagementAdapter implements RdbmsManagementPort, ProviderScoped {

    private final AwsRdsConfig awsRdsConfig;
    private final AwsRdsMapper mapper;

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    @Override
    public CloudResource createRdbms(RdbmsCreateCommand command) {
        log.debug("[AwsRdsManagementAdapter] Creating RDBMS instance with command: instanceName={}, engine={}", 
            command.instanceName(), command.engine());
        
        RdsClient client = awsRdsConfig.createRdsClient(command.session(), command.region());

        try {
            // Command → AWS SDK Request 변환 (Adapter에서 직접 생성)
            CreateDbInstanceRequest request = buildCreateRequest(command);
            CreateDbInstanceResponse response = client.createDBInstance(request);
            
            String instanceIdentifier = response.dbInstance().dbInstanceIdentifier();
            log.info("[AwsRdsManagementAdapter] RDBMS instance creation initiated: {}", instanceIdentifier);
            
            // 인스턴스가 available 상태가 될 때까지 대기
            DBInstance dbInstance = waitForInstanceAvailable(client, instanceIdentifier);
            
            // CloudResource로 변환
            CloudResource resource = mapper.toCloudResource(
                dbInstance,
                command.providerType(), 
                command.serviceKey(), 
                command.region()
            );
            
            log.info("[AwsRdsManagementAdapter] Successfully created RDBMS instance: {}", instanceIdentifier);
            return resource;

        } catch (Throwable t) {
            log.error("[AwsRdsManagementAdapter] Failed to create RDBMS instance", t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }

    @Override
    public CloudResource updateRdbms(RdbmsUpdateCommand command) {
        log.debug("[AwsRdsManagementAdapter] Updating RDBMS instance: instanceId={}", 
            command.providerResourceId());
        
        RdsClient client = awsRdsConfig.createRdsClient(command.session(), command.region());
        
        try {
            // Command → AWS SDK Request 변환 (Adapter에서 직접 생성)
            ModifyDbInstanceRequest request = buildModifyRequest(command);
            ModifyDbInstanceResponse response = client.modifyDBInstance(request);
            
            String instanceIdentifier = response.dbInstance().dbInstanceIdentifier();
            log.info("[AwsRdsManagementAdapter] RDBMS instance modification initiated: {}", instanceIdentifier);
            
            // 인스턴스가 available 상태가 될 때까지 대기
            DBInstance dbInstance = waitForInstanceAvailable(client, instanceIdentifier);
            
            // CloudResource로 변환 (providerType과 serviceKey는 command에서 추출 필요)
            // TODO: command에 serviceKey 추가하거나 다른 방법으로 조회
            CloudResource resource = mapper.toCloudResource(
                dbInstance, 
                command.providerType(), 
                "RDS", // 기본값, command에 serviceKey가 없을 경우
                command.region()
            );
            
            log.info("[AwsRdsManagementAdapter] Successfully updated RDBMS instance: {}", instanceIdentifier);
            return resource;

        } catch (Throwable t) {
            log.error("[AwsRdsManagementAdapter] Failed to update RDBMS instance: {}", 
                command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }

    @Override
    public void deleteRdbms(RdbmsDeleteCommand command) {
        log.debug("[AwsRdsManagementAdapter] Deleting RDBMS instance: instanceId={}", 
            command.providerResourceId());
        
        RdsClient client = awsRdsConfig.createRdsClient(command.session(), command.region());
        
        try {
            // Command → AWS SDK Request 변환 (Adapter에서 직접 생성)
            DeleteDbInstanceRequest request = buildDeleteRequest(command);
            DeleteDbInstanceResponse response = client.deleteDBInstance(request);
            
            String instanceIdentifier = response.dbInstance().dbInstanceIdentifier();
            log.info("[AwsRdsManagementAdapter] RDBMS instance deletion initiated: {}", instanceIdentifier);
            
            // 삭제는 비동기로 진행되므로 대기하지 않음
            // 필요시 별도로 상태 확인 가능

        } catch (Throwable t) {
            log.error("[AwsRdsManagementAdapter] Failed to delete RDBMS instance: {}", 
                command.providerResourceId(), t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }
    
    /**
     * RDS 인스턴스가 available 상태가 될 때까지 대기합니다.
     * 
     * @param client RDS 클라이언트
     * @param instanceIdentifier 인스턴스 식별자
     * @return available 상태의 DBInstance
     */
    private DBInstance waitForInstanceAvailable(RdsClient client, String instanceIdentifier) {
        log.debug("[AwsRdsManagementAdapter] Waiting for instance to be available: {}", instanceIdentifier);
        
        int maxWaitMinutes = 30; // 최대 대기 시간 (분)
        int pollIntervalSeconds = 30; // 폴링 간격 (초)
        Instant startTime = Instant.now();
        
        while (true) {
            try {
                DescribeDbInstancesRequest request = DescribeDbInstancesRequest.builder()
                    .dbInstanceIdentifier(instanceIdentifier)
                    .build();

                DescribeDbInstancesResponse response = client.describeDBInstances(request);
                
                if (response.dbInstances().isEmpty()) {
                    throw new RuntimeException("DBInstance not found: " + instanceIdentifier);
                }
                
                DBInstance dbInstance = response.dbInstances().get(0);
                String status = dbInstance.dbInstanceStatus();
                
                log.debug("[AwsRdsManagementAdapter] Instance status: {} (waiting for available)", status);
                
                if ("available".equalsIgnoreCase(status)) {
                    log.info("[AwsRdsManagementAdapter] Instance is now available: {}", instanceIdentifier);
                    return dbInstance;
                }
                
                if ("failed".equalsIgnoreCase(status) || "deleted".equalsIgnoreCase(status)) {
                    throw new RuntimeException("Instance is in failed or deleted state: " + status);
                }
                
                // 타임아웃 확인
                Duration elapsed = Duration.between(startTime, Instant.now());
                if (elapsed.toMinutes() > maxWaitMinutes) {
                    throw new RuntimeException("Timeout waiting for instance to be available: " + instanceIdentifier);
                }
                
                // 대기
                Thread.sleep(pollIntervalSeconds * 1000L);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for instance to be available", e);
            } catch (Throwable t) {
                log.error("[AwsRdsManagementAdapter] Error while waiting for instance to be available", t);
                throw CloudErrorTranslator.translate(t);
            }
        }
    }
    
    // ==================== Command → AWS Request 변환 ====================
    
    /**
     * RdbmsCreateCommand를 AWS CreateDbInstanceRequest로 변환합니다.
     * 
     * CSP 중립 필드 → AWS 특화 필드 매핑:
     * - instanceName → dbInstanceIdentifier
     * - instanceSize → dbInstanceClass
     * - networkSecurityId → vpcSecurityGroupIds
     * - zone → availabilityZone
     * - highAvailability → multiAz
     */
    private CreateDbInstanceRequest buildCreateRequest(RdbmsCreateCommand command) {
        log.debug("[AwsRdsManagementAdapter] Building CreateDbInstanceRequest: instanceName={}, engine={}", 
            command.instanceName(), command.engine());
        
        CreateDbInstanceRequest.Builder builder = CreateDbInstanceRequest.builder()
            .dbInstanceIdentifier(command.instanceName())  // instanceName → dbInstanceIdentifier
            .engine(command.engine())
            .dbInstanceClass(command.instanceSize())  // instanceSize → dbInstanceClass
            .allocatedStorage(command.allocatedStorage())
            .masterUsername(command.adminUsername())
            .masterUserPassword(command.adminPassword())
            .publiclyAccessible(command.publiclyAccessible() != null ? command.publiclyAccessible() : false);
        
        // engineVersion
        if (command.engineVersion() != null) {
            builder.engineVersion(command.engineVersion());
        }
        
        // dbName
        if (command.dbName() != null) {
            builder.dbName(command.dbName());
        }
        
        // networkSecurityId → vpcSecurityGroupIds
        if (command.networkSecurityId() != null) {
            builder.vpcSecurityGroupIds(command.networkSecurityId());
        }
        
        // subnetId → dbSubnetGroupName (providerSpecificConfig에서 추출)
        String subnetGroupName = getSubnetGroupName(command);
        if (subnetGroupName != null) {
            builder.dbSubnetGroupName(subnetGroupName);
        }
        
        // port
        if (command.port() != null) {
            builder.port(command.port());
        }
        
        // zone → availabilityZone
        if (command.zone() != null) {
            builder.availabilityZone(command.zone());
        }
        
        // highAvailability → multiAZ
        if (command.highAvailability() != null) {
            builder.multiAZ(command.highAvailability());
        }
        
        // tags
        if (command.tags() != null && !command.tags().isEmpty()) {
            builder.tags(convertTagsToAwsTags(command.tags()));
        }
        
        return builder.build();
    }
    
    /**
     * RdbmsUpdateCommand를 AWS ModifyDbInstanceRequest로 변환합니다.
     */
    private ModifyDbInstanceRequest buildModifyRequest(RdbmsUpdateCommand command) {
        log.debug("[AwsRdsManagementAdapter] Building ModifyDbInstanceRequest: instanceId={}", 
            command.providerResourceId());
        
        ModifyDbInstanceRequest.Builder builder = ModifyDbInstanceRequest.builder()
            .dbInstanceIdentifier(command.providerResourceId());
        
        // instanceSize → dbInstanceClass
        if (command.instanceSize() != null) {
            builder.dbInstanceClass(command.instanceSize());
        }
        
        // allocatedStorage
        if (command.allocatedStorage() != null) {
            builder.allocatedStorage(command.allocatedStorage());
        }
        
        // adminPassword
        if (command.adminPassword() != null) {
            builder.masterUserPassword(command.adminPassword());
        }
        
        // applyImmediately
        if (command.applyImmediately() != null) {
            builder.applyImmediately(command.applyImmediately());
        }
        
        return builder.build();
    }
    
    /**
     * RdbmsDeleteCommand를 AWS DeleteDbInstanceRequest로 변환합니다.
     */
    private DeleteDbInstanceRequest buildDeleteRequest(RdbmsDeleteCommand command) {
        log.debug("[AwsRdsManagementAdapter] Building DeleteDbInstanceRequest: instanceId={}", 
            command.providerResourceId());
        
        DeleteDbInstanceRequest.Builder builder = DeleteDbInstanceRequest.builder()
            .dbInstanceIdentifier(command.providerResourceId());
        
        // skipSnapshot → skipFinalSnapshot
        if (command.skipSnapshot() != null) {
            builder.skipFinalSnapshot(command.skipSnapshot());
        }
        
        // snapshotName → finalDBSnapshotIdentifier
        if (command.snapshotName() != null && !command.skipSnapshot()) {
            builder.finalDBSnapshotIdentifier(command.snapshotName());
        }
        
        // deleteAutomatedBackups
        if (command.deleteAutomatedBackups() != null) {
            builder.deleteAutomatedBackups(command.deleteAutomatedBackups());
        }
        
        return builder.build();
    }
    
    /**
     * providerSpecificConfig에서 AWS 특화 설정 추출
     */
    private String getSubnetGroupName(RdbmsCreateCommand command) {
        if (command.providerSpecificConfig() != null) {
            Object subnetGroupName = command.providerSpecificConfig().get("subnetGroupName");
            return subnetGroupName != null ? subnetGroupName.toString() : null;
        }
        return null;
    }
    
    /**
     * Map을 AWS Tag 리스트로 변환합니다.
     */
    private List<software.amazon.awssdk.services.rds.model.Tag> convertTagsToAwsTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        return tags.entrySet().stream()
            .map(entry -> software.amazon.awssdk.services.rds.model.Tag.builder()
                .key(entry.getKey())
                .value(entry.getValue())
                .build())
            .collect(java.util.stream.Collectors.toList());
    }
}
