package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.repository.SessionCacheRepository;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AwsAccountCredentialManagementAdapter implements AccountCredentialManagementPort, ProviderScoped {

    private final AwsCredentialManager awsCredentialManager;
    private final AwsSessionProvider awsSessionProvider;
    private final SessionCacheRepository sessionCachePort;
    private final CloudAccountRepository cloudAccountRepository;
    private final MaskingService maskingService;

    private static final int DEFAULT_SESSION_DURATION_SECONDS = 3600;

    @Override
    public ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        validateProvider(providerType);
        String maskedTenantKey = maskingService.maskTenantKey(tenantKey);
        String maskedAccountScope = maskingService.maskAccountScope(accountScope);
        log.debug("[AwsAccountCredentialManagementAdapter] resolveCredentials - tenantKey={}, accountScope={}", maskedTenantKey, maskedAccountScope);
        String credentialKey = findCredentialKey(tenantKey, providerType, accountScope);
        return awsCredentialManager.getCredentials(credentialKey);
    }

    @Override
    public String storeCredentials(String tenantKey, ProviderType providerType, String accountScope, Map<String, String> credentials) {
        validateProvider(providerType);
        String maskedTenantKey = maskingService.maskTenantKey(tenantKey);
        String maskedAccountScope = maskingService.maskAccountScope(accountScope);
        log.debug("[AwsAccountCredentialManagementAdapter] storeCredentials - tenantKey={}, accountScope={}", maskedTenantKey, maskedAccountScope);

        String accessKey = credentials.get("accessKeyId");
        String secretKey = credentials.get("secretAccessKey");
        String region = credentials.get("region");

        return awsCredentialManager.storeCredentials(tenantKey, accessKey, secretKey, region)
                .getCredentialKey();
    }

    @Override
    public void deleteCredentials(ProviderType providerType, String credentialKey) {
        validateProvider(providerType);
        log.debug("[AwsAccountCredentialManagementAdapter] deleteCredentials - credentialKey={}", credentialKey);
        awsCredentialManager.deleteCredentials(credentialKey);
    }

    @Override
    public CloudSessionCredential getSession(String tenantKey, String accountScope, ProviderType providerType) {
        validateProvider(providerType);
        String maskedTenantKey = maskingService.maskTenantKey(tenantKey);
        String maskedAccountScope = maskingService.maskAccountScope(accountScope);
        log.debug("[AwsAccountCredentialManagementAdapter] getSession - tenantKey={}, accountScope={}", maskedTenantKey, maskedAccountScope);

        Optional<CloudSessionCredential> cachedSession = sessionCachePort.getCachedSession(
                tenantKey, accountScope, providerType);

        if (cachedSession.isPresent() && cachedSession.get().isValid()) {
            log.debug("[AwsAccountCredentialManagementAdapter] getSession - using cached session");
            return cachedSession.get();
        }

        String credentialKey = findCredentialKey(tenantKey, providerType, accountScope);
        CloudSessionCredential session = awsSessionProvider.getSession(credentialKey, DEFAULT_SESSION_DURATION_SECONDS);

        int ttlMinutes = calculateTtlMinutes(session);
        sessionCachePort.cacheSession(tenantKey, accountScope, providerType, session, ttlMinutes);

        log.info("[AwsAccountCredentialManagementAdapter] getSession - session issued and cached, expiresAt={}",
                session.getExpiresAt());

        return session;
    }

    private void validateProvider(ProviderType providerType) {
        if (providerType != ProviderType.AWS) {
            throw new BusinessException(
                    CloudErrorCode.UNSUPPORTED_OPERATION,
                    String.format("AWS 어댑터는 AWS 프로바이더만 지원합니다. 요청된 프로바이더: %s", providerType)
            );
        }
    }

    private int calculateTtlMinutes(CloudSessionCredential session) {
        if (session.getExpiresAt() == null) {
            return sessionCachePort.getDefaultTtlMinutes();
        }

        long minutesUntilExpiry = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                session.getExpiresAt()
        ).toMinutes();

        return Math.max(0, (int) minutesUntilExpiry - 5);
    }

    /**
     * Credential Key를 직접 조회합니다.
     * LazyInitializationException을 방지하기 위해 Repository의 JOIN 쿼리를 사용합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param providerType 프로바이더 타입
     * @param accountScope 계정 범위
     * @return 자격증명 키
     * @throws BusinessException 계정을 찾을 수 없는 경우
     */
    private String findCredentialKey(String tenantKey, ProviderType providerType, String accountScope) {
        return cloudAccountRepository.findCredentialKeyByTenantKeyAndProviderTypeAndAccountScope(
                        tenantKey, providerType, accountScope)
                .orElseThrow(() -> new BusinessException(
                        CloudErrorCode.ACCOUNT_NOT_FOUND,
                        "계정을 찾을 수 없습니다: tenantKey=" + tenantKey + ", providerType=" + providerType + ", accountScope=" + accountScope
                ));
    }
}
