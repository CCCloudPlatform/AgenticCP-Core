package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.logging.masking.MaskingService;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditDatabaseListener 단위 테스트")
class AuditDatabaseListenerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private MaskingService maskingService;

    @InjectMocks
    private AuditDatabaseListener auditDatabaseListener;

    private AuditEventDto baseAuditEventDto;

    @BeforeEach
    void setUp() throws Exception {
        baseAuditEventDto = buildAuditEventDto();
    }

    @Test
    @DisplayName("감사 이벤트 처리 성공 시 저장 로직 수행")
    void handleAuditEvent_WithValidEvent_PersistsAuditLog() {
        // Given
        AuditPublishEvent event = new AuditPublishEvent(this, baseAuditEventDto);

        // When
        auditDatabaseListener.handleAuditEvent(event);

        // Then
        verify(maskingService, times(4)).mask(baseAuditEventDto.requestData());

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(baseAuditEventDto.action());
        assertThat(savedLog.getTenantId()).isEqualTo(baseAuditEventDto.tenantId());
        assertThat(savedLog.getUserId()).isEqualTo(baseAuditEventDto.userId());
    }

    @Test
    @DisplayName("마스킹 실패 시 BusinessException 발생")
    void handleAuditEvent_WhenMaskingFails_ThrowsBusinessException() {
        // Given
        AuditPublishEvent event = new AuditPublishEvent(this, baseAuditEventDto);
        doThrow(new RuntimeException("mask fail")).when(maskingService).mask(baseAuditEventDto.requestData());

        // When & Then
        assertThatThrownBy(() -> auditDatabaseListener.handleAuditEvent(event))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuditErrorCode.AUDIT_LOG_MASKING_FAILED.getMessage())
                .extracting("errorCode")
                .isEqualTo(AuditErrorCode.AUDIT_LOG_MASKING_FAILED);
    }

    @Test
    @DisplayName("저장 실패 시 BusinessException 발생")
    void handleAuditEvent_WhenPersistenceFails_ThrowsBusinessException() {
        // Given
        AuditPublishEvent event = new AuditPublishEvent(this, baseAuditEventDto);
        doThrow(new RuntimeException("db fail")).when(auditLogRepository).save(any(AuditLog.class));

        // When & Then
        assertThatThrownBy(() -> auditDatabaseListener.handleAuditEvent(event))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuditErrorCode.AUDIT_LOG_PERSISTENCE_FAILED.getMessage())
                .extracting("errorCode")
                .isEqualTo(AuditErrorCode.AUDIT_LOG_PERSISTENCE_FAILED);
    }

    private AuditEventDto buildAuditEventDto() {
        Map<String, Object> sampleData = Map.of("key", "value");

        return new AuditEventDto(
                "USER_CREATE",
                AuditResourceType.USER,
                "POST",
                "/api/v1/users",
                "사용자 생성",
                "UserController",
                "createUser",
                AuditSeverity.INFO,
                Instant.now(),
                "req-1",
                "tenant-1",
                "user-1",
                "127.0.0.1",
                true,
                null,
                sampleData,
                sampleData,
                sampleData,
                sampleData,
                sampleData,
                "resource-1"
        );
    }
}

