package com.agenticcp.core.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 알림 중복 방지 서비스
 * 
 * <p>동일한 알림이 연속으로 발생하는 것을 방지하고
 * 알림 빈도를 조절합니다.</p>
 * 
 * <p>Issue #15: 실시간 알림 시스템 - 시나리오 3 (알림 중복 방지)</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Service
@Slf4j
public class NotificationDeDuplicationService {

    /**
     * 알림 발송 이력 (알림 키 -> 마지막 발송 시간)
     */
    private final Map<String, NotificationHistory> notificationHistory = new ConcurrentHashMap<>();

    /**
     * 기본 중복 방지 시간 (5분)
     */
    private static final Duration DEFAULT_COOLDOWN_PERIOD = Duration.ofMinutes(5);

    /**
     * 알림 발송 가능 여부 확인
     * 
     * <p>동일한 알림이 쿨다운 기간 내에 발송되었는지 확인합니다.</p>
     * 
     * @param notificationKey 알림 고유 키 (예: "threshold_cpu.usage_90")
     * @return 발송 가능 여부
     */
    public boolean canSendNotification(String notificationKey) {
        return canSendNotification(notificationKey, DEFAULT_COOLDOWN_PERIOD);
    }

    /**
     * 알림 발송 가능 여부 확인 (커스텀 쿨다운 기간)
     * 
     * @param notificationKey 알림 고유 키
     * @param cooldownPeriod 쿨다운 기간
     * @return 발송 가능 여부
     */
    public boolean canSendNotification(String notificationKey, Duration cooldownPeriod) {
        NotificationHistory history = notificationHistory.get(notificationKey);
        
        if (history == null) {
            // 최초 발송 시도
            log.debug("알림 최초 발송: {}", notificationKey);
            return true;
        }
        
        LocalDateTime now = LocalDateTime.now();
        Duration timeSinceLastSent = Duration.between(history.getLastSentAt(), now);
        
        if (timeSinceLastSent.compareTo(cooldownPeriod) >= 0) {
            // 쿨다운 기간이 지남 - 발송 가능
            log.debug("알림 쿨다운 기간 경과 ({} 경과): {}", timeSinceLastSent, notificationKey);
            return true;
        }
        
        // 쿨다운 기간 내 - 발송 불가
        log.info("⏸️ 알림 중복 방지: {} (마지막 발송 후 {} 경과, 쿨다운: {})", 
            notificationKey, timeSinceLastSent, cooldownPeriod);
        
        // 중복 시도 횟수 증가
        history.incrementSuppressedCount();
        
        return false;
    }

    /**
     * 알림 발송 기록
     * 
     * <p>알림 발송 후 호출하여 이력을 기록합니다.</p>
     * 
     * @param notificationKey 알림 고유 키
     */
    public void recordNotificationSent(String notificationKey) {
        LocalDateTime now = LocalDateTime.now();
        
        NotificationHistory history = notificationHistory.get(notificationKey);
        if (history != null) {
            history.updateLastSentAt(now);
            log.debug("알림 발송 기록 업데이트: {} (억제된 알림: {})", 
                notificationKey, history.getSuppressedCount());
            
            // 억제된 알림이 있으면 로그 남기기
            if (history.getSuppressedCount() > 0) {
                log.info("📊 알림 통계: {} - 총 {}회 억제됨", 
                    notificationKey, history.getSuppressedCount());
            }
            
            // 발송 후 억제 카운터 리셋
            history.resetSuppressedCount();
        } else {
            notificationHistory.put(notificationKey, new NotificationHistory(now));
            log.debug("알림 발송 기록 생성: {}", notificationKey);
        }
    }

    /**
     * 알림 키 생성 (임계값 위반용)
     * 
     * @param metricName 메트릭명
     * @param thresholdValue 임계값
     * @return 알림 키
     */
    public String generateThresholdNotificationKey(String metricName, Double thresholdValue) {
        return String.format("threshold_%s_%.0f", metricName, thresholdValue);
    }

    /**
     * 알림 키 생성 (시스템 상태 변화용)
     * 
     * @param serviceName 서비스명
     * @param status 상태
     * @return 알림 키
     */
    public String generateStatusChangeNotificationKey(String serviceName, String status) {
        return String.format("status_%s_%s", serviceName, status);
    }

    /**
     * 알림 이력 정리
     * 
     * <p>오래된 알림 이력을 정리하여 메모리 사용량을 관리합니다.</p>
     * <p>1시간 이상 발송되지 않은 알림 이력을 삭제합니다.</p>
     */
    public void cleanupOldHistory() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minus(Duration.ofHours(1));
        
        notificationHistory.entrySet().removeIf(entry -> {
            boolean isOld = entry.getValue().getLastSentAt().isBefore(oneHourAgo);
            if (isOld) {
                log.debug("오래된 알림 이력 제거: {}", entry.getKey());
            }
            return isOld;
        });
        
        log.info("알림 이력 정리 완료 (현재 이력 수: {})", notificationHistory.size());
    }

    /**
     * 통계 정보 조회
     * 
     * @return 현재 관리 중인 알림 이력 수
     */
    public int getHistoryCount() {
        return notificationHistory.size();
    }

    /**
     * 알림 이력 내부 클래스
     */
    private static class NotificationHistory {
        private LocalDateTime lastSentAt;
        private int suppressedCount;

        public NotificationHistory(LocalDateTime lastSentAt) {
            this.lastSentAt = lastSentAt;
            this.suppressedCount = 0;
        }

        public LocalDateTime getLastSentAt() {
            return lastSentAt;
        }

        public void updateLastSentAt(LocalDateTime lastSentAt) {
            this.lastSentAt = lastSentAt;
        }

        public int getSuppressedCount() {
            return suppressedCount;
        }

        public void incrementSuppressedCount() {
            this.suppressedCount++;
        }

        public void resetSuppressedCount() {
            this.suppressedCount = 0;
        }
    }
}

