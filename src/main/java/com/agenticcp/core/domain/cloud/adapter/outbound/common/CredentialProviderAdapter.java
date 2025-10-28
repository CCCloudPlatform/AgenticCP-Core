package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.common.crypto.EncryptionService;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 자격증명 제공자 어댑터
 * 
 * CredentialProviderPort 인터페이스를 구현하여 클라우드 제공업체별 자격증명을 제공합니다.
 * 암호화된 자격증명을 안전하게 관리하고 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredentialProviderAdapter implements CredentialProviderPort {

    private final EncryptionService encryptionService;
    
    @Value("${aws.enabled:false}")
    private boolean awsEnabled;
    
    @Value("${aws.access-key-id:}")
    private String awsAccessKeyId;
    
    @Value("${aws.secret-access-key:}")
    private String awsSecretAccessKey;
    
    @Value("${aws.session-token:}")
    private String awsSessionToken;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    // 자격증명 캐시 (메모리 기반, 실제 운영에서는 Redis 등 사용 권장)
    private final Map<String, Object> credentialCache = new ConcurrentHashMap<>();

    @Override
    public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        try {
            log.debug("[CredentialProvider] Resolving credentials for tenant: {}, provider: {}, account: {}", 
                    tenantKey, providerType, accountScope);
            
            // 캐시 키 생성
            String cacheKey = generateCacheKey(tenantKey, providerType, accountScope);
            
            // 캐시에서 확인
            Object cachedCredentials = credentialCache.get(cacheKey);
            if (cachedCredentials != null) {
                log.debug("[CredentialProvider] Using cached credentials for key: {}", cacheKey);
                return cachedCredentials;
            }
            
            // 자격증명 해결
            Object credentials = createCredentials(tenantKey, providerType, accountScope);
            
            // 캐시에 저장
            credentialCache.put(cacheKey, credentials);
            
            log.debug("[CredentialProvider] Credentials resolved and cached for key: {}", cacheKey);
            return credentials;
            
        } catch (Exception e) {
            log.error("[CredentialProvider] Failed to resolve credentials for tenant: {}, provider: {}, account: {}, error: {}", 
                    tenantKey, providerType, accountScope, e.getMessage(), e);
            throw new RuntimeException("자격증명 해결 실패: " + e.getMessage(), e);
        }
    }
    
    /**
     * 캐시 키 생성
     */
    private String generateCacheKey(String tenantKey, ProviderType providerType, String accountScope) {
        return String.format("%s:%s:%s", tenantKey, providerType, accountScope);
    }
    
    /**
     * 제공업체별 자격증명 생성
     */
    private Object createCredentials(String tenantKey, ProviderType providerType, String accountScope) {
        switch (providerType) {
            case AWS:
                return createAwsCredentials(tenantKey, accountScope);
            case AZURE:
                return createAzureCredentials(tenantKey, accountScope);
            case GCP:
                return createGcpCredentials(tenantKey, accountScope);
            default:
                log.warn("[CredentialProvider] Unsupported provider type: {}", providerType);
                throw new IllegalArgumentException("지원되지 않는 클라우드 제공업체: " + providerType);
        }
    }
    
    /**
     * AWS 자격증명 생성
     */
    private AwsCredentials createAwsCredentials(String tenantKey, String accountScope) {
        if (!awsEnabled) {
            log.warn("[CredentialProvider] AWS is disabled, using default credentials");
            return new AwsCredentials("default-access-key", "default-secret-key", awsRegion);
        }
        
        // 실제 운영에서는 테넌트별로 다른 자격증명을 사용해야 함
        // 현재는 설정에서 가져온 자격증명을 사용
        String accessKey = decryptIfEncrypted(awsAccessKeyId);
        String secretKey = decryptIfEncrypted(awsSecretAccessKey);
        String sessionToken = decryptIfEncrypted(awsSessionToken);
        
        log.debug("[CredentialProvider] Created AWS credentials for tenant: {}, account: {}", tenantKey, accountScope);
        return new AwsCredentials(accessKey, secretKey, awsRegion, sessionToken);
    }
    
    /**
     * Azure 자격증명 생성
     */
    private AzureCredentials createAzureCredentials(String tenantKey, String accountScope) {
        // TODO: Azure 자격증명 관리 시스템과 연동
        // 현재는 기본 자격증명 반환
        log.debug("[CredentialProvider] Created Azure credentials for tenant: {}, account: {}", tenantKey, accountScope);
        return new AzureCredentials("default-client-id", "default-client-secret", "default-tenant-id");
    }
    
    /**
     * GCP 자격증명 생성
     */
    private GcpCredentials createGcpCredentials(String tenantKey, String accountScope) {
        // TODO: GCP 자격증명 관리 시스템과 연동
        // 현재는 기본 자격증명 반환
        log.debug("[CredentialProvider] Created GCP credentials for tenant: {}, account: {}", tenantKey, accountScope);
        return new GcpCredentials("default-service-account", "default-project-id");
    }
    
    /**
     * 암호화된 값인 경우 복호화
     */
    private String decryptIfEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        try {
            // 암호화된 값인지 확인 (Base64 패턴 체크)
            if (value.matches("^[A-Za-z0-9+/]*={0,2}$") && value.length() > 20) {
                return encryptionService.decrypt(value);
            }
            return value;
        } catch (Exception e) {
            log.debug("[CredentialProvider] Value is not encrypted, using as-is: {}", e.getMessage());
            return value;
        }
    }
    
    /**
     * 캐시 무효화
     */
    public void evictCache(String tenantKey, ProviderType providerType, String accountScope) {
        String cacheKey = generateCacheKey(tenantKey, providerType, accountScope);
        credentialCache.remove(cacheKey);
        log.debug("[CredentialProvider] Cache evicted for key: {}", cacheKey);
    }
    
    /**
     * 전체 캐시 무효화
     */
    public void evictAllCache() {
        credentialCache.clear();
        log.debug("[CredentialProvider] All cache evicted");
    }
    
    // 내부 자격증명 클래스들
    public static class AwsCredentials {
        private final String accessKeyId;
        private final String secretAccessKey;
        private final String region;
        private final String sessionToken;
        
        public AwsCredentials(String accessKeyId, String secretAccessKey, String region) {
            this(accessKeyId, secretAccessKey, region, null);
        }
        
        public AwsCredentials(String accessKeyId, String secretAccessKey, String region, String sessionToken) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.region = region;
            this.sessionToken = sessionToken;
        }
        
        public String getAccessKeyId() { return accessKeyId; }
        public String getSecretAccessKey() { return secretAccessKey; }
        public String getRegion() { return region; }
        public String getSessionToken() { return sessionToken; }
    }
    
    public static class AzureCredentials {
        private final String clientId;
        private final String clientSecret;
        private final String tenantId;
        
        public AzureCredentials(String clientId, String clientSecret, String tenantId) {
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.tenantId = tenantId;
        }
        
        public String getClientId() { return clientId; }
        public String getClientSecret() { return clientSecret; }
        public String getTenantId() { return tenantId; }
    }
    
    public static class GcpCredentials {
        private final String serviceAccount;
        private final String projectId;
        
        public GcpCredentials(String serviceAccount, String projectId) {
            this.serviceAccount = serviceAccount;
            this.projectId = projectId;
        }
        
        public String getServiceAccount() { return serviceAccount; }
        public String getProjectId() { return projectId; }
    }
}
