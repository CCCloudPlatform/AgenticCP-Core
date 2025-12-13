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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditDatabaseListener 단위 테스트")
@SuppressWarnings("deprecation") // mask() 메서드는 deprecated이지만 테스트에서 검증을 위해 사용
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
    void handleAuditEvent_WithValidEvent_PersistsAuditLog() throws Exception {
        // Given
        AuditPublishEvent event = new AuditPublishEvent(this, baseAuditEventDto);
        String maskedJson = "{\"key\":\"***\"}";
        
        // toMaskedJson이 호출되도록 설정 (5번: requestData, responseData, metadata, oldValue, newValue)
        when(maskingService.toMaskedJson(any(), any(ObjectMapper.class))).thenReturn(maskedJson);

        // When
        auditDatabaseListener.handleAuditEvent(event);

        // Then
        // toMaskedJson이 5번 호출됨 (null이 아닌 필드들)
        verify(maskingService, times(5)).toMaskedJson(any(), any(ObjectMapper.class));
        // mask()는 호출되지 않음 (원본 객체 보호)
        verify(maskingService, never()).mask(any());

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getAction()).isEqualTo(baseAuditEventDto.action());
        assertThat(savedLog.getTenantId()).isEqualTo(baseAuditEventDto.tenantId());
        assertThat(savedLog.getUserId()).isEqualTo(baseAuditEventDto.userId());
    }

    @Test
    @DisplayName("마스킹 실패 시 원본 JSON으로 저장 (예외 처리)")
    void handleAuditEvent_WhenMaskingFails_ThrowsBusinessException() throws Exception {
        // Given
        AuditPublishEvent event = new AuditPublishEvent(this, baseAuditEventDto);
        String originalJson = "{\"key\":\"value\"}";
        
        // toMaskedJson에서 예외 발생하도록 설정
        doThrow(new RuntimeException("mask fail")).when(maskingService)
                .toMaskedJson(any(), any(ObjectMapper.class));
        // 원본 JSON 변환은 성공하도록 설정
        when(objectMapper.writeValueAsString(any())).thenReturn(originalJson);

        // When
        // toMaskedJson 실패 시 toJson()에서 원본 JSON으로 변환을 시도하고,
        // 예외를 잡아 처리하므로 BusinessException이 발생하지 않고 정상적으로 저장됨
        auditDatabaseListener.handleAuditEvent(event);

        // Then
        // toMaskedJson이 호출되었지만 실패
        verify(maskingService, atLeastOnce()).toMaskedJson(any(), any(ObjectMapper.class));
        // 원본 JSON 변환이 호출됨 (마스킹 실패 시 fallback)
        verify(objectMapper, atLeastOnce()).writeValueAsString(any());
        // 저장은 정상적으로 수행됨
        verify(auditLogRepository).save(any(AuditLog.class));
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

