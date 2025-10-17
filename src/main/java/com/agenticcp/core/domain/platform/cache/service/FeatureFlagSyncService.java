package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.domain.platform.cache.event.FeatureFlagChangeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * 기능 플래그 동기화 서비스
 * <p>
 * `app.redis.enabled` 프로퍼티가 `true`일 때만 활성화됩니다.
 * Redis Pub/Sub을 통해 플래그 변경 이벤트를 발행하고 수신합니다.
 * </p>
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.redis", name = "enabled", havingValue = "true")
public class FeatureFlagSyncService implements MessageListener {

    private final RedisTemplate<String, FeatureFlagChangeEvent> redisTemplate;
    private final ChannelTopic featureFlagChangeTopic;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final FeatureFlagCacheService cacheService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 현재 노드의 고유 ID (UUID)
     * <p>
     * 자신이 발행한 이벤트를 필터링하기 위해 사용합니다.
     * </p>
     */
    @Getter
    private final String nodeId = UUID.randomUUID().toString();

    /**
     * 서비스 초기화 시 Redis Pub/Sub 리스너 등록
     */
    @PostConstruct
    public void init() {
        redisMessageListenerContainer.addMessageListener(this, featureFlagChangeTopic);
        log.info("[FeatureFlagSyncService] Initialized - NodeId: {}, Channel: {}", 
                nodeId, featureFlagChangeTopic.getTopic());
    }

    /**
     * Redis Pub/Sub 메시지 수신 핸들러
     * <p>
     * 다른 노드에서 발행한 플래그 변경 이벤트를 수신하여 로컬 캐시를 무효화합니다.
     * </p>
     *
     * @param message 수신된 메시지
     * @param pattern 구독 패턴 (사용 안 함)
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String messageBody = new String(message.getBody());
            FeatureFlagChangeEvent event = objectMapper.readValue(messageBody, FeatureFlagChangeEvent.class);
            
            log.debug("[FeatureFlagSyncService] Received event: flagKey={}, changeType={}, sourceNodeId={}", 
                    event.getFlagKey(), event.getChangeType(), event.getSourceNodeId());
            
            handleChangeEvent(event);
            
        } catch (Exception e) {
            log.error("[FeatureFlagSyncService] Failed to process message: {}", e.getMessage(), e);
        }
    }

    /**
     * 플래그 변경 이벤트 처리
     * <p>
     * sourceNodeId를 확인하여 자신이 발행한 이벤트는 무시하고,
     * 다른 노드의 이벤트만 처리하여 로컬 캐시를 무효화합니다.
     * </p>
     *
     * @param event 플래그 변경 이벤트
     */
    public void handleChangeEvent(FeatureFlagChangeEvent event) {
        // null 체크
        if (event == null) {
            log.warn("[FeatureFlagSyncService] Received null event, ignoring");
            return;
        }

        // flagKey null 체크
        if (event.getFlagKey() == null || event.getFlagKey().isEmpty()) {
            log.warn("[FeatureFlagSyncService] Received event with null/empty flagKey, ignoring");
            return;
        }

        // 자신이 발행한 이벤트는 무시 (sourceNodeId 필터링)
        if (nodeId.equals(event.getSourceNodeId())) {
            log.debug("[FeatureFlagSyncService] Ignoring own event: flagKey={}", event.getFlagKey());
            return;
        }

        // 다른 노드의 이벤트만 처리
        log.info("[FeatureFlagSyncService] Processing event from other node: flagKey={}, changeType={}, sourceNodeId={}", 
                event.getFlagKey(), event.getChangeType(), event.getSourceNodeId());

        try {
            cacheService.invalidateCache(event.getFlagKey());
            log.info("[FeatureFlagSyncService] Cache invalidated for flagKey: {}", event.getFlagKey());
        } catch (Exception e) {
            log.error("[FeatureFlagSyncService] Failed to invalidate cache for flagKey: {}. Error: {}", 
                    event.getFlagKey(), e.getMessage(), e);
        }
    }

    /**
     * 플래그 생성 이벤트 발행
     *
     * @param flagKey 생성된 플래그 키
     */
    public void publishCreated(String flagKey) {
        FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(flagKey, nodeId);
        publishEvent(event);
    }

    /**
     * 플래그 수정 이벤트 발행
     *
     * @param flagKey 수정된 플래그 키
     */
    public void publishUpdated(String flagKey) {
        FeatureFlagChangeEvent event = FeatureFlagChangeEvent.updated(flagKey, nodeId);
        publishEvent(event);
    }

    /**
     * 플래그 삭제 이벤트 발행
     *
     * @param flagKey 삭제된 플래그 키
     */
    public void publishDeleted(String flagKey) {
        FeatureFlagChangeEvent event = FeatureFlagChangeEvent.deleted(flagKey, nodeId);
        publishEvent(event);
    }

    /**
     * 플래그 토글 이벤트 발행
     *
     * @param flagKey 토글된 플래그 키
     */
    public void publishToggled(String flagKey) {
        FeatureFlagChangeEvent event = FeatureFlagChangeEvent.toggled(flagKey, nodeId);
        publishEvent(event);
    }

    /**
     * 캐시 무효화 이벤트 발행
     *
     * @param flagKey 무효화할 플래그 키
     */
    public void publishInvalidated(String flagKey) {
        FeatureFlagChangeEvent event = FeatureFlagChangeEvent.invalidated(flagKey, nodeId);
        publishEvent(event);
    }

    /**
     * 이벤트를 Redis Pub/Sub 채널에 발행
     *
     * @param event 발행할 이벤트
     */
    private void publishEvent(FeatureFlagChangeEvent event) {
        try {
            redisTemplate.convertAndSend(featureFlagChangeTopic.getTopic(), event);
            log.info("[FeatureFlagSyncService] Published event: flagKey={}, changeType={}, nodeId={}", 
                    event.getFlagKey(), event.getChangeType(), nodeId);
        } catch (Exception e) {
            log.error("[FeatureFlagSyncService] Failed to publish event: flagKey={}, changeType={}. Error: {}", 
                    event.getFlagKey(), event.getChangeType(), e.getMessage(), e);
            // 이벤트 발행 실패는 예외를 던지지 않음
        }
    }
}

