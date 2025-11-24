package com.agenticcp.core.domain.platform.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 캐시 설정 클래스
 * 기능 플래그용 Redis 캐시 매니저 및 직렬화 설정
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true", matchIfMissing = false)
public class RedisCacheConfig {

    /**
     * Redis 캐시 매니저 빈 생성
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @return CacheManager
     */
    @Bean("redisCacheManager")
    @Primary
    @ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5)) // 기본 TTL 5분
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new StringRedisSerializer()
                        )
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                createJsonSerializer()
                        )
                )
                .disableCachingNullValues(); // null 값 캐싱 비활성화

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware() // 트랜잭션 지원
                .build();
    }

    /**
     * JSON 직렬화기 생성
     * LocalDateTime 등 Java 8 Time API 지원
     * 
     * @return GenericJackson2JsonRedisSerializer
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Java 8 Time API 지원
        objectMapper.registerModule(new JavaTimeModule());
        
        // 다형성 타입 처리 (보안 강화)
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

    /**
     * 캐시 설정 프로퍼티 빈
     * application.yml의 feature-flag.cache 설정을 바인딩
     *
     * @return 기능 플래그 캐시 설정 프로퍼티
     */
    @Bean
    @ConfigurationProperties(prefix = "feature-flag.cache")
    public FeatureFlagCacheProperties featureFlagCacheProperties() {
        return new FeatureFlagCacheProperties();
    }

    /**
     * 기능 플래그 캐시 설정 프로퍼티
     */
    public static class FeatureFlagCacheProperties {
        private boolean warmupOnStartup = true;
        private int defaultTtlSeconds = 300;
        private int minTtlSeconds = 10;
        private int maxTtlSeconds = 3600;

        public boolean isWarmupOnStartup() {
            return warmupOnStartup;
        }

        public void setWarmupOnStartup(boolean warmupOnStartup) {
            this.warmupOnStartup = warmupOnStartup;
        }

        public int getDefaultTtlSeconds() {
            return defaultTtlSeconds;
        }

        public void setDefaultTtlSeconds(int defaultTtlSeconds) {
            this.defaultTtlSeconds = defaultTtlSeconds;
        }

        public int getMinTtlSeconds() {
            return minTtlSeconds;
        }

        public void setMinTtlSeconds(int minTtlSeconds) {
            this.minTtlSeconds = minTtlSeconds;
        }

        public int getMaxTtlSeconds() {
            return maxTtlSeconds;
        }

        public void setMaxTtlSeconds(int maxTtlSeconds) {
            this.maxTtlSeconds = maxTtlSeconds;
        }
    }
}

