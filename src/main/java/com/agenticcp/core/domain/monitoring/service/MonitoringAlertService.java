package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.entity.Alert;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.monitoring.entity.MetricThreshold;
import com.agenticcp.core.domain.monitoring.enums.AlertStatus;
import com.agenticcp.core.domain.monitoring.enums.AlertType;
import com.agenticcp.core.domain.monitoring.enums.Severity;
import com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent;
import com.agenticcp.core.domain.monitoring.event.ThresholdExceededEvent;
import com.agenticcp.core.domain.monitoring.repository.AlertRepository;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import com.agenticcp.core.domain.notification.service.NotificationDeDuplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 모니터링 알림 서비스 (이벤트 기반)
 * 
 * <p>Issue #15 가이드: 이벤트 리스너 기반 알림 처리</p>
 * <p>임계값 초과 및 헬스 상태 변화 이벤트를 수신하여 Alert를 생성하고 알림을 발송합니다.</p>
 * 
 * <h3>주요 기능:</h3>
 * <ul>
 *   <li>임계값 초과 이벤트 처리 (시나리오 1)</li>
 *   <li>헬스 상태 변화 이벤트 처리 (시나리오 2)</li>
 *   <li>NotificationDeDuplicationService 활용 중복 알림 방지 (5분 윈도우)</li>
 *   <li>Alert 엔티티 저장 (이력 관리)</li>
 * </ul>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringAlertService {
    
    private final MonitoringNotificationService monitoringNotificationService;
    private final AlertRepository alertRepository;
    private final NotificationDeDuplicationService deDuplicationService;
    
    /**
     * 기본 쿨다운 기간 (5분)
     */
    private static final Duration DEFAULT_COOLDOWN = Duration.ofMinutes(5);
    
    /**
     * 임계값 초과 이벤트 처리 (시나리오 1)
     * 
     * <p>Given: CPU 사용률이 90%를 초과함</p>
     * <p>When: 임계값 체크 수행</p>
     * <p>Then: 관리자에게 즉시 알림이 발송됨</p>
     * 
     * @param event 임계값 초과 이벤트
     */
    @EventListener
    @Transactional
    public void handleThresholdExceeded(ThresholdExceededEvent event) {
        MetricThreshold threshold = event.getThreshold();
        Metric metric = event.getMetric();
        
        log.info("🚨 임계값 초과 이벤트 수신: {} = {} (임계값: {})", 
            metric.getMetricName(), metric.getMetricValue(), threshold.getThresholdValue());
        
        // 알림 중복 방지 체크 (시나리오 3)
        if (isDuplicateAlert(threshold, metric)) {
            log.debug("⏸️ 중복 알림 방지: {} (쿨다운 기간 내)", metric.getMetricName());
            return;
        }
        
        // Alert 엔티티 생성 및 저장
        Alert alert = createThresholdAlert(threshold, metric);
        Alert savedAlert = alertRepository.save(alert);
        log.info("📝 Alert 저장 완료: id={}, name={}", savedAlert.getId(), savedAlert.getAlertName());
        
        // 알림 발송 (기존 MonitoringNotificationService 재사용)
        monitoringNotificationService.sendThresholdViolationAlert(
            metric, 
            threshold.getThresholdValue(), 
            threshold.getOperator()
        );
        
        // 알림 발송 기록 (NotificationDeDuplicationService)
        recordAlertSent(threshold, metric);
        log.info("✅ 임계값 초과 알림 처리 완료: {}", metric.getMetricName());
    }
    
    /**
     * 헬스 상태 변화 이벤트 처리 (시나리오 2)
     * 
     * <p>Given: 데이터베이스 연결이 실패함</p>
     * <p>When: 헬스체크 수행</p>
     * <p>Then: CRITICAL 상태로 변경되고 긴급 알림이 발송됨</p>
     * 
     * @param event 헬스 상태 변화 이벤트
     */
    @EventListener
    @Transactional
    public void handleHealthStatusChanged(HealthStatusChangedEvent event) {
        log.info("🔄 헬스 상태 변화 이벤트 수신: {} {} -> {}", 
            event.getServiceName(), event.getPreviousStatus(), event.getNewStatus());
        
        // CRITICAL 또는 WARNING 상태면 Alert 생성
        if ("CRITICAL".equals(event.getNewStatus()) || "WARNING".equals(event.getNewStatus())) {
            Alert alert = createHealthAlert(event);
            Alert savedAlert = alertRepository.save(alert);
            log.info("📝 Alert 저장 완료: id={}, name={}", savedAlert.getId(), savedAlert.getAlertName());
            
            // 긴급 알림 발송
            monitoringNotificationService.sendSystemStatusChangeAlert(
                event.getServiceName(),
                event.getPreviousStatus(),
                event.getNewStatus(),
                event.getTenantId()
            );
            
            log.info("✅ 헬스 상태 변화 알림 처리 완료: {}", event.getServiceName());
        }
    }
    
    /**
     * 중복 알림 체크 (NotificationDeDuplicationService 사용)
     * 
     * <p>Given: 동일한 알림이 연속으로 발생함</p>
     * <p>When: 알림 발송 시도</p>
     * <p>Then: 중복 알림은 발송되지 않고 알림 빈도가 조절됨</p>
     * 
     * @param threshold 임계값 규칙
     * @param metric 메트릭 값
     * @return 중복 여부 (true: 중복, false: 발송 가능)
     */
    private boolean isDuplicateAlert(MetricThreshold threshold, Metric metric) {
        String key = generateAlertKey(threshold, metric);
        
        // NotificationDeDuplicationService 사용
        boolean canSend = deDuplicationService.canSendNotification(key, DEFAULT_COOLDOWN);
        
        if (!canSend) {
            log.debug("⏸️ 중복 알림 방지: {} (쿨다운 기간 내)", key);
        }
        
        return !canSend; // canSend=false면 중복=true
    }
    
    /**
     * 알림 발송 기록 (NotificationDeDuplicationService 사용)
     * 
     * @param threshold 임계값 규칙
     * @param metric 메트릭 값
     */
    private void recordAlertSent(MetricThreshold threshold, Metric metric) {
        String key = generateAlertKey(threshold, metric);
        
        // NotificationDeDuplicationService 사용
        deDuplicationService.recordNotificationSent(key);
        
        log.debug("알림 발송 기록: {}", key);
    }
    
    
    /**
     * Alert 키 생성
     */
    private String generateAlertKey(MetricThreshold threshold, Metric metric) {
        return String.format("alert_sent:%d:%s:%s", 
            threshold.getId(), 
            metric.getTenantId(),
            metric.getMetricName());
    }
    
    /**
     * 임계값 Alert 생성
     * 
     * @param threshold 임계값 규칙
     * @param metric 메트릭 값
     * @return Alert 엔티티
     */
    private Alert createThresholdAlert(MetricThreshold threshold, Metric metric) {
        Severity severity = determineSeverity(threshold);
        
        return Alert.builder()
                .tenantId(metric.getTenantId())
                .alertName(String.format("메트릭 임계값 초과: %s", metric.getMetricName()))
                .description(String.format(
                    "%s가 임계값 %.2f %s를 초과했습니다. 현재값: %.2f %s", 
                    metric.getMetricName(), 
                    threshold.getThresholdValue(),
                    metric.getUnit() != null ? metric.getUnit() : "",
                    metric.getMetricValue(),
                    metric.getUnit() != null ? metric.getUnit() : ""
                ))
                .alertType(AlertType.THRESHOLD)
                .severity(severity)
                .status(AlertStatus.TRIGGERED)
                .isEnabled(true)
                .cooldownPeriod((int) DEFAULT_COOLDOWN.toSeconds())
                .lastTriggered(LocalDateTime.now())
                .triggerCount(1)
                .build();
    }
    
    /**
     * 헬스 Alert 생성
     * 
     * @param event 헬스 상태 변화 이벤트
     * @return Alert 엔티티
     */
    private Alert createHealthAlert(HealthStatusChangedEvent event) {
        Severity severity = "CRITICAL".equals(event.getNewStatus()) 
            ? Severity.CRITICAL 
            : Severity.WARNING;
        
        return Alert.builder()
                .tenantId(event.getTenantId())
                .alertName(String.format("서비스 장애: %s", event.getServiceName()))
                .description(String.format(
                    "서비스 %s의 상태가 %s에서 %s로 변경되었습니다", 
                    event.getServiceName(), 
                    event.getPreviousStatus(), 
                    event.getNewStatus()
                ))
                .alertType(AlertType.AVAILABILITY)
                .severity(severity)
                .status(AlertStatus.TRIGGERED)
                .isEnabled(true)
                .lastTriggered(LocalDateTime.now())
                .triggerCount(1)
                .build();
    }
    
    /**
     * Severity 결정
     * 
     * @param threshold 임계값 규칙
     * @return Severity
     */
    private Severity determineSeverity(MetricThreshold threshold) {
        return switch (threshold.getThresholdType()) {
            case CRITICAL -> Severity.CRITICAL;
            case WARNING -> Severity.WARNING;
            case INFO -> Severity.INFO;
        };
    }
}

