package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.service.AwsCredentialManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 자격증명 제공 포트의 실제 구현체
 * 프로바이더 타입에 따라 적절한 자격증명 관리자에게 위임합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialProviderPortAdapter implements CredentialProviderPort {

    private final AwsCredentialManager awsCredentialManager;

    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        log.debug("[CredentialProviderPortAdapter] resolveCredentials - tenantKey={}, providerType={}, accountScope={}",
                tenantKey, providerType, accountScope);

        switch (providerType) {
            case AWS:
                return awsCredentialManager.getCredentials(accountScope);
            case AZURE:
            case GCP:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("프로바이더 타입 %s는 아직 지원되지 않습니다", providerType)
                );
            default:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("알 수 없는 프로바이더 타입입니다: %s", providerType)
                );
        }
    }

    @Override
    public String storeCredentials(String tenantKey, ProviderType providerType, 
                                   String accountScope, Map<String, String> credentials) {
        log.debug("[CredentialProviderPortAdapter] storeCredentials - tenantKey={}, providerType={}, accountScope={}",
                tenantKey, providerType, accountScope);

        switch (providerType) {
            case AWS:
                String accessKey = credentials.get("accessKeyId");
                String secretKey = credentials.get("secretAccessKey");
                String region = credentials.get("region");
                
                return awsCredentialManager.storeCredentials(tenantKey, accessKey, secretKey, region)
                        .getCredentialKey();
            case AZURE:
            case GCP:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("프로바이더 타입 %s는 아직 지원되지 않습니다", providerType)
                );
            default:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("알 수 없는 프로바이더 타입입니다: %s", providerType)
                );
        }
    }

    @Override
    public void deleteCredentials(ProviderType providerType, String credentialKey) {
        log.debug("[CredentialProviderPortAdapter] deleteCredentials - providerType={}, credentialKey={}", 
                providerType, credentialKey);
        
        switch (providerType) {
            case AWS:
                awsCredentialManager.deleteCredentials(credentialKey);
                break;
            case AZURE:
            case GCP:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("프로바이더 타입 %s는 아직 지원되지 않습니다", providerType)
                );
            default:
                throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("알 수 없는 프로바이더 타입입니다: %s", providerType)
                );
        }
    }
}

