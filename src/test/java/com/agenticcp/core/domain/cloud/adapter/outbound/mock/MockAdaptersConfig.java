package com.agenticcp.core.domain.cloud.adapter.outbound.mock;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.OutboxEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * 테스트 환경에서 사용할 Mock 어댑터들
 * 헥사고날 아키텍처의 포트 인터페이스만 정의하고 실제 구현체가 없을 때 사용
 */
@TestConfiguration
@Profile("test")
@Slf4j
public class MockAdaptersConfig {

    @Bean
    @Primary
    public CredentialProviderPort mockCredentialProviderPort() {
        return new CredentialProviderPort() {
            @Override
            public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
                log.debug("Mock credential resolution for tenant: {}, provider: {}, account: {}", 
                         tenantKey, providerType, accountScope);
                return "mock-credentials";
            }

            @Override
            public String storeCredentials(String tenantKey, ProviderType providerType, 
                                          String accountScope, Map<String, String> credentials) {
                log.debug("Mock credential storage for tenant: {}, provider: {}, account: {}",
                         tenantKey, providerType, accountScope);
                return "mock-credential-key";
            }

            @Override
            public void deleteCredentials(ProviderType providerType, String credentialKey) {
                log.debug("Mock credential deletion: providerType={}, credentialKey={}", providerType, credentialKey);
            }

            @Override
            public com.agenticcp.core.domain.cloud.port.model.CloudSessionCredential getSession(
                    String tenantKey, Long accountId, ProviderType providerType) {
                log.debug("Mock session retrieval: tenantKey={}, accountId={}, providerType={}", 
                        tenantKey, accountId, providerType);
                return com.agenticcp.core.domain.cloud.port.model.AwsSessionCredential.builder()
                        .accessKeyId("mock-access-key")
                        .secretAccessKey("mock-secret-key")
                        .sessionToken("mock-session-token")
                        .region("us-east-1")
                        .expiresAt(java.time.LocalDateTime.now().plusHours(1))
                        .build();
            }
        };
    }

    @Bean
    @Primary
    public AuditEventPort mockAuditEventPort() {
        return new AuditEventPort() {
            @Override
            public void record(String action, String subject, String outcome, Map<String, Object> attributes) {
                log.debug("Mock audit event: action={}, subject={}, outcome={}, attributes={}", 
                         action, subject, outcome, attributes);
            }
        };
    }

    @Bean
    @Primary
    public TracingPort mockTracingPort() {
        return new TracingPort() {
            @Override
            public AutoCloseable startSpan(String name, Map<String, String> tags) {
                log.debug("Mock tracing span started: name={}, tags={}", name, tags);
                return () -> log.debug("Mock tracing span closed: name={}", name);
            }

            @Override
            public void tag(String key, String value) {
                log.debug("Mock tracing tag: {}={}", key, value);
            }

            @Override
            public void event(String name, Map<String, String> attrs) {
                log.debug("Mock tracing event: name={}, attrs={}", name, attrs);
            }
        };
    }

    @Bean
    @Primary
    public OutboxEventPort mockOutboxEventPort() {
        return new OutboxEventPort() {
            @Override
            public void publish(String topic, String key, Map<String, Object> payload) {
                log.debug("Mock outbox event published: topic={}, key={}, payload={}", 
                         topic, key, payload);
            }
        };
    }
}
