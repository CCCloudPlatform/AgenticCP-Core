package com.agenticcp.core.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NotificationDeDuplicationService 단위 테스트
 * 
 * <p>알림 중복 방지 로직을 테스트합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@DisplayName("NotificationDeDuplicationService 단위 테스트")
class NotificationDeDuplicationServiceTest {

    private NotificationDeDuplicationService deDuplicationService;
    
    @BeforeEach
    void setUp() {
        deDuplicationService = new NotificationDeDuplicationService();
        // 내부 히스토리 맵 초기화
        Map<String, Object> history = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(deDuplicationService, "notificationHistory", history);
    }

    @Nested
    @DisplayName("알림 발송 가능 여부 확인 테스트")
    class CanSendNotificationTest {

        @Test
        @DisplayName("최초 발송 시 true 반환")
        void canSendNotification_WhenFirstTime_ReturnsTrue() {
            // Given
            String notificationKey = "test_alert_001";

            // When
            boolean canSend = deDuplicationService.canSendNotification(notificationKey);

            // Then
            assertThat(canSend).isTrue();
        }

        @Test
        @DisplayName("쿨다운 기간 내 재발송 시 false 반환")
        void canSendNotification_WhenWithinCooldown_ReturnsFalse() {
            // Given
            String notificationKey = "test_alert_002";
            
            // 첫 번째 발송
            assertThat(deDuplicationService.canSendNotification(notificationKey)).isTrue();
            deDuplicationService.recordNotificationSent(notificationKey);
            
            // When - 즉시 재발송 시도
            boolean canSend = deDuplicationService.canSendNotification(notificationKey);

            // Then
            assertThat(canSend).isFalse();
        }

        @Test
        @DisplayName("쿨다운 기간 경과 후 재발송 시 true 반환")
        void canSendNotification_WhenAfterCooldown_ReturnsTrue() throws InterruptedException {
            // Given
            String notificationKey = "test_alert_003";
            Duration shortCooldown = Duration.ofSeconds(1);
            
            // 첫 번째 발송
            assertThat(deDuplicationService.canSendNotification(notificationKey, shortCooldown)).isTrue();
            deDuplicationService.recordNotificationSent(notificationKey);
            
            // 쿨다운 기간보다 조금 더 대기
            Thread.sleep(1100);
            
            // When - 재발송 시도
            boolean canSend = deDuplicationService.canSendNotification(notificationKey, shortCooldown);

            // Then
            assertThat(canSend).isTrue();
        }

        @Test
        @DisplayName("커스텀 쿨다운 기간 적용 확인")
        void canSendNotification_WhenCustomCooldownPeriod_RespectsCooldown() {
            // Given
            String notificationKey = "test_alert_004";
            Duration customCooldown = Duration.ofSeconds(10);
            
            // 첫 번째 발송
            assertThat(deDuplicationService.canSendNotification(notificationKey, customCooldown)).isTrue();
            deDuplicationService.recordNotificationSent(notificationKey);
            
            // When - 즉시 재발송 시도
            boolean canSend = deDuplicationService.canSendNotification(notificationKey, customCooldown);

            // Then
            assertThat(canSend).isFalse();
        }

        @Test
        @DisplayName("여러 번 시도 시 억제 카운터 증가")
        void canSendNotification_WhenMultipleTries_IncrementsSuppressedCount() {
            // Given
            String notificationKey = "test_alert_006";
            
            // 첫 번째 발송
            deDuplicationService.canSendNotification(notificationKey);
            deDuplicationService.recordNotificationSent(notificationKey);
            
            // When - 여러 번 재발송 시도
            deDuplicationService.canSendNotification(notificationKey);
            deDuplicationService.canSendNotification(notificationKey);
            deDuplicationService.canSendNotification(notificationKey);

            // Then
            assertThat(deDuplicationService.getHistoryCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("서로 다른 알림 키는 독립적으로 동작")
        void canSendNotification_WhenDifferentKeys_IndependentCooldown() {
            // Given
            String key1 = "alert_type_A";
            String key2 = "alert_type_B";
            
            // key1 발송
            assertThat(deDuplicationService.canSendNotification(key1)).isTrue();
            deDuplicationService.recordNotificationSent(key1);
            
            // When - key2는 발송 가능
            boolean canSendKey2 = deDuplicationService.canSendNotification(key2);

            // Then
            assertThat(canSendKey2).isTrue();
            assertThat(deDuplicationService.canSendNotification(key1)).isFalse();
        }

        @Test
        @DisplayName("동시 접근 시 Thread-safe 동작")
        void canSendNotification_WhenConcurrentAccess_ThreadSafe() throws InterruptedException {
            // Given
            String notificationKey = "concurrent_test";
            int threadCount = 10;
            Thread[] threads = new Thread[threadCount];
            
            // When - 여러 스레드에서 동시 접근
            for (int i = 0; i < threadCount; i++) {
                threads[i] = new Thread(() -> {
                    deDuplicationService.canSendNotification(notificationKey);
                    deDuplicationService.recordNotificationSent(notificationKey);
                });
                threads[i].start();
            }
            
            // 모든 스레드 종료 대기
            for (Thread thread : threads) {
                thread.join();
            }

            // Then
            assertThat(deDuplicationService.getHistoryCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("알림 발송 기록 테스트")
    class RecordNotificationSentTest {

        @Test
        @DisplayName("알림 발송 기록 시 히스토리 생성")
        void recordNotificationSent_WhenNewNotification_CreatesHistory() {
            // Given
            String notificationKey = "test_alert_005";

            // When
            deDuplicationService.recordNotificationSent(notificationKey);

            // Then
            assertThat(deDuplicationService.canSendNotification(notificationKey)).isFalse();
        }

        @Test
        @DisplayName("재발송 성공 시 억제 카운터 리셋")
        void recordNotificationSent_WhenResent_ResetsSuppressedCount() throws InterruptedException {
            // Given
            String notificationKey = "test_alert_007";
            Duration shortCooldown = Duration.ofMillis(500);
            
            // 첫 발송
            deDuplicationService.canSendNotification(notificationKey, shortCooldown);
            deDuplicationService.recordNotificationSent(notificationKey);
            
            // 억제 시도 (쿨다운 내)
            deDuplicationService.canSendNotification(notificationKey, shortCooldown);
            deDuplicationService.canSendNotification(notificationKey, shortCooldown);
            
            // 쿨다운 경과 대기
            Thread.sleep(600);
            
            // When - 재발송 성공
            assertThat(deDuplicationService.canSendNotification(notificationKey, shortCooldown)).isTrue();
            deDuplicationService.recordNotificationSent(notificationKey);

            // Then
            assertThat(deDuplicationService.canSendNotification(notificationKey, shortCooldown)).isFalse();
        }
    }

    @Nested
    @DisplayName("알림 키 생성 테스트")
    class GenerateNotificationKeyTest {

        @Test
        @DisplayName("임계값 알림 키 생성 시 올바른 형식 반환")
        void generateThresholdNotificationKey_WhenCalled_ReturnsCorrectFormat() {
            // When
            String key = deDuplicationService.generateThresholdNotificationKey("cpu.usage", 90.0);

            // Then
            assertThat(key).isEqualTo("threshold_cpu.usage_90");
        }

        @Test
        @DisplayName("상태 변경 알림 키 생성 시 올바른 형식 반환")
        void generateStatusChangeNotificationKey_WhenCalled_ReturnsCorrectFormat() {
            // When
            String key = deDuplicationService.generateStatusChangeNotificationKey("api-server", "DOWN");

            // Then
            assertThat(key).isEqualTo("status_api-server_DOWN");
        }
    }

    @Nested
    @DisplayName("이력 관리 테스트")
    class HistoryManagementTest {

        @Test
        @DisplayName("오래된 이력 정리 시 이력 제거")
        void cleanupOldHistory_WhenCalled_RemovesOldEntries() {
            // Given
            String recentKey = "recent_alert";
            String oldKey = "old_alert";
            
            deDuplicationService.recordNotificationSent(recentKey);
            deDuplicationService.recordNotificationSent(oldKey);
            
            // When
            deDuplicationService.cleanupOldHistory();

            // Then
            assertThat(deDuplicationService.getHistoryCount()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("이력 수 조회 시 올바른 수 반환")
        void getHistoryCount_WhenCalled_ReturnsCorrectCount() {
            // Given
            deDuplicationService.recordNotificationSent("alert_001");
            deDuplicationService.recordNotificationSent("alert_002");
            deDuplicationService.recordNotificationSent("alert_003");

            // When
            int count = deDuplicationService.getHistoryCount();

            // Then
            assertThat(count).isEqualTo(3);
        }
    }
}

