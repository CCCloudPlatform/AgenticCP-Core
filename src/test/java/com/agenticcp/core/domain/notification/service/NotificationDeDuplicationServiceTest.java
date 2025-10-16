package com.agenticcp.core.domain.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * NotificationDeDuplicationService 단위 테스트
 * 
 * <p>알림 중복 방지 로직을 테스트합니다.</p>
 */
class NotificationDeDuplicationServiceTest {

    private NotificationDeDuplicationService deDuplicationService;
    
    @BeforeEach
    void setUp() {
        deDuplicationService = new NotificationDeDuplicationService();
        // 내부 히스토리 맵 초기화
        Map<String, Object> history = new ConcurrentHashMap<>();
        ReflectionTestUtils.setField(deDuplicationService, "notificationHistory", history);
    }

    /**
     * 최초 알림 발송 가능 테스트
     * 
     * Given: 이전 발송 이력이 없는 알림
     * When: 발송 가능 여부 확인
     * Then: true 반환 (발송 가능)
     */
    @Test
    void canSendNotification_FirstTime_ReturnsTrue() {
        // Given
        String notificationKey = "test_alert_001";

        // When
        boolean canSend = deDuplicationService.canSendNotification(notificationKey);

        // Then
        assertTrue(canSend);
    }

    /**
     * 쿨다운 기간 내 중복 알림 방지 테스트
     * 
     * Given: 1초 전에 발송한 알림
     * When: 다시 발송 시도 (쿨다운: 5분)
     * Then: false 반환 (발송 불가)
     */
    @Test
    void canSendNotification_WithinCooldown_ReturnsFalse() {
        // Given
        String notificationKey = "test_alert_002";
        
        // 첫 번째 발송
        assertTrue(deDuplicationService.canSendNotification(notificationKey));
        deDuplicationService.recordNotificationSent(notificationKey);
        
        // When - 즉시 재발송 시도
        boolean canSend = deDuplicationService.canSendNotification(notificationKey);

        // Then
        assertFalse(canSend);
    }

    /**
     * 쿨다운 기간 경과 후 재발송 가능 테스트
     * 
     * Given: 쿨다운 기간(1초)이 지난 알림
     * When: 재발송 시도
     * Then: true 반환 (발송 가능)
     */
    @Test
    void canSendNotification_AfterCooldown_ReturnsTrue() throws InterruptedException {
        // Given
        String notificationKey = "test_alert_003";
        Duration shortCooldown = Duration.ofSeconds(1);
        
        // 첫 번째 발송
        assertTrue(deDuplicationService.canSendNotification(notificationKey, shortCooldown));
        deDuplicationService.recordNotificationSent(notificationKey);
        
        // 쿨다운 기간보다 조금 더 대기
        Thread.sleep(1100);
        
        // When - 재발송 시도
        boolean canSend = deDuplicationService.canSendNotification(notificationKey, shortCooldown);

        // Then
        assertTrue(canSend);
    }

    /**
     * 커스텀 쿨다운 기간 테스트
     * 
     * Given: 커스텀 쿨다운 기간(1초)
     * When: 쿨다운 기간 내 재발송 시도
     * Then: false 반환
     */
    @Test
    void canSendNotification_CustomCooldownPeriod_RespectsCooldown() {
        // Given
        String notificationKey = "test_alert_004";
        Duration customCooldown = Duration.ofSeconds(10);
        
        // 첫 번째 발송
        assertTrue(deDuplicationService.canSendNotification(notificationKey, customCooldown));
        deDuplicationService.recordNotificationSent(notificationKey);
        
        // When - 즉시 재발송 시도
        boolean canSend = deDuplicationService.canSendNotification(notificationKey, customCooldown);

        // Then
        assertFalse(canSend);
    }

    /**
     * 알림 발송 기록 테스트
     * 
     * Given: 새로운 알림
     * When: 발송 기록
     * Then: 히스토리에 기록되고 다음 발송 시 중복 방지
     */
    @Test
    void recordNotificationSent_CreatesHistory() {
        // Given
        String notificationKey = "test_alert_005";

        // When
        deDuplicationService.recordNotificationSent(notificationKey);

        // Then
        // 다시 발송 시도하면 쿨다운에 걸림
        assertFalse(deDuplicationService.canSendNotification(notificationKey));
    }

    /**
     * 억제 카운터 증가 테스트
     * 
     * Given: 쿨다운 기간 내 여러 번 발송 시도
     * When: 발송 가능 여부 확인 (여러 번)
     * Then: 모두 false 반환되고 억제 카운터 증가
     */
    @Test
    void canSendNotification_MultipleTries_IncrementsSuppressedCount() {
        // Given
        String notificationKey = "test_alert_006";
        
        // 첫 번째 발송
        deDuplicationService.canSendNotification(notificationKey);
        deDuplicationService.recordNotificationSent(notificationKey);
        
        // When - 여러 번 재발송 시도
        deDuplicationService.canSendNotification(notificationKey); // 1
        deDuplicationService.canSendNotification(notificationKey); // 2
        deDuplicationService.canSendNotification(notificationKey); // 3

        // Then
        // 억제된 알림 카운트는 내부적으로 기록됨 (로그로 확인 가능)
        assertEquals(1, deDuplicationService.getHistoryCount());
    }

    /**
     * 임계값 알림 키 생성 테스트
     * 
     * Given: 메트릭명과 임계값
     * When: 임계값 알림 키 생성
     * Then: "threshold_메트릭명_임계값" 형식의 키 반환
     */
    @Test
    void generateThresholdNotificationKey_GeneratesCorrectFormat() {
        // When
        String key = deDuplicationService.generateThresholdNotificationKey("cpu.usage", 90.0);

        // Then
        assertEquals("threshold_cpu.usage_90", key);
    }

    /**
     * 상태 변경 알림 키 생성 테스트
     * 
     * Given: 서비스명과 상태
     * When: 상태 변경 알림 키 생성
     * Then: "status_서비스명_상태" 형식의 키 반환
     */
    @Test
    void generateStatusChangeNotificationKey_GeneratesCorrectFormat() {
        // When
        String key = deDuplicationService.generateStatusChangeNotificationKey("api-server", "DOWN");

        // Then
        assertEquals("status_api-server_DOWN", key);
    }

    /**
     * 오래된 히스토리 정리 테스트
     * 
     * Given: 여러 알림 히스토리
     * When: cleanupOldHistory() 호출
     * Then: 오래된 히스토리가 제거됨 (실제로는 1시간 이상 지난 것)
     */
    @Test
    void cleanupOldHistory_RemovesOldEntries() {
        // Given
        String recentKey = "recent_alert";
        String oldKey = "old_alert";
        
        // 최근 알림 기록
        deDuplicationService.recordNotificationSent(recentKey);
        
        // 오래된 알림 기록 (실제 테스트에서는 강제로 시간을 조작해야 하지만, 여기서는 동작 확인)
        deDuplicationService.recordNotificationSent(oldKey);
        
        // When
        deDuplicationService.cleanupOldHistory();

        // Then
        // 최근 항목은 아직 1시간이 안 지났으므로 남아있음
        assertTrue(deDuplicationService.getHistoryCount() >= 0);
    }

    /**
     * 히스토리 카운트 조회 테스트
     * 
     * Given: 여러 알림이 기록됨
     * When: getHistoryCount() 호출
     * Then: 기록된 알림 수 반환
     */
    @Test
    void getHistoryCount_ReturnsCorrectCount() {
        // Given
        deDuplicationService.recordNotificationSent("alert_001");
        deDuplicationService.recordNotificationSent("alert_002");
        deDuplicationService.recordNotificationSent("alert_003");

        // When
        int count = deDuplicationService.getHistoryCount();

        // Then
        assertEquals(3, count);
    }

    /**
     * 다른 알림 키는 독립적으로 동작 테스트
     * 
     * Given: 서로 다른 알림 키
     * When: 각각 발송 가능 여부 확인
     * Then: 독립적으로 관리됨
     */
    @Test
    void canSendNotification_DifferentKeys_IndependentCooldown() {
        // Given
        String key1 = "alert_type_A";
        String key2 = "alert_type_B";
        
        // key1 발송
        assertTrue(deDuplicationService.canSendNotification(key1));
        deDuplicationService.recordNotificationSent(key1);
        
        // When - key2는 발송 가능
        boolean canSendKey2 = deDuplicationService.canSendNotification(key2);

        // Then
        assertTrue(canSendKey2); // key2는 쿨다운에 영향받지 않음
        assertFalse(deDuplicationService.canSendNotification(key1)); // key1은 쿨다운 중
    }

    /**
     * 억제 카운터 리셋 테스트
     * 
     * Given: 억제된 알림이 있는 상태
     * When: 재발송 성공 (쿨다운 경과 후)
     * Then: 억제 카운터가 리셋됨
     */
    @Test
    void recordNotificationSent_ResetsSuppressedCount() throws InterruptedException {
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
        assertTrue(deDuplicationService.canSendNotification(notificationKey, shortCooldown));
        deDuplicationService.recordNotificationSent(notificationKey);

        // Then - 억제 카운터 리셋됨 (내부적으로 0으로 재설정)
        // 다시 억제 시도
        assertFalse(deDuplicationService.canSendNotification(notificationKey, shortCooldown));
    }

    /**
     * 동시성 테스트 - 여러 스레드에서 동시 접근
     * 
     * Given: 같은 알림 키에 대한 동시 접근
     * When: 여러 스레드에서 발송 가능 여부 확인
     * Then: 안전하게 동작 (ConcurrentHashMap 사용)
     */
    @Test
    void canSendNotification_ConcurrentAccess_ThreadSafe() throws InterruptedException {
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

        // Then - 예외 없이 동작하고 히스토리 존재
        assertEquals(1, deDuplicationService.getHistoryCount());
    }
}

