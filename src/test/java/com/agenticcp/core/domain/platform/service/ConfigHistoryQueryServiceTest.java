package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * ConfigHistoryQueryService 단위 테스트
 * - 하이브리드 접근법: RDBMS 기반 설정 이력 조회
 * - AuditLog 테이블에서 설정 변경 이력 조회
 * - ENCRYPTED 타입 마스킹 처리
 * - JSON 파싱 및 필드 추출
 */
public class ConfigHistoryQueryServiceTest {

    private AuditLogRepository auditLogRepository;
    private ObjectMapper objectMapper;
    private ConfigHistoryQueryService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = Mockito.mock(AuditLogRepository.class);
        objectMapper = new ObjectMapper();
        service = new ConfigHistoryQueryService(auditLogRepository, objectMapper);
    }

    @Test
    void getHistory_ShouldReturnConfigHistoryFromDatabase() {
        // given: AuditLog 테이블에서 조회할 데이터
        AuditLog auditLog1 = AuditLog.builder()
                .action("UPDATE")
                .resourceType(AuditResourceType.PLATFORM_CONFIG)
                .timestamp(java.time.Instant.now())
                .metadata("{\"reason\":\"test reason\",\"valueType\":\"STRING\"}")
                .oldValue("old-value")
                .newValue("new-value")
                .targetResourceId("test.key")
                .success(true)
                .build();

        AuditLog auditLog2 = AuditLog.builder()
                .action("CREATE")
                .resourceType(AuditResourceType.PLATFORM_CONFIG)
                .timestamp(java.time.Instant.now().minusSeconds(60))
                .metadata("{\"reason\":\"initial setup\",\"valueType\":\"STRING\"}")
                .oldValue(null)
                .newValue("initial-value")
                .targetResourceId("test.key")
                .success(true)
                .build();

        Page<AuditLog> auditLogPage = new PageImpl<>(
                List.of(auditLog1, auditLog2),
                PageRequest.of(0, 20),
                2
        );

        when(auditLogRepository.findByResourceTypeAndTargetResourceId(
                eq(AuditResourceType.PLATFORM_CONFIG),
                eq("test.key"),
                any(Pageable.class)
        )).thenReturn(auditLogPage);

        // when: 설정 이력 조회
        Page<ConfigHistoryResponse> result = service.getHistory("test.key", 0, 20);

        // then: 결과 검증
        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // 첫 번째 레코드 (UPDATE) 검증
        ConfigHistoryResponse response1 = result.getContent().get(0);
        assertEquals("UPDATE", response1.action());
        assertNull(response1.actor()); // user가 null이므로 null
        assertEquals("test reason", response1.reason());
        assertEquals("STRING", response1.valueType());
        assertEquals("old-value", response1.prevValue());
        assertEquals("new-value", response1.newValue());
        assertNotNull(response1.at());

        // 두 번째 레코드 (CREATE) 검증
        ConfigHistoryResponse response2 = result.getContent().get(1);
        assertEquals("CREATE", response2.action());
        assertNull(response2.actor()); // user가 null이므로 null
        assertEquals("initial setup", response2.reason());
        assertEquals("STRING", response2.valueType());
        assertNull(response2.prevValue());
        assertEquals("initial-value", response2.newValue());
        assertNotNull(response2.at());
    }

    @Test
    void getHistory_ShouldMaskEncryptedValues() {
        // given: ENCRYPTED 타입 설정 변경 이력
        AuditLog auditLog = AuditLog.builder()
                .action("UPDATE")
                .resourceType(AuditResourceType.PLATFORM_CONFIG)
                .timestamp(java.time.Instant.now())
                .metadata("{\"reason\":\"secret rotation\",\"valueType\":\"ENCRYPTED\"}")
                .oldValue("old-secret")
                .newValue("new-secret")
                .targetResourceId("secure.key")
                .success(true)
                .build();

        Page<AuditLog> auditLogPage = new PageImpl<>(
                List.of(auditLog),
                PageRequest.of(0, 20),
                1
        );

        when(auditLogRepository.findByResourceTypeAndTargetResourceId(
                eq(AuditResourceType.PLATFORM_CONFIG),
                eq("secure.key"),
                any(Pageable.class)
        )).thenReturn(auditLogPage);

        // when: 설정 이력 조회
        Page<ConfigHistoryResponse> result = service.getHistory("secure.key", 0, 20);

        // then: ENCRYPTED 값이 마스킹되었는지 검증
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        ConfigHistoryResponse response = result.getContent().get(0);
        assertEquals("UPDATE", response.action());
        assertEquals("ENCRYPTED", response.valueType());
        assertEquals("Encrypted", response.prevValue()); // 마스킹됨
        assertEquals("Encrypted", response.newValue()); // 마스킹됨
    }

    @Test
    void getHistory_ShouldHandleInvalidJsonGracefully() {
        // given: 잘못된 JSON 형식의 details
        AuditLog auditLog = AuditLog.builder()
                .action("UPDATE")
                .resourceType(AuditResourceType.PLATFORM_CONFIG)
                .timestamp(java.time.Instant.now())
                .metadata("invalid-json") // 잘못된 JSON
                .oldValue(null)
                .newValue(null)
                .targetResourceId("test.key")
                .success(true)
                .build();

        Page<AuditLog> auditLogPage = new PageImpl<>(
                List.of(auditLog),
                PageRequest.of(0, 20),
                1
        );

        when(auditLogRepository.findByResourceTypeAndTargetResourceId(
                eq(AuditResourceType.PLATFORM_CONFIG),
                eq("test.key"),
                any(Pageable.class)
        )).thenReturn(auditLogPage);

        // when & then: 예외가 발생하지 않고 기본값으로 응답 생성
        assertDoesNotThrow(() -> {
            Page<ConfigHistoryResponse> result = service.getHistory("test.key", 0, 20);
            
            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            
            ConfigHistoryResponse response = result.getContent().get(0);
            assertEquals("UPDATE", response.action());
            assertNull(response.actor()); // user가 null이므로 null
            assertNull(response.reason());
            assertNull(response.valueType());
            assertNull(response.prevValue());
            assertNull(response.newValue());
            assertNotNull(response.at());
        });
    }

    @Test
    void getHistory_ShouldReturnEmptyPageWhenNoHistory() {
        // given: 빈 결과
        Page<AuditLog> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 20),
                0
        );

        when(auditLogRepository.findByResourceTypeAndTargetResourceId(
                eq(AuditResourceType.PLATFORM_CONFIG),
                eq("nonexistent.key"),
                any(Pageable.class)
        )).thenReturn(emptyPage);

        // when: 존재하지 않는 설정 키로 조회
        Page<ConfigHistoryResponse> result = service.getHistory("nonexistent.key", 0, 20);

        // then: 빈 페이지 반환
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());
    }
}
