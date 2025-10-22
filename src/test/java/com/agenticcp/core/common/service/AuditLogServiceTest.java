package com.agenticcp.core.common.service;

import com.agenticcp.core.common.dto.audit.AuditLogResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSearchRequest;
import com.agenticcp.core.common.dto.audit.AuditLogSearchResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSummaryResponse;
import com.agenticcp.core.common.entity.AuditLog;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.repository.AuditLogRepository;
import com.agenticcp.core.common.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AuditLogService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @Mock
    private SecurityContext securityContext;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog testAuditLog;
    private AuditLogSearchRequest testSearchRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 AuditLog 생성
        testAuditLog = AuditLog.builder()
                .action("CREATE")
                .resourceType(AuditResourceType.USER)
                .httpMethod("POST")
                .requestPath("/api/v1/users")
                .operationSummary("사용자 생성")
                .severity(AuditSeverity.INFO)
                .timestamp(Instant.now())
                .requestId("req-123")
                .userId("user-123")
                .clientIp("192.168.1.1")
                .success(true)
                .error(null)
                .targetResourceId("user-456")
                .requestData("{\"username\":\"test\"}")
                .responseData("{\"id\":456}")
                .metadata("{\"tenantId\":\"tenant-123\"}")
                .oldValue(null)
                .newValue("{\"username\":\"test\"}")
                .tenantId("tenant-123")
                .build();

        // 테스트용 검색 요청 생성
        testSearchRequest = new AuditLogSearchRequest(
                Instant.now().minusSeconds(3600),
                Instant.now(),
                "CREATE",
                AuditResourceType.USER,
                AuditSeverity.INFO,
                "user-123",
                true,
                "user-456",
                0,
                20,
                "timestamp",
                "desc"
        );

        // SecurityContext 설정
        setupSecurityContext();
    }

    private void setupSecurityContext() {
        // 테넌트 정보가 포함된 JWT 인증 세부사항 생성
        JwtAuthenticationFilter.JwtAuthenticationDetails details = 
                new JwtAuthenticationFilter.JwtAuthenticationDetails("test@example.com", 1L, List.of());
        
        // GrantedAuthority 목 생성 (TENANT_ADMIN 역할)
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_TENANT_ADMIN");
        
        when(authentication.getName()).thenReturn("user-123");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        when(authentication.getDetails()).thenReturn(details);
        when(authentication.isAuthenticated()).thenReturn(true);
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("감사 로그 검색 - 성공")
    void searchAuditLogs_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByTenantIdAndFilters(
                eq("1"), // 실제 서비스에서 반환하는 테넌트 ID
                any(Instant.class),
                any(Instant.class),
                eq("CREATE"),
                eq(AuditResourceType.USER),
                eq(AuditSeverity.INFO),
                eq("user-123"),
                eq(true),
                eq("user-456"),
                any(Pageable.class)
        )).thenReturn(mockPage);

        // When
        AuditLogSearchResponse result = auditLogService.searchAuditLogs(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).action()).isEqualTo("CREATE");
        assertThat(result.content().get(0).resourceType()).isEqualTo(AuditResourceType.USER);
        assertThat(result.content().get(0).severity()).isEqualTo(AuditSeverity.INFO);
    }

    @Test
    @DisplayName("감사 로그 검색 - 권한 없음 예외")
    void searchAuditLogs_NoPermission_ThrowsException() {
        // Given - 권한이 없는 사용자로 설정
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_VIEWER");
        doReturn(List.of(authority)).when(authentication).getAuthorities();

        // When & Then
        assertThatThrownBy(() -> auditLogService.searchAuditLogs(testSearchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("감사 로그 조회 권한이 없습니다.");
    }

    @Test
    @DisplayName("감사 로그 검색 - 테넌트 정보 없음 예외 발생")
    void searchAuditLogs_NoTenantId_ThrowsException() {
        // Given - 테넌트 정보가 없는 경우
        when(authentication.getDetails()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> auditLogService.searchAuditLogs(testSearchRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("유효하지 않은 테넌트 컨텍스트");
    }

    @Test
    @DisplayName("특정 리소스의 감사 로그 조회 - 성공")
    void getAuditLogsByResource_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        String targetResourceId = "user-456";
        when(auditLogRepository.findByTargetResourceIdAndTenantId(targetResourceId, "1"))
                .thenReturn(List.of(testAuditLog));

        // When
        List<AuditLogResponse> result = auditLogService.getAuditLogsByResource(targetResourceId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).targetResourceId()).isEqualTo("user-456");
        assertThat(result.get(0).action()).isEqualTo("CREATE");
        
        verify(auditLogRepository).findByTargetResourceIdAndTenantId(targetResourceId, "1");
    }

    @Test
    @DisplayName("특정 리소스의 감사 로그 조회 - 권한 없음 예외")
    void getAuditLogsByResource_NoPermission_ThrowsException() {
        // Given - 권한이 없는 사용자로 설정
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_VIEWER");
        doReturn(List.of(authority)).when(authentication).getAuthorities();

        // When & Then
        assertThatThrownBy(() -> auditLogService.getAuditLogsByResource("user-456"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("감사 로그 조회 권한이 없습니다.");
    }

    @Test
    @DisplayName("감사 로그 대시보드 요약 - 성공")
    void getAuditLogSummary_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();
        
        AuditLog auditLog1 = AuditLog.builder()
                .action("CREATE")
                .resourceType(AuditResourceType.USER)
                .httpMethod("POST")
                .requestPath("/api/v1/users")
                .operationSummary("사용자 생성")
                .severity(AuditSeverity.INFO)
                .timestamp(Instant.now())
                .requestId("req-123")
                .userId("user-123")
                .clientIp("192.168.1.1")
                .success(true)
                .targetResourceId("user-456")
                .tenantId("tenant-123")
                .build();
        
        AuditLog auditLog2 = AuditLog.builder()
                .action("UPDATE")
                .resourceType(AuditResourceType.USER)
                .httpMethod("PUT")
                .requestPath("/api/v1/users")
                .operationSummary("사용자 수정")
                .severity(AuditSeverity.HIGH)
                .timestamp(Instant.now())
                .requestId("req-124")
                .userId("user-124")
                .clientIp("192.168.1.2")
                .success(false)
                .targetResourceId("user-457")
                .tenantId("tenant-123")
                .build();
        
        when(auditLogRepository.findByTenantIdAndTimestampBetween("1", startDate, endDate))
                .thenReturn(List.of(auditLog1, auditLog2));

        // When
        AuditLogSummaryResponse result = auditLogService.getAuditLogSummary(startDate, endDate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalLogs()).isEqualTo(2);
        assertThat(result.successLogs()).isEqualTo(1);
        assertThat(result.failedLogs()).isEqualTo(1);
        assertThat(result.successRate()).isEqualTo(50.0);
        assertThat(result.severityDistribution()).containsEntry(AuditSeverity.INFO, 1L);
        assertThat(result.severityDistribution()).containsEntry(AuditSeverity.HIGH, 1L);
    }

    @Test
    @DisplayName("감사 로그 대시보드 요약 - 기본 날짜 범위 사용")
    void getAuditLogSummary_DefaultDateRange_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        // Given - null 날짜로 호출
        when(auditLogRepository.findByTenantIdAndTimestampBetween(
                eq("1"),
                any(Instant.class), // 30일 전
                any(Instant.class)  // 현재 시간
        )).thenReturn(List.of(testAuditLog));

        // When
        AuditLogSummaryResponse result = auditLogService.getAuditLogSummary(null, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalLogs()).isEqualTo(1);
        verify(auditLogRepository).findByTenantIdAndTimestampBetween(
                eq("1"),
                any(Instant.class),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("감사 로그 대시보드 요약 - 빈 결과")
    void getAuditLogSummary_EmptyResult_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        Instant startDate = Instant.now().minusSeconds(3600);
        Instant endDate = Instant.now();
        
        when(auditLogRepository.findByTenantIdAndTimestampBetween("1", startDate, endDate))
                .thenReturn(List.of());

        // When
        AuditLogSummaryResponse result = auditLogService.getAuditLogSummary(startDate, endDate);

        // Then
        assertThat(result.totalLogs()).isEqualTo(0);
        assertThat(result.successLogs()).isEqualTo(0);
        assertThat(result.failedLogs()).isEqualTo(0);
        assertThat(result.successRate()).isEqualTo(0.0);
        assertThat(result.severityDistribution()).isEmpty();
        assertThat(result.actionDistribution()).isEmpty();
        assertThat(result.dailyLogCount()).isEmpty();
        assertThat(result.hourlyLogCount()).isEmpty();
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 null 반환")
    void parseJsonToMap_InvalidJson_ReturnsNull() throws Exception {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        String invalidJson = "{invalid json}";
        when(objectMapper.readValue(eq(invalidJson), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // When - parseJsonToMap은 private 메서드이므로 직접 테스트할 수 없음
        // 대신 convertToResponse에서 JSON 파싱 오류가 발생할 때의 동작을 테스트
        AuditLog auditLogWithInvalidJson = AuditLog.builder()
                .action("CREATE")
                .resourceType(AuditResourceType.USER)
                .httpMethod("POST")
                .requestPath("/api/v1/users")
                .operationSummary("사용자 생성")
                .severity(AuditSeverity.INFO)
                .timestamp(Instant.now())
                .requestId("req-123")
                .userId("user-123")
                .clientIp("192.168.1.1")
                .success(true)
                .targetResourceId("user-456")
                .requestData("{invalid json}")
                .responseData(null)
                .metadata(null)
                .oldValue(null)
                .newValue(null)
                .tenantId("tenant-123")
                .build();

        when(auditLogRepository.findByTenantIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(List.of(auditLogWithInvalidJson));

        // When
        AuditLogSummaryResponse result = auditLogService.getAuditLogSummary(Instant.now().minusSeconds(3600), Instant.now());

        // Then
        assertThat(result).isNotNull();
        // JSON 파싱 실패 시에도 서비스는 정상 동작해야 함
    }

    @Test
    @DisplayName("SUPER_ADMIN 권한으로 감사 로그 조회 성공")
    void searchAuditLogs_SuperAdminRole_Success() {
        // Given - SUPER_ADMIN 역할로 설정
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_SUPER_ADMIN");
        doReturn(List.of(authority)).when(authentication).getAuthorities();
        
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByTenantIdAndFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        AuditLogSearchResponse result = auditLogService.searchAuditLogs(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("AUDITOR 권한으로 감사 로그 조회 성공")
    void searchAuditLogs_AuditorRole_Success() {
        // Given - AUDITOR 역할로 설정
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_AUDITOR");
        doReturn(List.of(authority)).when(authentication).getAuthorities();

        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByTenantIdAndFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        AuditLogSearchResponse result = auditLogService.searchAuditLogs(testSearchRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("페이징 정보가 올바르게 설정되는지 확인")
    void searchAuditLogs_PagingInfo_Success() {
        // Given - TENANT_ADMIN 권한 설정
        setupSecurityContext();
        
        Page<AuditLog> mockPage = new PageImpl<>(
                List.of(testAuditLog),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp")),
                100L
        );
        
        when(auditLogRepository.findByTenantIdAndFilters(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        AuditLogSearchResponse result = auditLogService.searchAuditLogs(testSearchRequest);

        // Then
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(100L);
        assertThat(result.totalPages()).isEqualTo(5);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
    }
}
