package com.agenticcp.core.domain.platform.cache.service;

import com.agenticcp.core.domain.platform.cache.event.FeatureFlagChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagSyncService 단위 테스트
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagSyncService 단위 테스트")
class FeatureFlagSyncServiceTest {

    @Mock
    private RedisTemplate<String, FeatureFlagChangeEvent> redisTemplate;

    @Mock
    private ChannelTopic featureFlagChangeTopic;

    @Mock
    private FeatureFlagCacheService cacheService;

    @InjectMocks
    private FeatureFlagSyncService syncService;

    private String testNodeId;

    @BeforeEach
    void setUp() {
        // 테스트용 노드 ID 설정
        testNodeId = syncService.getNodeId();
        
        // Topic 채널명 설정 (lenient로 설정 - 모든 테스트에서 사용되지 않음)
        lenient().when(featureFlagChangeTopic.getTopic()).thenReturn("agenticcp:feature-flag:change");
    }

    @Nested
    @DisplayName("이벤트 발행 테스트")
    class PublishEventTest {

        @Test
        @DisplayName("CREATED 이벤트 발행 성공")
        void publishCreatedEvent_Success() {
            // Given
            String flagKey = "new-feature";

            // When
            syncService.publishCreated(flagKey);

            // Then
            ArgumentCaptor<FeatureFlagChangeEvent> eventCaptor = ArgumentCaptor.forClass(FeatureFlagChangeEvent.class);
            verify(redisTemplate).convertAndSend(eq("agenticcp:feature-flag:change"), eventCaptor.capture());

            FeatureFlagChangeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getFlagKey()).isEqualTo(flagKey);
            assertThat(capturedEvent.getChangeType()).isEqualTo(FeatureFlagChangeEvent.ChangeType.CREATED);
            assertThat(capturedEvent.getSourceNodeId()).isEqualTo(testNodeId);
            assertThat(capturedEvent.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("UPDATED 이벤트 발행 성공")
        void publishUpdatedEvent_Success() {
            // Given
            String flagKey = "existing-feature";

            // When
            syncService.publishUpdated(flagKey);

            // Then
            ArgumentCaptor<FeatureFlagChangeEvent> eventCaptor = ArgumentCaptor.forClass(FeatureFlagChangeEvent.class);
            verify(redisTemplate).convertAndSend(eq("agenticcp:feature-flag:change"), eventCaptor.capture());

            FeatureFlagChangeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getFlagKey()).isEqualTo(flagKey);
            assertThat(capturedEvent.getChangeType()).isEqualTo(FeatureFlagChangeEvent.ChangeType.UPDATED);
            assertThat(capturedEvent.getSourceNodeId()).isEqualTo(testNodeId);
        }

        @Test
        @DisplayName("DELETED 이벤트 발행 성공")
        void publishDeletedEvent_Success() {
            // Given
            String flagKey = "old-feature";

            // When
            syncService.publishDeleted(flagKey);

            // Then
            ArgumentCaptor<FeatureFlagChangeEvent> eventCaptor = ArgumentCaptor.forClass(FeatureFlagChangeEvent.class);
            verify(redisTemplate).convertAndSend(eq("agenticcp:feature-flag:change"), eventCaptor.capture());

            FeatureFlagChangeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getFlagKey()).isEqualTo(flagKey);
            assertThat(capturedEvent.getChangeType()).isEqualTo(FeatureFlagChangeEvent.ChangeType.DELETED);
        }

        @Test
        @DisplayName("TOGGLED 이벤트 발행 성공")
        void publishToggledEvent_Success() {
            // Given
            String flagKey = "toggle-feature";

            // When
            syncService.publishToggled(flagKey);

            // Then
            ArgumentCaptor<FeatureFlagChangeEvent> eventCaptor = ArgumentCaptor.forClass(FeatureFlagChangeEvent.class);
            verify(redisTemplate).convertAndSend(eq("agenticcp:feature-flag:change"), eventCaptor.capture());

            FeatureFlagChangeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getFlagKey()).isEqualTo(flagKey);
            assertThat(capturedEvent.getChangeType()).isEqualTo(FeatureFlagChangeEvent.ChangeType.TOGGLED);
        }

        @Test
        @DisplayName("INVALIDATED 이벤트 발행 성공")
        void publishInvalidatedEvent_Success() {
            // Given
            String flagKey = "cache-feature";

            // When
            syncService.publishInvalidated(flagKey);

            // Then
            ArgumentCaptor<FeatureFlagChangeEvent> eventCaptor = ArgumentCaptor.forClass(FeatureFlagChangeEvent.class);
            verify(redisTemplate).convertAndSend(eq("agenticcp:feature-flag:change"), eventCaptor.capture());

            FeatureFlagChangeEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.getFlagKey()).isEqualTo(flagKey);
            assertThat(capturedEvent.getChangeType()).isEqualTo(FeatureFlagChangeEvent.ChangeType.INVALIDATED);
        }

        @Test
        @DisplayName("이벤트 발행 실패 시 예외를 던지지 않고 로그만 기록")
        void publishEvent_Failure_LogsErrorButNoException() {
            // Given
            String flagKey = "error-feature";
            doThrow(new RuntimeException("Redis connection error"))
                    .when(redisTemplate).convertAndSend(anyString(), any(FeatureFlagChangeEvent.class));

            // When & Then
            assertThatCode(() -> syncService.publishCreated(flagKey))
                    .doesNotThrowAnyException();

            verify(redisTemplate).convertAndSend(anyString(), any(FeatureFlagChangeEvent.class));
        }
    }

    @Nested
    @DisplayName("이벤트 수신 테스트")
    class ReceiveEventTest {

        @Test
        @DisplayName("다른 노드의 CREATED 이벤트 수신 시 캐시 무효화")
        void onCreatedEvent_FromOtherNode_InvalidatesCache() {
            // Given
            String flagKey = "new-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(flagKey, "other-node-id");
            doNothing().when(cacheService).invalidateCache(flagKey);

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService).invalidateCache(flagKey);
        }

        @Test
        @DisplayName("다른 노드의 UPDATED 이벤트 수신 시 캐시 무효화")
        void onUpdatedEvent_FromOtherNode_InvalidatesCache() {
            // Given
            String flagKey = "existing-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.updated(flagKey, "other-node-id");
            doNothing().when(cacheService).invalidateCache(flagKey);

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService).invalidateCache(flagKey);
        }

        @Test
        @DisplayName("다른 노드의 DELETED 이벤트 수신 시 캐시 무효화")
        void onDeletedEvent_FromOtherNode_InvalidatesCache() {
            // Given
            String flagKey = "old-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.deleted(flagKey, "other-node-id");
            doNothing().when(cacheService).invalidateCache(flagKey);

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService).invalidateCache(flagKey);
        }

        @Test
        @DisplayName("자신의 이벤트는 무시 (sourceNodeId 필터링)")
        void onEvent_FromSameNode_IgnoresEvent() {
            // Given
            String flagKey = "my-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(flagKey, testNodeId);

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService, never()).invalidateCache(anyString());
        }

        @Test
        @DisplayName("sourceNodeId가 null인 이벤트는 처리")
        void onEvent_WithNullSourceNodeId_ProcessesEvent() {
            // Given
            String flagKey = "legacy-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(flagKey, null);
            doNothing().when(cacheService).invalidateCache(flagKey);

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService).invalidateCache(flagKey);
        }

        @Test
        @DisplayName("이벤트가 null이면 무시")
        void onEvent_Null_IgnoresEvent() {
            // When
            syncService.handleChangeEvent(null);

            // Then
            verify(cacheService, never()).invalidateCache(anyString());
        }

        @Test
        @DisplayName("flagKey가 null인 이벤트는 무시")
        void onEvent_WithNullFlagKey_IgnoresEvent() {
            // Given
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(null, "other-node-id");

            // When
            syncService.handleChangeEvent(event);

            // Then
            verify(cacheService, never()).invalidateCache(anyString());
        }

        @Test
        @DisplayName("캐시 무효화 실패 시 예외를 던지지 않고 로그만 기록")
        void onEvent_CacheInvalidationFailure_LogsErrorButNoException() {
            // Given
            String flagKey = "error-feature";
            FeatureFlagChangeEvent event = FeatureFlagChangeEvent.created(flagKey, "other-node-id");
            doThrow(new RuntimeException("Cache invalidation failed"))
                    .when(cacheService).invalidateCache(flagKey);

            // When & Then
            assertThatCode(() -> syncService.handleChangeEvent(event))
                    .doesNotThrowAnyException();

            verify(cacheService).invalidateCache(flagKey);
        }
    }

    @Nested
    @DisplayName("노드 ID 테스트")
    class NodeIdTest {

        @Test
        @DisplayName("노드 ID는 UUID 형식")
        void nodeId_IsUUID() {
            // When
            String nodeId = syncService.getNodeId();

            // Then
            assertThat(nodeId).isNotNull();
            assertThat(nodeId).isNotEmpty();
            assertThat(nodeId).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("노드 ID는 인스턴스마다 동일")
        void nodeId_IsSameForSameInstance() {
            // When
            String nodeId1 = syncService.getNodeId();
            String nodeId2 = syncService.getNodeId();

            // Then
            assertThat(nodeId1).isEqualTo(nodeId2);
        }
    }
}

