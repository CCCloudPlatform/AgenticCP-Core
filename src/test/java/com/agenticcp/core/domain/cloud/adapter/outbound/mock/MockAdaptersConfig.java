package com.agenticcp.core.domain.cloud.adapter.outbound.mock;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.outbound.AuditEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.CredentialProviderPort;
import com.agenticcp.core.domain.cloud.port.outbound.OutboxEventPort;
import com.agenticcp.core.domain.cloud.port.outbound.TracingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 테스트 환경에서만 사용할 Mock 어댑터들
 * 헥사고날 아키텍처의 포트 인터페이스만 정의하고 실제 구현체가 없을 때 사용
 * 
 * 주의: src/test 폴더에 있어 빌드 시 JAR에 포함되지 않음
 * 프로덕션/Docker 환경에서는 실제 구현체가 필요함
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

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.redis.enabled", havingValue = "false", matchIfMissing = true)
    public RedisTemplate<String, Object> mockRedisTemplate() {
        RedisTemplate<String, Object> mockRedis = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOps = mock(ValueOperations.class);
        
        Map<String, Object> cache = new HashMap<>();
        
        when(mockRedis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), any(), any(Duration.class));
        when(mockRedis.keys(anyString())).thenAnswer(invocation -> {
            String pattern = ((String) invocation.getArgument(0)).replace("*", ".*");
            return cache.keySet().stream()
                    .filter(key -> key.matches(pattern))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        });
        when(mockRedis.delete(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return cache.remove(key) != null ? 1L : 0L;
        });
        when(mockRedis.delete(any(Collection.class))).thenAnswer(invocation -> {
            Collection<String> keys = invocation.getArgument(0);
            long deleted = keys.stream().mapToLong(key -> cache.remove(key) != null ? 1L : 0L).sum();
            return deleted;
        });
        when(mockRedis.hasKey(anyString())).thenAnswer(invocation -> cache.containsKey(invocation.getArgument(0)));

        log.debug("Mock RedisTemplate configured - will use in-memory Map for caching");
        return mockRedis;
    }
}
