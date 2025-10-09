package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.AuditEventDto;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.security.entity.AuditLog;
import com.agenticcp.core.domain.security.repository.AuditLogRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 감사 로깅 전용 로거
 * 
 * 감사 이벤트를 JSON 형태로 로깅합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);
    
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    private static final Map<AuditSeverity, BiConsumer<Logger, String>> logActions
            = new EnumMap<>(AuditSeverity.class);

    static {
        logActions.put(AuditSeverity.CRITICAL, Logger::error);
        logActions.put(AuditSeverity.HIGH, Logger::warn);
        logActions.put(AuditSeverity.MEDIUM, Logger::info);
        logActions.put(AuditSeverity.LOW, Logger::info);
        logActions.put(AuditSeverity.INFO, Logger::info);
    }

    private final ObjectMapper objectMapper;

    public void log(AuditEventDto auditEvent) {
        log.info("[AuditLogger] log() called for action={} resourceType={}", 
                auditEvent.action(), auditEvent.resourceType());
        
        try {
            // 1. 로그 파일에 기록
            String jsonLog = objectMapper.writeValueAsString(auditEvent);
            logActions.getOrDefault(auditEvent.severity(), Logger::info)
                    .accept(auditLog, jsonLog);
            log.info("[AuditLogger] Log file written successfully");

            // 2. 데이터베이스에 저장
            saveToDatabase(auditEvent);
            log.info("[AuditLogger] Database save completed");

        } catch (JsonProcessingException e) {
            log.error("감사 이벤트 JSON 직렬화에 실패했습니다: {}", e.getMessage(), e);
        }
    }

    private void saveToDatabase(AuditEventDto auditEvent) {
        try {
            // AuditEventDto의 metadata에서 eventType, eventCategory 추출
            String eventTypeStr = (String) auditEvent.metadata().get("eventType");
            String eventCategoryStr = (String) auditEvent.metadata().get("eventCategory");
            
            AuditLog.EventType eventType = eventTypeStr != null ? 
                    AuditLog.EventType.valueOf(eventTypeStr) : AuditLog.EventType.CONFIGURATION_CHANGE;
            AuditLog.EventCategory eventCategory = eventCategoryStr != null ? 
                    AuditLog.EventCategory.valueOf(eventCategoryStr) : AuditLog.EventCategory.CONFIGURE;
            
            // Severity 매핑
            AuditLog.Severity severity = mapSeverity(auditEvent.severity());
            
            // User 엔티티 조회 (userId가 있는 경우) - 성능 최적화를 위해 병렬 처리
            User user = null;
            Tenant tenant = null;
            
            try {
                // User와 Tenant 조회를 병렬로 처리
                java.util.concurrent.CompletableFuture<User> userFuture = null;
                java.util.concurrent.CompletableFuture<Tenant> tenantFuture = null;
                
                if (auditEvent.userId() != null && !auditEvent.userId().trim().isEmpty()) {
                    try {
                        Long userId = Long.valueOf(auditEvent.userId());
                        userFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                            userRepository.findById(userId).orElse(null));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid userId format: {}", auditEvent.userId());
                    }
                }
                
                if (auditEvent.tenantId() != null && !auditEvent.tenantId().trim().isEmpty()) {
                    try {
                        Long tenantId = Long.valueOf(auditEvent.tenantId());
                        tenantFuture = java.util.concurrent.CompletableFuture.supplyAsync(() -> 
                            tenantRepository.findById(tenantId).orElse(null));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid tenantId format: {}", auditEvent.tenantId());
                    }
                }
                
                // 결과 대기 (최대 1초 타임아웃)
                if (userFuture != null) {
                    user = userFuture.get(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (user == null) {
                        log.warn("User not found for userId: {}", auditEvent.userId());
                    }
                }
                
                if (tenantFuture != null) {
                    tenant = tenantFuture.get(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (tenant == null) {
                        log.warn("Tenant not found for tenantId: {}", auditEvent.tenantId());
                    }
                }
                
            } catch (Exception e) {
                log.warn("Failed to fetch User/Tenant entities: {}", e.getMessage());
                // 조회 실패 시에도 감사 로그는 계속 진행
            }

            AuditLog auditLogEntity = AuditLog.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .eventType(eventType)
                    .eventCategory(eventCategory)
                    .severity(severity)
                    .eventName(auditEvent.operationSummary())
                    .description(auditEvent.operationSummary())
                    .resourceType(auditEvent.resourceType().name())
                    .resourceId((String) auditEvent.metadata().get("resourceId"))
                    .action(auditEvent.action())
                    .result(auditEvent.success() ? AuditLog.Result.SUCCESS : AuditLog.Result.FAILURE)
                    .ipAddress(auditEvent.clientIp())
                    .requestId(auditEvent.requestId())
                    .eventTimestamp(auditEvent.timestamp().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime())
                    .details(objectMapper.writeValueAsString(auditEvent.requestData()))
                    .user(user)  // 실제 DB에서 조회한 User 엔티티
                    .tenant(tenant) // 실제 DB에서 조회한 Tenant 엔티티
                    .build();

            auditLogRepository.save(auditLogEntity);
            log.debug("감사 로그가 데이터베이스에 저장되었습니다: eventId={}, resourceType={}, resourceId={}", 
                    auditLogEntity.getEventId(), auditLogEntity.getResourceType(), auditLogEntity.getResourceId());

        } catch (Exception e) {
            log.error("감사 로그 데이터베이스 저장에 실패했습니다: {}", e.getMessage(), e);
        }
    }
    
    private AuditLog.Severity mapSeverity(com.agenticcp.core.common.enums.AuditSeverity severity) {
        return switch (severity) {
            case CRITICAL -> AuditLog.Severity.FATAL;
            case HIGH -> AuditLog.Severity.ERROR;
            case MEDIUM -> AuditLog.Severity.WARN;
            case LOW, INFO -> AuditLog.Severity.INFO;
        };
    }
    
}
