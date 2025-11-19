package com.agenticcp.core.domain.cloud.adapter.outbound.aws.account;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.common.ProviderScoped;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.service.SessionCacheService;
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
    private final SessionCacheService sessionCacheService;
    private final CloudAccountRepository cloudAccountRepository;

    private static final int DEFAULT_SESSION_DURATION_SECONDS = 3600;

    @Override
    public ProviderType getProviderType() {
        return CloudProvider.ProviderType.AWS;
    }

    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        validateProvider(providerType);
        log.debug("[AwsAccountCredentialManagementAdapter] resolveCredentials - tenantKey={}, accountScope={}", tenantKey, accountScope);
        String credentialKey = findCredentialKey(tenantKey, providerType, accountScope);
        return awsCredentialManager.getCredentials(credentialKey);
    }

    @Override
    public String storeCredentials(String tenantKey, ProviderType providerType, String accountScope, Map<String, String> credentials) {
        validateProvider(providerType);
        log.debug("[AwsAccountCredentialManagementAdapter] storeCredentials - tenantKey={}, accountScope={}", tenantKey, accountScope);

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
        log.debug("[AwsAccountCredentialManagementAdapter] getSession - tenantKey={}, accountScope={}", tenantKey, accountScope);

        Optional<CloudSessionCredential> cachedSession = sessionCacheService.getCachedSession(
                tenantKey, accountScope, providerType);

        if (cachedSession.isPresent() && cachedSession.get().isValid()) {
            log.debug("[AwsAccountCredentialManagementAdapter] getSession - using cached session");
            return cachedSession.get();
        }

        String credentialKey = findCredentialKey(tenantKey, providerType, accountScope);
        CloudSessionCredential session = awsSessionProvider.getSession(credentialKey, DEFAULT_SESSION_DURATION_SECONDS);

        int ttlMinutes = calculateTtlMinutes(session);
        sessionCacheService.cacheSession(tenantKey, accountScope, providerType, session, ttlMinutes);

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
            return sessionCacheService.getDefaultTtlMinutes();
        }

        long minutesUntilExpiry = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                session.getExpiresAt()
        ).toMinutes();

        return Math.max(0, (int) minutesUntilExpiry - 5);
    }

    private String findCredentialKey(String tenantKey, ProviderType providerType, String accountScope) {
        return cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType)
                .stream()
                .filter(account -> account.getAccountScope() != null && account.getAccountScope().equals(accountScope))
                .findFirst()
                .map(account -> {
                    if (account.getCredential() == null) {
                        throw new BusinessException(
                                CloudErrorCode.ACCOUNT_NOT_FOUND,
                                "계정에 자격증명이 없습니다: " + accountScope
                        );
                    }
                    return account.getCredential().getCredentialKey();
                })
                .orElseThrow(() -> new BusinessException(
                        CloudErrorCode.ACCOUNT_NOT_FOUND,
                        "계정을 찾을 수 없습니다: tenantKey=" + tenantKey + ", providerType=" + providerType + ", accountScope=" + accountScope
                ));
    }
}
