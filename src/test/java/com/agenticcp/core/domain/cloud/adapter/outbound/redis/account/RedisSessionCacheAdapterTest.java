package com.agenticcp.core.domain.cloud.adapter.outbound.redis.account;

import com.agenticcp.core.domain.cloud.adapter.outbound.aws.account.AwsSessionCredential;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.repository.RedisSessionCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisSessionCacheAdapter 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisSessionCacheAdapter 테스트")
class RedisSessionCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private RedisSessionCacheRepository sessionCacheAdapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        sessionCacheAdapter = new RedisSessionCacheRepository(objectMapper, redisTemplate);
    }

    @Nested
    @DisplayName("세션 캐싱 테스트")
    class CacheSessionTest {

        @Test
        @DisplayName("세션을 Redis에 캐싱 성공")
        void cacheSession_Success() throws Exception {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AWS;
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-access-key")
                    .secretAccessKey("test-secret-key")
                    .sessionToken("test-session-token")
                    .region("us-east-1")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();
            int ttlMinutes = 55;

            // when
            sessionCacheAdapter.cacheSession(tenantKey, accountScope, providerType, session, ttlMinutes);

            // then
            verify(valueOperations).set(anyString(), anyString(), eq((long) ttlMinutes), eq(TimeUnit.MINUTES));
        }

        @Test
        @DisplayName("Redis가 null이면 캐싱하지 않음")
        void cacheSession_RedisNull_Skip() {
            // given
            RedisSessionCacheRepository adapterWithoutRedis = new RedisSessionCacheRepository(objectMapper, null);
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-key")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            // when & then - 예외 없이 종료
            adapterWithoutRedis.cacheSession("tenant-1", "123456789012", ProviderType.AWS, session, 55);
        }
    }

    @Nested
    @DisplayName("세션 조회 테스트")
    class GetCachedSessionTest {

        @Test
        @DisplayName("캐시된 유효한 세션 조회 성공")
        void getCachedSession_ValidSession_Success() throws Exception {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AWS;
            AwsSessionCredential session = AwsSessionCredential.builder()
                    .accessKeyId("test-access-key")
                    .secretAccessKey("test-secret-key")
                    .sessionToken("test-session-token")
                    .region("us-east-1")
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            String sessionJson = objectMapper.writeValueAsString(session);
            when(valueOperations.get(anyString())).thenReturn(sessionJson);

            // when
            Optional<CloudSessionCredential> result = sessionCacheAdapter.getCachedSession(
                    tenantKey, accountScope, providerType);

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(AwsSessionCredential.class);
            AwsSessionCredential awsSession = (AwsSessionCredential) result.get();
            assertThat(awsSession.getAccessKeyId()).isEqualTo("test-access-key");
            assertThat(awsSession.isValid()).isTrue();
        }

        @Test
        @DisplayName("캐시에 세션이 없으면 empty 반환")
        void getCachedSession_NoCache_ReturnsEmpty() {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // when
            Optional<CloudSessionCredential> result = sessionCacheAdapter.getCachedSession(
                    "tenant-1", "123456789012", ProviderType.AWS);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("만료된 세션이면 삭제하고 empty 반환")
        void getCachedSession_ExpiredSession_DeletesAndReturnsEmpty() throws Exception {
            // given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            AwsSessionCredential expiredSession = AwsSessionCredential.builder()
                    .accessKeyId("test-access-key")
                    .expiresAt(LocalDateTime.now().minusHours(1)) // 만료됨
                    .build();
            String sessionJson = objectMapper.writeValueAsString(expiredSession);
            when(valueOperations.get(anyString())).thenReturn(sessionJson);

            // when
            Optional<CloudSessionCredential> result = sessionCacheAdapter.getCachedSession(
                    "tenant-1", "123456789012", ProviderType.AWS);

            // then
            assertThat(result).isEmpty();
            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("Redis가 null이면 empty 반환")
        void getCachedSession_RedisNull_ReturnsEmpty() {
            // given
            RedisSessionCacheRepository adapterWithoutRedis = new RedisSessionCacheRepository(objectMapper, null);

            // when
            Optional<CloudSessionCredential> result = adapterWithoutRedis.getCachedSession(
                    "tenant-1", "123456789012", ProviderType.AWS);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("세션 삭제 테스트")
    class EvictSessionTest {

        @Test
        @DisplayName("세션을 Redis에서 삭제 성공")
        void evictSession_Success() {
            // given
            String tenantKey = "tenant-1";
            String accountScope = "123456789012";
            ProviderType providerType = ProviderType.AWS;

            // when
            sessionCacheAdapter.evictSession(tenantKey, accountScope, providerType);

            // then
            verify(redisTemplate).delete(anyString());
        }

        @Test
        @DisplayName("Redis가 null이면 삭제하지 않음")
        void evictSession_RedisNull_Skip() {
            // given
            RedisSessionCacheRepository adapterWithoutRedis = new RedisSessionCacheRepository(objectMapper, null);

            // when & then - 예외 없이 종료
            adapterWithoutRedis.evictSession("tenant-1", "123456789012", ProviderType.AWS);
        }
    }

    @Test
    @DisplayName("기본 TTL 반환")
    void getDefaultTtlMinutes_ReturnsDefault() {
        // when
        int ttl = sessionCacheAdapter.getDefaultTtlMinutes();

        // then
        assertThat(ttl).isEqualTo(55);
    }
}

