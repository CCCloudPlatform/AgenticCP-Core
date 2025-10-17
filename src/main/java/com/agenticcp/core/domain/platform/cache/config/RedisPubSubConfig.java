package com.agenticcp.core.domain.platform.cache.config;

import com.agenticcp.core.domain.platform.cache.event.FeatureFlagChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 설정 클래스
 * <p>
 * `app.redis.enabled` 프로퍼티가 `true`일 때만 활성화됩니다.
 * 기능 플래그 변경 이벤트를 Redis Pub/Sub을 통해 전파하기 위한 설정입니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class RedisPubSubConfig {

    /**
     * 기능 플래그 변경 이벤트 채널명
     */
    public static final String FEATURE_FLAG_CHANGE_CHANNEL = "agenticcp:feature-flag:change";

    private final RedisConnectionFactory redisConnectionFactory;

    /**
     * Redis 메시지 리스너 컨테이너
     * <p>
     * Redis Pub/Sub 메시지를 수신하는 컨테이너입니다.
     * 리스너는 FeatureFlagSyncService에서 등록됩니다.
     * </p>
     *
     * @return RedisMessageListenerContainer
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        log.info("[RedisPubSubConfig] RedisMessageListenerContainer initialized");
        return container;
    }

    /**
     * 기능 플래그 변경 이벤트 채널 토픽
     *
     * @return ChannelTopic
     */
    @Bean
    public ChannelTopic featureFlagChangeTopic() {
        log.info("[RedisPubSubConfig] Feature flag change topic created: {}", FEATURE_FLAG_CHANGE_CHANNEL);
        return new ChannelTopic(FEATURE_FLAG_CHANGE_CHANNEL);
    }

    /**
     * 기능 플래그 변경 이벤트 발행용 RedisTemplate
     * <p>
     * FeatureFlagChangeEvent를 JSON으로 직렬화하여 발행합니다.
     * </p>
     *
     * @return RedisTemplate
     */
    @Bean
    public RedisTemplate<String, FeatureFlagChangeEvent> featureFlagEventRedisTemplate() {
        RedisTemplate<String, FeatureFlagChangeEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        
        // Key Serializer (String)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Value Serializer (JSON with LocalDateTime support)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();
        log.info("[RedisPubSubConfig] FeatureFlagEvent RedisTemplate initialized");
        return template;
    }
}

