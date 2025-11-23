package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.notification.config.NotificationChannelConfig;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.ChannelType;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.monitoring.entity.Metric;
import com.agenticcp.core.domain.notification.enums.NotificationPriority;
import com.agenticcp.core.domain.notification.enums.NotificationType;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 모니터링 도메인과 연동되는 알림 서비스
 * 
 * <p>메트릭 임계값 위반, 시스템 상태 변화, 수집 실패 등을 감지하여
 * 적절한 알림을 발송합니다.</p>
 * 
 * <p>feature/39 브랜치의 모니터링 도메인과 연동됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MonitoringNotificationService {

    private final NotificationService notificationService;
    private final NotificationChannelRepository channelRepository;
    private final NotificationChannelConfig channelConfig;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    /**
     * 메트릭 임계값 위반 알림 발송
     * 
     * <p>MetricsCollectionService.checkThresholdViolations()에서 호출됩니다.</p>
     * 
     * @param metric 메트릭 엔티티 (메트릭 정보 포함)
     * @param thresholdValue 임계값
     * @param operator 비교 연산자 (>, <, >=, <=, ==)
     */
    @Transactional
    public void sendThresholdViolationAlert(Metric metric, Double thresholdValue, String operator) {
        try {
            log.warn("[MonitoringNotificationService] sendThresholdViolationAlert - 임계값 위반 감지: {} {} {} {} (tenantId: {})", 
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

            // 채널 ID 조회 (설정 기반 - 확장 가능)
            String channelId = getChannelIdForNotification(
                metric.getTenantId(), 
                NotificationType.ALERT, 
                priority
            );
            
            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("threshold", metric.getMetricName()))
                    .tenantId(metric.getTenantId())
                    .userId(getTenantAdminUserId(metric.getTenantId()))  // 테넌트별 관리자 ID 조회
                    .channelId(channelId)  // 설정 기반으로 동적 조회 (SLACK/DISCORD 등)
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
                log.info("[MonitoringNotificationService] sendThresholdViolationAlert - success notificationId={}", request.getNotificationId());
            } else {
                log.error("[MonitoringNotificationService] sendThresholdViolationAlert - 실패 notificationId={}, error={}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[MonitoringNotificationService] sendThresholdViolationAlert - 임계값 위반 알림 발송 중 오류 발생: metricName={}, tenantId={}", 
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
    @Transactional
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
            log.error("[MonitoringNotificationService] sendThresholdViolationAlert - 임계값 위반 알림 발송 중 오류 발생 (legacy): metricName={}, tenantId={}", 
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
    @Transactional
    public void sendCollectionFailureAlert(String collectorType, String errorMessage, String tenantId) {
        try {
            log.error("[MonitoringNotificationService] sendCollectionFailureAlert - 메트릭 수집 실패: {} (tenantId: {})", collectorType, tenantId);

            // 알림 데이터 구성
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("collectorType", collectorType);
            alertData.put("errorMessage", errorMessage);
            alertData.put("failureTime", LocalDateTime.now());

            // 채널 ID 조회 (설정 기반)
            String channelId = getChannelIdForNotification(
                tenantId, 
                NotificationType.SYSTEM, 
                NotificationPriority.HIGH
            );
            
            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("collection_failure", collectorType))
                    .tenantId(tenantId)
                    .userId(getTenantAdminUserId(tenantId))  // 테넌트별 관리자 ID 조회
                    .channelId(channelId)  // 설정 기반으로 동적 조회
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
                log.info("[MonitoringNotificationService] sendCollectionFailureAlert - success notificationId={}", request.getNotificationId());
            } else {
                log.error("[MonitoringNotificationService] sendCollectionFailureAlert - 실패 notificationId={}, error={}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[MonitoringNotificationService] sendCollectionFailureAlert - 메트릭 수집 실패 알림 발송 중 오류 발생: collectorType={}, tenantId={}", 
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
    @Transactional
    public void sendSystemStatusChangeAlert(String serviceName, String previousStatus, 
                                          String currentStatus, String tenantId) {
        try {
            log.warn("[MonitoringNotificationService] sendSystemStatusChangeAlert - 시스템 상태 변화: {} {} -> {} (tenantId: {})", 
                serviceName, previousStatus, currentStatus, tenantId);

            // 알림 우선순위 결정
            NotificationPriority priority = determineSystemStatusPriority(currentStatus);

            // 알림 데이터 구성
            Map<String, Object> alertData = new HashMap<>();
            alertData.put("serviceName", serviceName);
            alertData.put("previousStatus", previousStatus);
            alertData.put("currentStatus", currentStatus);
            alertData.put("changeTime", LocalDateTime.now());

            // 채널 ID 조회 (설정 기반)
            String channelId = getChannelIdForNotification(
                tenantId, 
                NotificationType.SYSTEM, 
                priority
            );
            
            // 알림 요청 생성
            NotificationRequest request = NotificationRequest.builder()
                    .notificationId(generateNotificationId("status_change", serviceName))
                    .tenantId(tenantId)
                    .userId(getTenantAdminUserId(tenantId))  // 테넌트별 관리자 ID 조회
                    .channelId(channelId)  // 설정 기반으로 동적 조회
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
                log.info("[MonitoringNotificationService] sendSystemStatusChangeAlert - success notificationId={}", request.getNotificationId());
            } else {
                log.error("[MonitoringNotificationService] sendSystemStatusChangeAlert - 실패 notificationId={}, error={}", 
                    request.getNotificationId(), response.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("[MonitoringNotificationService] sendSystemStatusChangeAlert - 시스템 상태 변화 알림 발송 중 오류 발생: serviceName={}, tenantId={}", 
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

    /**
     * 알림용 채널 ID 조회 (설정 기반 - 확장 가능)
     * 
     * <p>알림 타입과 우선순위에 따라 적절한 채널을 자동 선택합니다.</p>
     * <p>디스코드, 텔레그램 등 새로운 채널 추가 시 코드 수정 없이 설정만 변경하면 됩니다.</p>
     * 
     * <h3>선택 로직:</h3>
     * <ol>
     *   <li>우선순위 or 알림 타입 기반 채널 결정</li>
     *   <li>해당 채널 조회</li>
     *   <li>폴백 채널 시도 (선택사항)</li>
     *   <li>최종 폴백 (ID 1)</li>
     * </ol>
     * 
     * @param tenantId 테넌트 ID
     * @param notificationType 알림 타입 (ALERT, SYSTEM 등)
     * @param priority 우선순위 (URGENT, HIGH 등)
     * @return 채널 ID (문자열)
     */
    private String getChannelIdForNotification(String tenantId, NotificationType notificationType, 
                                              NotificationPriority priority) {
        
        // 1. 설정에서 채널 타입 결정 (우선순위 고려)
        ChannelType targetChannel = determineChannelType(notificationType, priority);
        
        // 2. 해당 채널 조회
        String channelId = findChannelId(tenantId, targetChannel);
        if (channelId != null) {
            log.debug("[MonitoringNotificationService] getChannelIdForNotification - 채널 사용: {} (타입: {}, 우선순위: {})", targetChannel, notificationType, priority);
            return channelId;
        }
        
        // 3. 폴백 채널 시도 (설정에 따라)
        if (channelConfig.getFallbackOrder() != null && !channelConfig.getFallbackOrder().isEmpty()) {
            for (ChannelType fallback : channelConfig.getFallbackOrder()) {
                channelId = findChannelId(tenantId, fallback);
                if (channelId != null) {
                    log.warn("[MonitoringNotificationService] getChannelIdForNotification - 폴백 채널 사용: {} → {} (원래: {})", targetChannel, fallback, notificationType);
                    return channelId;
                }
            }
        }
        
        // 4. 최종 폴백: ID 1 (기본 채널)
        log.error("[MonitoringNotificationService] getChannelIdForNotification - 사용 가능한 채널을 찾을 수 없습니다. 기본 채널 ID '1'을 사용합니다. tenantId: {}", tenantId);
        return "1";
    }

    /**
     * 채널 타입 결정 (우선순위 고려)
     * 
     * <p>우선순위 매핑이 있으면 우선 사용하고, 없으면 알림 타입 매핑 사용</p>
     * 
     * @param notificationType 알림 타입
     * @param priority 우선순위
     * @return 채널 타입
     */
    private ChannelType determineChannelType(NotificationType notificationType, NotificationPriority priority) {
        // 1. 우선순위 매핑 확인 (있으면 우선 사용)
        ChannelType priorityChannel = channelConfig.getChannelTypeForPriority(priority.name());
        if (priorityChannel != null) {
            log.debug("[MonitoringNotificationService] determineChannelType - 우선순위 기반 채널 선택: {} ({})", priorityChannel, priority);
            return priorityChannel;
        }
        
        // 2. 알림 타입 매핑 사용 (우선순위 매핑 없으면)
        ChannelType typeChannel = channelConfig.getChannelTypeForNotification(notificationType);
        log.debug("[MonitoringNotificationService] determineChannelType - 알림 타입 기반 채널 선택: {} ({})", typeChannel, notificationType);
        return typeChannel;
    }

    /**
     * 채널 타입으로 채널 ID 찾기
     * 
     * @param tenantId 테넌트 ID
     * @param channelType 채널 타입
     * @return 채널 ID (없으면 null)
     */
    private String findChannelId(String tenantId, ChannelType channelType) {
        return channelRepository
                .findByTenantIdAndChannelTypeAndIsActiveTrueAndIsDeletedFalse(tenantId, channelType)
                .stream()
                .findFirst()
                .map(channel -> String.valueOf(channel.getId()))
                .orElse(null);
    }

    /**
     * 테넌트별 관리자 사용자 ID 조회
     * 
     * <p>해당 테넌트의 TENANT_ADMIN 역할을 가진 사용자를 조회합니다.</p>
     * <p>관리자가 여러 명인 경우 첫 번째 관리자를 반환합니다.</p>
     * 
     * @param tenantId 테넌트 ID (tenantKey)
     * @return 관리자 사용자 ID
     * @throws RuntimeException 테넌트를 찾을 수 없거나 관리자가 없는 경우
     */
    private Long getTenantAdminUserId(String tenantId) {
        // 1. 테넌트 조회
        Optional<Tenant> tenantOpt = tenantRepository.findByTenantKey(tenantId);
        if (tenantOpt.isEmpty()) {
            log.error("[MonitoringNotificationService] getTenantAdminUserId - 테넌트를 찾을 수 없습니다: {}", tenantId);
            // 폴백: 설정 파일의 기본 관리자 ID 사용
            return channelConfig.getDefaultAdminUserId();
        }
        
        Tenant tenant = tenantOpt.get();
        
        // 2. 테넌트의 관리자 조회 (TENANT_ADMIN 역할)
        Optional<User> adminOpt = userRepository
                .findActiveUsersByTenant(tenant, Status.ACTIVE)
                .stream()
                .filter(user -> user.getRole() == UserRole.TENANT_ADMIN)
                .findFirst();
        
        if (adminOpt.isEmpty()) {
            log.warn("[MonitoringNotificationService] getTenantAdminUserId - 테넌트 {}의 관리자를 찾을 수 없습니다. 기본 관리자 ID 사용: {}", 
                tenantId, channelConfig.getDefaultAdminUserId());
            // 폴백: 설정 파일의 기본 관리자 ID 사용
            return channelConfig.getDefaultAdminUserId();
        }
        
        Long adminUserId = adminOpt.get().getId();
        log.debug("[MonitoringNotificationService] getTenantAdminUserId - 테넌트 {} 관리자: userId={}, name={}", 
            tenantId, adminUserId, adminOpt.get().getName());
        
        return adminUserId;
    }
}
