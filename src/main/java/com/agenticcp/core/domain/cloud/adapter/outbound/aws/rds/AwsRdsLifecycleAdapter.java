package com.agenticcp.core.domain.cloud.adapter.outbound.aws.rds;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.config.AwsRdsConfig;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.CloudErrorTranslator;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.outbound.rdbms.RdbmsLifecyclePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.*;

/**
 * AWS RDS 생명주기 어댑터
 * 
 * RDBMS 인스턴스의 시작, 중지, 재시작 기능을 제공합니다.
 * 모든 Lifecycle 작업은 세션 자격증명을 사용하여 요청별로 RDS 클라이언트를 생성합니다.
 * 
 * RdbmsLifecyclePort를 구현하여 ResourceLifecyclePort의 start, stop, terminate와
 * RDBMS 특화 기능인 reboot를 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class AwsRdsLifecycleAdapter implements RdbmsLifecyclePort, ProviderScoped {

    private final AwsRdsConfig awsRdsConfig;

    @Override
    public CloudProvider.ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    // ==================== ResourceLifecyclePort 구현 (RdbmsLifecyclePort를 통해 상속) ====================

    @Override
    public void start(ResourceIdentity id, CloudSessionCredential session) {
        String instanceId = id.getProviderResourceId();
        log.debug("[AwsRdsLifecycleAdapter] Starting RDBMS instance via ResourceLifecyclePort: {}", instanceId);

        RdsClient client = awsRdsConfig.createRdsClient(session, id.getRegion());
        
        try {
            log.warn("[AwsRdsLifecycleAdapter] RDS does not support startInstance operation. " +
                    "RDS instances cannot be stopped and started like EC2 instances. " +
                    "Use modifyDBInstance to change instance configuration instead.");
            
            throw new UnsupportedOperationException(
                "AWS RDS does not support startInstance operation. " +
                "RDS instances are always running when available. " +
                "To resume a stopped instance, use modifyDBInstance or wait for automatic resume."
            );

        } catch (Throwable t) {
            log.error("[AwsRdsLifecycleAdapter] Failed to start RDBMS instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }

    @Override
    public void stop(ResourceIdentity id, CloudSessionCredential session) {
        String instanceId = id.getProviderResourceId();
        log.debug("[AwsRdsLifecycleAdapter] Stopping RDBMS instance via ResourceLifecyclePort: {}", instanceId);
        
        RdsClient client = awsRdsConfig.createRdsClient(session, id.getRegion());
        
        try {
            StopDbInstanceRequest request = buildStopRequest(instanceId);
            client.stopDBInstance(request);
            
            log.info("[AwsRdsLifecycleAdapter] Successfully stopped RDBMS instance: {}", instanceId);

        } catch (Throwable t) {
            log.error("[AwsRdsLifecycleAdapter] Failed to stop RDBMS instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }

    @Override
    public void terminate(ResourceIdentity id, CloudSessionCredential session) {
        String instanceId = id.getProviderResourceId();
        log.debug("[AwsRdsLifecycleAdapter] Terminating RDBMS instance via ResourceLifecyclePort: {}", instanceId);
        
        // RDS의 경우 terminate는 delete와 동일하지만, 실제로는 RdbmsManagementPort의 deleteRdbms를 사용해야 합니다.
        // 여기서는 UnsupportedOperationException을 던지거나, 실제 삭제 로직을 구현할 수 있습니다.
        log.warn("[AwsRdsLifecycleAdapter] terminate() is called for RDS instance. " +
                "RDS termination should be handled through RdbmsManagementPort.deleteRdbms() instead.");
        
        throw new UnsupportedOperationException(
            "RDS 인스턴스 종료는 RdbmsManagementPort.deleteRdbms()를 통해 처리해야 합니다."
        );
    }

    // ==================== RdbmsLifecyclePort 구현 (reboot 추가 기능) ====================

    @Override
    public void rebootInstance(String instanceId, CloudSessionCredential session) {
        log.debug("[AwsRdsLifecycleAdapter] Rebooting RDBMS instance: {}", instanceId);
        
        RdsClient client = awsRdsConfig.createRdsClient(session, null);
        
        try {
            // Command → AWS SDK Request 변환 (Adapter에서 직접 생성)
            RebootDbInstanceRequest request = buildRebootRequest(instanceId);
            RebootDbInstanceResponse response = client.rebootDBInstance(request);
            
            log.info("[AwsRdsLifecycleAdapter] Successfully rebooted RDBMS instance: {}", instanceId);
            log.debug("[AwsRdsLifecycleAdapter] Reboot operation initiated for instance: {}", 
                response.dbInstance().dbInstanceIdentifier());

        } catch (Throwable t) {
            log.error("[AwsRdsLifecycleAdapter] Failed to reboot RDBMS instance: {}", instanceId, t);
            throw CloudErrorTranslator.translate(t);
        } finally {
            client.close();
        }
    }

    // ==================== Private Helper Methods ====================

    /**
     * 인스턴스 ID로 AWS StopDbInstanceRequest를 생성합니다.
     */
    private StopDbInstanceRequest buildStopRequest(String instanceId) {
        log.debug("[AwsRdsLifecycleAdapter] Building StopDbInstanceRequest for instanceId: {}", instanceId);
        
        return StopDbInstanceRequest.builder()
                .dbInstanceIdentifier(instanceId)
                .build();
    }
    
    /**
     * 인스턴스 ID로 AWS RebootDbInstanceRequest를 생성합니다.
     */
    private RebootDbInstanceRequest buildRebootRequest(String instanceId) {
        log.debug("[AwsRdsLifecycleAdapter] Building RebootDbInstanceRequest for instanceId: {}", instanceId);
        
        return RebootDbInstanceRequest.builder()
                .dbInstanceIdentifier(instanceId)
                .build();
    }
}
