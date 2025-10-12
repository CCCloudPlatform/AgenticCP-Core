package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 모니터링 도메인과 연동되는 알림 서비스
 * 
 * <p>메트릭 임계값 위반, 시스템 상태 변화, 수집 실패 등을 감지하여
 * 적절한 알림을 발송합니다.</p>
 * 
 * <p>feature/39 브랜치의 모니터링 도메인과 연동됩니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringNotificationService {

    private final NotificationService notificationService;

    /**
     * 메트릭 임계값 위반 알림 발송
     * 
     * <p>MetricsCollectionService.checkThresholdViolations()에서 호출됩니다.</p>
     * 
     * @param metric 메트릭 엔티티 (메트릭 정보 포함)
     * @param thresholdValue 임계값
     * @param operator 비교 연산자 (>, <, >=, <=, ==)
     */
    public void sendThresholdViolationAlert(Metric metric, Double thresholdValue, String operator) {
        try {
            log.warn("🚨 임계값 위반 감지: {} {} {} {} (tenantId: {})", 
                metric.getMetricName(), metric.getMetricValue(), operator, thresholdValue, metric.getTenantId());

            // 알림 우선순위 결정
            NotificationPriority priority = determinePriority(metric, thresholdValue);

            // 알림 데이터 구성
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("metricName", metric.getMetricName());
            alertData.put("metricValue", metric.getMetricValue());
            alertData.put("metricType", metric.getMetricType());
            alertData.put("unit", metric.getUnit());
            alertData.put("source", metric.getSource());
            alertData.put("collectedAt", metric.getCollectedAt());
            alertData.put("thresholdValue", thresholdValue);
            alertData.put("operator", operator);
            alertData.put("violationTime", LocalDateTime.now());
            alertData.put("severity", priority.name());

            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("threshold", metric.getMetricName()))
                    .tenantId(metric.getTenantId())
                    .title("메트릭 임계값 위반 알림")
                    .content(buildThresholdViolationContent(metric, thresholdValue, operator))
                    .type(NotificationType.ALERT)
                    .priority(priority)
                    .data(alertData)
                    .metadata(Map.of("source", "monitoring", "type", "threshold_violation"))
                    .build();

            // 알림 발송
            NotificationResponse response = notificationService.sendNotification(request);
            
            if (response.isSuccess()) {
                log.info("임계값 위반 알림 발송 성공: {}", request.getNotificationId());
            } else {
                log.error("임계값 위반 알림 발송 실패: {}, 에러: {}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("임계값 위반 알림 발송 중 오류 발생: metricName={}, tenantId={}", 
                metric.getMetricName(), metric.getTenantId(), e);
        }
    }

    /**
     * 메트릭 임계값 위반 알림 발송 (기존 호환성을 위한 메서드)
     * 
     * @param metricName 메트릭명 (예: cpu.usage, memory.usage)
     * @param metricValue 현재 메트릭 값
     * @param thresholdValue 임계값
     * @param operator 비교 연산자 (>, <, >=, <=, ==)
     * @param tenantId 테넌트 ID
     * @deprecated Metric 엔티티 기반 메서드 사용 권장
     */
    @Deprecated
    public void sendThresholdViolationAlert(String metricName, Double metricValue, 
                                           Double thresholdValue, String operator, String tenantId) {
        try {
            // 임시 Metric 객체 생성 (기존 호환성을 위해)
            Metric tempMetric = Metric.builder()
                    .metricName(metricName)
                    .metricValue(metricValue)
                    .metricType(Metric.MetricType.SYSTEM)
                    .collectedAt(LocalDateTime.now())
                    .source("legacy")
                    .tenantId(tenantId)
                    .build();
            
            sendThresholdViolationAlert(tempMetric, thresholdValue, operator);
        } catch (Exception e) {
            log.error("임계값 위반 알림 발송 중 오류 발생 (legacy): metricName={}, tenantId={}", 
                metricName, tenantId, e);
        }
    }

    /**
     * 메트릭 수집 실패 알림 발송
     * 
     * <p>MetricsCollectionService의 재시도 실패 시 호출됩니다.</p>
     * 
     * @param collectorType 수집기 타입 (SYSTEM, APPLICATION, CUSTOM)
     * @param errorMessage 오류 메시지
     * @param tenantId 테넌트 ID
     */
    public void sendCollectionFailureAlert(String collectorType, String errorMessage, String tenantId) {
        try {
            log.error("📊 메트릭 수집 실패: {} (tenantId: {})", collectorType, tenantId);

            // 알림 데이터 구성
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("collectorType", collectorType);
            alertData.put("errorMessage", errorMessage);
            alertData.put("failureTime", LocalDateTime.now());

            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("collection_failure", collectorType))
                    .tenantId(tenantId)
                    .title("메트릭 수집 실패 알림")
                    .content(buildCollectionFailureContent(collectorType, errorMessage))
                    .type(NotificationType.SYSTEM)
                    .priority(NotificationPriority.HIGH)
                    .data(alertData)
                    .metadata(Map.of("source", "monitoring", "type", "collection_failure"))
                    .build();

            // 알림 발송
            NotificationResponse response = notificationService.sendNotification(request);
            
            if (response.isSuccess()) {
                log.info("메트릭 수집 실패 알림 발송 성공: {}", request.getNotificationId());
            } else {
                log.error("메트릭 수집 실패 알림 발송 실패: {}, 에러: {}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("메트릭 수집 실패 알림 발송 중 오류 발생: collectorType={}, tenantId={}", 
                collectorType, tenantId, e);
        }
    }

    /**
     * 시스템 상태 변화 알림 발송
     * 
     * <p>PlatformHealth 엔티티의 상태 변화를 감지하여 알림을 발송합니다.</p>
     * 
     * @param serviceName 서비스명
     * @param previousStatus 이전 상태
     * @param currentStatus 현재 상태
     * @param tenantId 테넌트 ID
     */
    public void sendSystemStatusChangeAlert(String serviceName, String previousStatus, 
                                          String currentStatus, String tenantId) {
        try {
            log.warn("🔄 시스템 상태 변화: {} {} -> {} (tenantId: {})", 
                serviceName, previousStatus, currentStatus, tenantId);

            // 알림 우선순위 결정
            NotificationPriority priority = determineSystemStatusPriority(currentStatus);

            // 알림 데이터 구성
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("serviceName", serviceName);
            alertData.put("previousStatus", previousStatus);
            alertData.put("currentStatus", currentStatus);
            alertData.put("changeTime", LocalDateTime.now());

            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("status_change", serviceName))
                    .tenantId(tenantId)
                    .title("시스템 상태 변화 알림")
                    .content(buildSystemStatusChangeContent(serviceName, previousStatus, currentStatus))
                    .type(NotificationType.SYSTEM)
                    .priority(priority)
                    .data(alertData)
                    .metadata(Map.of("source", "monitoring", "type", "status_change"))
                    .build();

            // 알림 발송
            NotificationResponse response = notificationService.sendNotification(request);
            
            if (response.isSuccess()) {
                log.info("시스템 상태 변화 알림 발송 성공: {}", request.getNotificationId());
            } else {
                log.error("시스템 상태 변화 알림 발송 실패: {}, 에러: {}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("시스템 상태 변화 알림 발송 중 오류 발생: serviceName={}, tenantId={}", 
                serviceName, tenantId, e);
        }
    }

    /**
     * 메트릭 정보에 따른 우선순위 결정
     */
    private NotificationPriority determinePriority(Metric metric, Double thresholdValue) {
        String metricName = metric.getMetricName();
        Double metricValue = metric.getMetricValue();
        
        // CPU, 메모리, 디스크 사용률에 따른 우선순위 결정
        if (metricName.contains("cpu.usage") || metricName.contains("memory.usage") || metricName.contains("disk.usage")) {
            if (metricValue >= 90.0) {
                return NotificationPriority.URGENT;
            } else if (metricValue >= 80.0) {
                return NotificationPriority.HIGH;
            } else if (metricValue >= 70.0) {
                return NotificationPriority.MEDIUM;
            }
        }
        
        // 메트릭 타입별 우선순위 조정
        if (metric.getMetricType() == Metric.MetricType.SYSTEM) {
            // 시스템 메트릭은 더 높은 우선순위
            if (metricValue >= thresholdValue * 1.2) {
                return NotificationPriority.HIGH;
            }
        }
        
        // 기본값
        return NotificationPriority.MEDIUM;
    }

    /**
     * 시스템 상태에 따른 우선순위 결정
     */
    private NotificationPriority determineSystemStatusPriority(String status) {
        return switch (status.toUpperCase()) {
            case "CRITICAL" -> NotificationPriority.URGENT;
            case "WARNING" -> NotificationPriority.HIGH;
            case "HEALTHY" -> NotificationPriority.LOW;
            default -> NotificationPriority.MEDIUM;
        };
    }

    /**
     * 임계값 위반 알림 내용 생성
     */
    private String buildThresholdViolationContent(Metric metric, Double thresholdValue, String operator) {
        return String.format(
            "메트릭 '%s'의 값이 임계값을 초과했습니다.\n\n" +
            "메트릭 정보:\n" +
            "  - 이름: %s\n" +
            "  - 타입: %s\n" +
            "  - 현재 값: %.2f %s\n" +
            "  - 임계값: %s %.2f\n" +
            "  - 소스: %s\n" +
            "  - 수집 시간: %s\n" +
            "  - 위반 시간: %s",
            metric.getMetricName(),
            metric.getMetricName(),
            metric.getMetricType(),
            metric.getMetricValue(),
            metric.getUnit() != null ? metric.getUnit() : "",
            operator,
            thresholdValue,
            metric.getSource(),
            metric.getCollectedAt(),
            LocalDateTime.now()
        );
    }

    /**
     * 수집 실패 알림 내용 생성
     */
    private String buildCollectionFailureContent(String collectorType, String errorMessage) {
        return String.format(
            "메트릭 수집기 '%s'에서 오류가 발생했습니다.\n\n" +
            "오류 메시지: %s\n" +
            "실패 시간: %s",
            collectorType, errorMessage, LocalDateTime.now()
        );
    }

    /**
     * 시스템 상태 변화 알림 내용 생성
     */
    private String buildSystemStatusChangeContent(String serviceName, String previousStatus, String currentStatus) {
        return String.format(
            "서비스 '%s'의 상태가 변경되었습니다.\n\n" +
            "이전 상태: %s\n" +
            "현재 상태: %s\n" +
            "변경 시간: %s",
            serviceName, previousStatus, currentStatus, LocalDateTime.now()
        );
    }

    /**
     * 알림 ID 생성
     */
    private String generateNotificationId(String type, String identifier) {
        return String.format("%s_%s_%s_%d", 
            type, identifier, LocalDateTime.now().toString().replace(":", "-"), 
            System.currentTimeMillis() % 10000);
    }
}
