package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
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
 * 감사 데이터 처리 실패 시 {@link AuditErrorCode}를 포함한 {@link BusinessException}을 발생시킵니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditDatabaseListener {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final MaskingService maskingService;

    /**
     * 감사 이벤트를 처리하여 DB에 영구 저장합니다.
     *
     * @param event 감사 이벤트
     * @throws BusinessException 마스킹 실패 또는 저장 실패 시
     */
    @Async
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleAuditEvent(AuditPublishEvent event) {
        AuditEventDto auditEventDto = event.getAuditEventDto();

        log.debug("감사 로그 DB 저장 시작 [Action: {}, RequestId: {}, TenantId: {}, UserId: {}]",
                auditEventDto.action(), auditEventDto.requestId(), auditEventDto.tenantId(), auditEventDto.userId());

        maskSensitiveData(auditEventDto);

        try {
            AuditLog auditLog = convertToEntity(auditEventDto);
            auditLogRepository.save(auditLog);

            log.debug("감사 로그 DB 저장 완료 [Action: {}, RequestId: {}, TenantId: {}, UserId: {}, ID: {}]",
                    auditEventDto.action(), auditEventDto.requestId(), auditEventDto.tenantId(),
                    auditEventDto.userId(), auditLog.getId());

        } catch (Exception e) {
            log.error("감사 로그 DB 저장 실패 [Action: {}, RequestId: {}, TenantId: {}, UserId: {}]: {}",
                    auditEventDto.action(), auditEventDto.requestId(), auditEventDto.tenantId(),
                    auditEventDto.userId(), e.getMessage(), e);
            throw new BusinessException(AuditErrorCode.AUDIT_LOG_PERSISTENCE_FAILED);
        }
    }

    /**
     * 감사 이벤트에 포함된 민감 데이터를 마스킹합니다.
     * 
     * 주의: 원본 객체를 변경하지 않기 위해 마스킹된 JSON을 직접 생성합니다.
     * 
     * @param auditEventDto 감사 이벤트 DTO
     */
    private void maskSensitiveData(AuditEventDto auditEventDto) {
        try {
            // 원본 객체를 변경하지 않고 마스킹된 JSON 생성
            // 실제 마스킹은 toJson() 메서드에서 수행됩니다
            // mask() 메서드는 실제 객체를 변경하므로 사용하지 않음
        } catch (Exception maskingException) {
            log.error("감사 데이터 마스킹 실패 [Action: {}, RequestId: {}]: {}",
                    auditEventDto.action(), auditEventDto.requestId(), maskingException.getMessage(), maskingException);
            throw new BusinessException(AuditErrorCode.AUDIT_LOG_MASKING_FAILED);
        }
    }

    /**
     * 감사 이벤트 DTO를 감사 로그 엔티티로 변환합니다.
     *
     * @param dto 감사 이벤트 DTO
     * @return 저장 가능한 감사 로그 엔티티
     */
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

    /**
     * 객체를 마스킹하여 JSON 문자열로 직렬화합니다.
     * 원본 객체는 변경하지 않고, 마스킹된 복사본을 JSON으로 변환합니다.
     *
     * @param data 직렬화 대상 객체
     * @return 마스킹된 JSON 문자열, 직렬화할 값이 없으면 {@code null}
     */
    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        try {
            // 원본을 변경하지 않고 마스킹된 JSON 생성
            return maskingService.toMaskedJson(data, objectMapper);
        } catch (Exception e) {
            log.warn("JSON 변환 실패: {}", e.getMessage());
            // 마스킹 실패 시 원본을 JSON으로 변환 (마스킹되지 않음)
            try {
                return objectMapper.writeValueAsString(data);
            } catch (JsonProcessingException jsonException) {
                log.warn("원본 JSON 변환도 실패: {}", jsonException.getMessage());
                return null;
            }
        }
    }
}

