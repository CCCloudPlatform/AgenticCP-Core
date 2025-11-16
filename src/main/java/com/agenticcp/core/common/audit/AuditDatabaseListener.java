package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 감사 이벤트를 수신하여 데이터베이스에 저장하는 리스너입니다.
 * 민감 정보 마스킹 후 엔티티로 변환하여 저장합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditDatabaseListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final MaskingService maskingService;

    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditPublishEvent event) {
        try {
            AuditEventDto auditEventDto = event.getAuditEventDto();
            
            log.debug("감사 로그 DB 저장 시작 [Action: {}, RequestId: {}]", 
                     auditEventDto.action(), auditEventDto.requestId());

            // DB에 저장하기 직전, 민감 정보를 마스킹 처리
            maskingService.mask(auditEventDto.requestData());
            maskingService.mask(auditEventDto.oldValue());
            maskingService.mask(auditEventDto.newValue());
            maskingService.mask(auditEventDto.responseData());

            AuditLog auditLog = convertToEntity(auditEventDto);
            auditLogRepository.save(auditLog);

            log.debug("감사 로그 DB 저장 완료 [Action: {}, RequestId: {}, ID: {}]", 
                     auditEventDto.action(), auditEventDto.requestId(), auditLog.getId());

        } catch (Exception e) {
            log.error("감사 로그 DB 저장 실패 [Action: {}]: {}",
                     event.getAuditEventDto().action(), e.getMessage(), e);
        }
    }

    private AuditLog convertToEntity(AuditEventDto dto) {
        return AuditLog.builder()
                .action(dto.action())
                .resourceType(dto.resourceType())
                .httpMethod(dto.httpMethod())
                .requestPath(dto.requestPath())
                .operationSummary(dto.operationSummary())
                .controllerName(dto.controllerName())
                .methodName(dto.methodName())
                .severity(dto.severity())
                .timestamp(dto.timestamp())
                .requestId(dto.requestId())
                .tenantId(dto.tenantId())
                .userId(dto.userId())
                .clientIp(dto.clientIp())
                .success(dto.success())
                .error(dto.error())
                .requestData(toJson(dto.requestData()))
                .responseData(toJson(dto.responseData()))
                .metadata(toJson(dto.metadata()))
                .oldValue(toJson(dto.oldValue()))
                .newValue(toJson(dto.newValue()))
                .targetResourceId(dto.targetResourceId())
                .build();
    }

    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            return null;
        }
    }
}

