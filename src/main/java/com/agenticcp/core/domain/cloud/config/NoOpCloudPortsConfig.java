package com.agenticcp.core.domain.cloud.config;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Map;

/**
 * Cloud 포트의 기본 No-Op 구현 제공
 * 실제 구현 빈이 없을 때만 등록되어 개발/로컬 환경에서 안전하게 기동되도록 합니다.
 */
@Configuration
public class NoOpCloudPortsConfig {

    @Bean
    @ConditionalOnMissingBean(AccountCredentialManagementPort.class)
    public AccountCredentialManagementPort accountCredentialManagementPort() {
        return new AccountCredentialManagementPort() {
            @Override
            public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
                return null; // 기본 No-Op 동작
            }

            @Override
            public String storeCredentials(String tenantKey, ProviderType providerType, 
                                         String accountScope, Map<String, String> credentials) {
                return null; // 기본 No-Op 동작
            }

            @Override
            public void deleteCredentials(ProviderType providerType, String credentialKey) {
                // No-Op
            }

            @Override
            public CloudSessionCredential getSession(String tenantKey, String accountScope, ProviderType providerType) {
                return null; // 기본 No-Op 동작
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(AuditEventPort.class)
    public AuditEventPort auditEventPort() {
        return new AuditEventPort() {
            @Override
            public void record(String tenantKey, String eventType, String resource, Map<String, Object> metadata) {
                // No-Op
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(TracingPort.class)
    public TracingPort tracingPort() {
        return new TracingPort() {
            @Override
            public AutoCloseable startSpan(String name, Map<String, String> tags) {
                return () -> { /* No-Op */ };
            }

            @Override
            public void tag(String key, String value) {
                // No-Op
            }

            @Override
            public void event(String name, Map<String, String> attrs) {
                // No-Op
            }
        };
    }
}


