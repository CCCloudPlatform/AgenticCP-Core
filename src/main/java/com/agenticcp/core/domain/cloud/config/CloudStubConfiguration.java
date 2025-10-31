package com.agenticcp.core.domain.cloud.config;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.OutboxEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Map;

/**
 * Cloud 기능의 기본(Stub) 구현체
 * 
 * 실제 구현이 없는 초기 단계나 개발 환경에서 사용
 * 프로덕션 배포 시에는 cloud.stub.enabled=false로 설정하여 비활성화
 */
@Configuration
@Profile("docker")
@ConditionalOnProperty(name = "cloud.stub.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CloudStubConfiguration {

    @Bean
    @Primary
    public CredentialProviderPort credentialProviderPort() {
        log.info("Cloud Stub implementation enabled for CredentialProviderPort");
        return new CredentialProviderPort() {
            @Override
            public Object resolveCredentials(String tenantKey, ProviderType providerType, String accountScope) {
                log.debug("Stub: Credential resolution - tenant={}, provider={}, account={}", 
                         tenantKey, providerType, accountScope);
                return "stub-credentials";
            }
        };
    }

    @Bean
    @Primary
    public AuditEventPort auditEventPort() {
        log.info("Cloud Stub implementation enabled for AuditEventPort");
        return new AuditEventPort() {
            @Override
            public void record(String action, String subject, String outcome, Map<String, Object> attributes) {
                log.debug("Stub: Audit event - action={}, subject={}, outcome={}", action, subject, outcome);
            }
        };
    }

    @Bean
    @Primary
    public TracingPort tracingPort() {
        log.info("Cloud Stub implementation enabled for TracingPort");
        return new TracingPort() {
            @Override
            public AutoCloseable startSpan(String name, Map<String, String> tags) {
                log.debug("Stub: Tracing span started - name={}", name);
                return () -> log.debug("Stub: Tracing span closed - name={}", name);
            }

            @Override
            public void tag(String key, String value) {
                log.debug("Stub: Tracing tag - {}={}", key, value);
            }

            @Override
            public void event(String name, Map<String, String> attrs) {
                log.debug("Stub: Tracing event - name={}", name);
            }
        };
    }

    @Bean
    @Primary
    public OutboxEventPort outboxEventPort() {
        log.info("Cloud Stub implementation enabled for OutboxEventPort");
        return new OutboxEventPort() {
            @Override
            public void publish(String topic, String key, Map<String, Object> payload) {
                log.debug("Stub: Outbox event - topic={}, key={}", topic, key);
            }
        };
    }
}

