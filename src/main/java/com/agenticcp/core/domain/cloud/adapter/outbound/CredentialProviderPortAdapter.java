package com.agenticcp.core.domain.cloud.adapter.outbound;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsSessionProvider;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.repository.CloudAccountRepository;
import com.agenticcp.core.domain.cloud.adapter.outbound.aws.AwsCredentialManager;
import com.agenticcp.core.domain.cloud.service.SessionCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

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
    private final AwsSessionProvider awsSessionProvider;
    private final SessionCacheService sessionCacheService;
    private final CloudAccountRepository cloudAccountRepository;

    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        log.debug("[CredentialProviderPortAdapter] resolveCredentials - tenantKey={}, providerType={}, accountScope={}",
                tenantKey, providerType, accountScope);

        // accountScope로 CloudAccount를 찾아서 credentialKey 획득
        String credentialKey = cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType)
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

        switch (providerType) {
            case AWS:
                return awsCredentialManager.getCredentials(credentialKey);
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
    
    @Override
    public CloudSessionCredential getSession(String tenantKey, String accountScope, ProviderType providerType) {
        log.debug("[CredentialProviderPortAdapter] getSession - tenantKey={}, accountScope={}, providerType={}",
                tenantKey, accountScope, providerType);
        
        // 1. Redis 캐시에서 세션 조회
        Optional<CloudSessionCredential> cachedSession = sessionCacheService.getCachedSession(
                tenantKey, accountScope, providerType);
        
        if (cachedSession.isPresent() && cachedSession.get().isValid()) {
            log.debug("[CredentialProviderPortAdapter] getSession - using cached session");
            return cachedSession.get();
        }
        
        // 2. 캐시에 없거나 만료된 경우 새로 발급
        // accountScope로 CloudAccount를 찾아서 credentialKey 획득
        String credentialKey = cloudAccountRepository.findByTenantKeyAndProviderType(tenantKey, providerType)
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
        
        CloudSessionCredential session;
        switch (providerType) {
            case AWS:
                // AWS STS 세션 발급 (1시간 유효)
                session = awsSessionProvider.getSession(credentialKey, 3600);
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
        
        // 3. Redis에 캐싱 (TTL: 세션 만료 5분 전까지)
        int ttlMinutes = calculateTtlMinutes(session);
        sessionCacheService.cacheSession(tenantKey, accountScope, providerType, session, ttlMinutes);
        
        log.info("[CredentialProviderPortAdapter] getSession - session issued and cached, expiresAt={}",
                session.getExpiresAt());
        
        return session;
    }
    
    /**
     * 세션의 TTL을 계산합니다 (분 단위).
     * 세션 만료 5분 전까지 캐싱합니다.
     * 
     * @param session 세션 자격증명
     * @return TTL (분)
     */
    private int calculateTtlMinutes(CloudSessionCredential session) {
        if (session.getExpiresAt() == null) {
            return sessionCacheService.getDefaultTtlMinutes();
        }
        
        long minutesUntilExpiry = java.time.Duration.between(
                java.time.LocalDateTime.now(),
                session.getExpiresAt()
        ).toMinutes();
        
        // 만료 5분 전까지만 캐싱
        return Math.max(0, (int) minutesUntilExpiry - 5);
    }
}

