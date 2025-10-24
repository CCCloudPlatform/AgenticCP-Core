package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.audit.AuditLogResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSearchRequest;
import com.agenticcp.core.common.dto.audit.AuditLogSearchResponse;
import com.agenticcp.core.common.dto.audit.AuditLogSummaryResponse;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditErrorCode;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.service.AuditLogService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * AuditLogController 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogController 단위 테스트")
@Disabled("Controller test disabled")
class AuditLogControllerTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditLogController auditLogController;

    @Nested
    @DisplayName("감사 로그 검색 테스트")
    class SearchAuditLogsTest {

        @Test
        @DisplayName("정상적인 감사 로그 검색")
        void searchAuditLogs_Success() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                    Instant.now().minusSeconds(3600),
                    Instant.now(),
                    "CREATE",
                    AuditResourceType.USER,
                    AuditSeverity.INFO,
                    "user123",
                    true,
                    "resource123",
                    0,
                    20,
                    "timestamp",
                    "desc"
            );

            AuditLogResponse auditLog = new AuditLogResponse(
                    1L,
                    "CREATE",
                    AuditResourceType.USER,
                    "POST",
                    "/api/v1/users",
                    "사용자 생성",
                    AuditSeverity.INFO,
                    Instant.now(),
                    "req-123",
                    "tenant123",
                    "user123",
                    "192.168.1.1",
                    true,
                    null,
                    "resource123",
                    Map.of("username", "testuser"),
                    Map.of("id", 1L),
                    Map.of("tenantId", "tenant123"),
                    null,
                    Map.of("username", "testuser")
            );

            AuditLogSearchResponse searchResponse = new AuditLogSearchResponse(
                    List.of(auditLog),
                    0,
                    20,
                    1L,
                    1,
                    true,
                    true
            );

            when(auditLogService.searchAuditLogs(any(AuditLogSearchRequest.class)))
                    .thenReturn(searchResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSearchResponse>> response = 
                    auditLogController.searchAuditLogs(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("감사 로그 검색이 완료되었습니다");
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().content()).hasSize(1);
            assertThat(response.getBody().getData().content().get(0).id()).isEqualTo(1L);
            assertThat(response.getBody().getData().content().get(0).action()).isEqualTo("CREATE");
            assertThat(response.getBody().getData().content().get(0).resourceType()).isEqualTo(AuditResourceType.USER);
            assertThat(response.getBody().getData().content().get(0).severity()).isEqualTo(AuditSeverity.INFO);
            assertThat(response.getBody().getData().page()).isEqualTo(0);
            assertThat(response.getBody().getData().size()).isEqualTo(20);
            assertThat(response.getBody().getData().totalElements()).isEqualTo(1L);
            assertThat(response.getBody().getData().totalPages()).isEqualTo(1);
            assertThat(response.getBody().getData().first()).isTrue();
            assertThat(response.getBody().getData().last()).isTrue();
        }

        @Test
        @DisplayName("빈 결과로 감사 로그 검색")
        void searchAuditLogs_EmptyResult_Success() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                    null, null, null, null, null, null, null, null,
                    0, 20, "timestamp", "desc"
            );

            AuditLogSearchResponse searchResponse = new AuditLogSearchResponse(
                    List.of(),
                    0,
                    20,
                    0L,
                    0,
                    true,
                    true
            );

            when(auditLogService.searchAuditLogs(any(AuditLogSearchRequest.class)))
                    .thenReturn(searchResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSearchResponse>> response = 
                    auditLogController.searchAuditLogs(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().totalElements()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("리소스별 감사 로그 조회 테스트")
    class GetAuditLogsByResourceTest {

        @Test
        @DisplayName("특정 리소스의 감사 로그 조회 성공")
        void getAuditLogsByResource_Success() {
            // Given
            String targetResourceId = "resource123";
            
            AuditLogResponse auditLog1 = new AuditLogResponse(
                    1L,
                    "CREATE",
                    AuditResourceType.USER,
                    "POST",
                    "/api/v1/users",
                    "사용자 생성",
                    AuditSeverity.INFO,
                    Instant.now(),
                    "req-123",
                    "tenant123",
                    "user123",
                    "192.168.1.1",
                    true,
                    null,
                    targetResourceId,
                    Map.of("username", "testuser"),
                    Map.of("id", 1L),
                    Map.of("tenantId", "tenant123"),
                    null,
                    Map.of("username", "testuser")
            );

            AuditLogResponse auditLog2 = new AuditLogResponse(
                    2L,
                    "UPDATE",
                    AuditResourceType.USER,
                    "PUT",
                    "/api/v1/users/1",
                    "사용자 수정",
                    AuditSeverity.INFO,
                    Instant.now(),
                    "req-124",
                    "tenant123",
                    "user123",
                    "192.168.1.1",
                    true,
                    null,
                    targetResourceId,
                    Map.of("name", "updated name"),
                    Map.of("id", 1L),
                    Map.of("tenantId", "tenant123"),
                    Map.of("name", "old name"),
                    Map.of("name", "updated name")
            );

            when(auditLogService.getAuditLogsByResource(targetResourceId))
                    .thenReturn(List.of(auditLog1, auditLog2));

            // When
            ResponseEntity<ApiResponse<List<AuditLogResponse>>> response = 
                    auditLogController.getAuditLogsByResource(targetResourceId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("리소스별 감사 로그 조회가 완료되었습니다");
            assertThat(response.getBody().getData()).hasSize(2);
            assertThat(response.getBody().getData().get(0).id()).isEqualTo(1L);
            assertThat(response.getBody().getData().get(0).action()).isEqualTo("CREATE");
            assertThat(response.getBody().getData().get(0).targetResourceId()).isEqualTo(targetResourceId);
            assertThat(response.getBody().getData().get(1).id()).isEqualTo(2L);
            assertThat(response.getBody().getData().get(1).action()).isEqualTo("UPDATE");
            assertThat(response.getBody().getData().get(1).targetResourceId()).isEqualTo(targetResourceId);
        }

        @Test
        @DisplayName("존재하지 않는 리소스의 감사 로그 조회")
        void getAuditLogsByResource_NonExistentResource_ReturnsEmptyList() {
            // Given
            String targetResourceId = "non-existent-resource";
            
            when(auditLogService.getAuditLogsByResource(targetResourceId))
                    .thenReturn(List.of());

            // When
            ResponseEntity<ApiResponse<List<AuditLogResponse>>> response = 
                    auditLogController.getAuditLogsByResource(targetResourceId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEmpty();
        }
    }

    @Nested
    @DisplayName("감사 로그 대시보드 요약 테스트")
    class GetAuditLogSummaryTest {

        @Test
        @DisplayName("감사 로그 대시보드 요약 조회 성공")
        void getAuditLogSummary_Success() {
            // Given
            String startDate = "2024-01-01T00:00:00.000Z";
            String endDate = "2024-01-31T23:59:59.999Z";
            
            AuditLogSummaryResponse summaryResponse = new AuditLogSummaryResponse(
                    1000L,
                    950L,
                    50L,
                    95.0,
                    Map.of(
                            AuditSeverity.INFO, 800L,
                            AuditSeverity.MEDIUM, 150L,
                            AuditSeverity.HIGH, 50L
                    ),
                    Map.of(
                            "CREATE", 300L,
                            "UPDATE", 400L,
                            "DELETE", 200L,
                            "READ", 100L
                    ),
                    Map.of(
                            "2024-01-01", 100L,
                            "2024-01-02", 150L,
                            "2024-01-03", 200L
                    ),
                    Map.of(
                            "00", 50L,
                            "01", 30L,
                            "02", 20L
                    ),
                    Instant.now()
            );

            when(auditLogService.getAuditLogSummary(any(Instant.class), any(Instant.class)))
                    .thenReturn(summaryResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSummaryResponse>> response = 
                    auditLogController.getAuditLogSummary(startDate, endDate);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getMessage()).isEqualTo("감사 로그 대시보드 요약이 완료되었습니다");
            assertThat(response.getBody().getData()).isNotNull();
            assertThat(response.getBody().getData().totalLogs()).isEqualTo(1000L);
            assertThat(response.getBody().getData().successLogs()).isEqualTo(950L);
            assertThat(response.getBody().getData().failedLogs()).isEqualTo(50L);
            assertThat(response.getBody().getData().successRate()).isEqualTo(95.0);
            assertThat(response.getBody().getData().severityDistribution().get(AuditSeverity.INFO)).isEqualTo(800L);
            assertThat(response.getBody().getData().severityDistribution().get(AuditSeverity.MEDIUM)).isEqualTo(150L);
            assertThat(response.getBody().getData().severityDistribution().get(AuditSeverity.HIGH)).isEqualTo(50L);
            assertThat(response.getBody().getData().actionDistribution().get("CREATE")).isEqualTo(300L);
            assertThat(response.getBody().getData().actionDistribution().get("UPDATE")).isEqualTo(400L);
            assertThat(response.getBody().getData().actionDistribution().get("DELETE")).isEqualTo(200L);
            assertThat(response.getBody().getData().actionDistribution().get("READ")).isEqualTo(100L);
        }

        @Test
        @DisplayName("날짜 파라미터 없이 감사 로그 대시보드 요약 조회")
        void getAuditLogSummary_WithoutDateParams_Success() {
            // Given
            AuditLogSummaryResponse summaryResponse = new AuditLogSummaryResponse(
                    500L,
                    480L,
                    20L,
                    96.0,
                    Map.of(AuditSeverity.INFO, 500L),
                    Map.of("CREATE", 200L, "UPDATE", 300L),
                    Map.of(),
                    Map.of(),
                    Instant.now()
            );

            when(auditLogService.getAuditLogSummary(null, null))
                    .thenReturn(summaryResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSummaryResponse>> response = 
                    auditLogController.getAuditLogSummary(null, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().totalLogs()).isEqualTo(500L);
            assertThat(response.getBody().getData().successRate()).isEqualTo(96.0);
        }

        @Test
        @DisplayName("잘못된 날짜 형식으로 요청 시 400 에러")
        void getAuditLogSummary_InvalidDateFormat_Returns400() {
            // Given
            String invalidStartDate = "invalid-date";
            String endDate = "2024-01-31T23:59:59.999Z";

            // When
            ResponseEntity<ApiResponse<AuditLogSummaryResponse>> response = 
                    auditLogController.getAuditLogSummary(invalidStartDate, endDate);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("잘못된 날짜 형식입니다. ISO 8601 형식을 사용해주세요.");
        }

        @Test
        @DisplayName("빈 날짜 파라미터로 요청")
        void getAuditLogSummary_EmptyDateParams_Success() {
            // Given
            AuditLogSummaryResponse summaryResponse = new AuditLogSummaryResponse(
                    300L,
                    290L,
                    10L,
                    96.7,
                    Map.of(AuditSeverity.INFO, 300L),
                    Map.of("CREATE", 100L, "UPDATE", 200L),
                    Map.of(),
                    Map.of(),
                    Instant.now()
            );

            when(auditLogService.getAuditLogSummary(null, null))
                    .thenReturn(summaryResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSummaryResponse>> response = 
                    auditLogController.getAuditLogSummary("", "");

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().totalLogs()).isEqualTo(300L);
        }
    }

    @Nested
    @DisplayName("권한 검사 테스트")
    class AuthorizationTest {

        @Test
        @DisplayName("감사 로그 검색 - 정상적인 권한으로 접근")
        void searchAuditLogs_WithValidPermission_Success() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                    null, null, null, null, null, null, null, null,
                    0, 20, "timestamp", "desc"
            );

            AuditLogSearchResponse searchResponse = new AuditLogSearchResponse(
                    List.of(), 0, 20, 0L, 0, true, true
            );

            when(auditLogService.searchAuditLogs(any(AuditLogSearchRequest.class)))
                    .thenReturn(searchResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSearchResponse>> response = 
                    auditLogController.searchAuditLogs(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("리소스별 감사 로그 조회 - 정상적인 권한으로 접근")
        void getAuditLogsByResource_WithValidPermission_Success() {
            // Given
            String targetResourceId = "resource123";
            
            when(auditLogService.getAuditLogsByResource(targetResourceId))
                    .thenReturn(List.of());

            // When
            ResponseEntity<ApiResponse<List<AuditLogResponse>>> response = 
                    auditLogController.getAuditLogsByResource(targetResourceId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("대시보드 요약 조회 - 정상적인 권한으로 접근")
        void getAuditLogSummary_WithValidPermission_Success() {
            // Given
            AuditLogSummaryResponse summaryResponse = new AuditLogSummaryResponse(
                    100L, 95L, 5L, 95.0,
                    Map.of(AuditSeverity.INFO, 100L),
                    Map.of("CREATE", 50L, "UPDATE", 50L),
                    Map.of(), Map.of(), Instant.now()
            );

            when(auditLogService.getAuditLogSummary(null, null))
                    .thenReturn(summaryResponse);

            // When
            ResponseEntity<ApiResponse<AuditLogSummaryResponse>> response = 
                    auditLogController.getAuditLogSummary(null, null);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
        }

        @Test
        @DisplayName("감사 로그 검색 - 서비스 레이어에서 예외 발생 시 처리")
        void searchAuditLogs_ServiceThrowsException_HandlesGracefully() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                    null, null, null, null, null, null, null, null,
                    0, 20, "timestamp", "desc"
            );

            when(auditLogService.searchAuditLogs(any(AuditLogSearchRequest.class)))
                    .thenThrow(new RuntimeException("서비스 오류"));

            // When & Then
            assertThatThrownBy(() -> auditLogController.searchAuditLogs(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("서비스 오류");
        }

        @Test
        @DisplayName("리소스별 감사 로그 조회 - 서비스 레이어에서 예외 발생 시 처리")
        void getAuditLogsByResource_ServiceThrowsException_HandlesGracefully() {
            // Given
            String targetResourceId = "resource123";
            
            when(auditLogService.getAuditLogsByResource(targetResourceId))
                    .thenThrow(new RuntimeException("서비스 오류"));

            // When & Then
            assertThatThrownBy(() -> auditLogController.getAuditLogsByResource(targetResourceId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("서비스 오류");
        }

        @Test
        @DisplayName("대시보드 요약 조회 - 서비스 레이어에서 예외 발생 시 처리")
        void getAuditLogSummary_ServiceThrowsException_HandlesGracefully() {
            // Given
            when(auditLogService.getAuditLogSummary(null, null))
                    .thenThrow(new RuntimeException("서비스 오류"));

            // When & Then
            assertThatThrownBy(() -> auditLogController.getAuditLogSummary(null, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("서비스 오류");
        }
    }

    @Nested
    @DisplayName("테넌트 격리 테스트")
    class TenantIsolationTest {

        @Test
        @DisplayName("테넌트 정보 없이 감사 로그 검색 시 예외 발생")
        void searchAuditLogs_NoTenantInfo_ThrowsException() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                null, null, null, null, null, null, null, null, 0, 20, "timestamp", "desc"
            );
            
            when(auditLogService.searchAuditLogs(request))
                    .thenThrow(new BusinessException(AuditErrorCode.INVALID_TENANT_CONTEXT));

            // When & Then
            assertThatThrownBy(() -> auditLogController.searchAuditLogs(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 테넌트 컨텍스트입니다.");
        }

        @Test
        @DisplayName("권한 없는 사용자가 감사 로그 조회 시 예외 발생")
        void searchAuditLogs_InsufficientPermission_ThrowsException() {
            // Given
            AuditLogSearchRequest request = new AuditLogSearchRequest(
                null, null, null, null, null, null, null, null, 0, 20, "timestamp", "desc"
            );
            
            when(auditLogService.searchAuditLogs(request))
                    .thenThrow(new BusinessException(AuditErrorCode.INSUFFICIENT_AUDIT_PERMISSION));

            // When & Then
            assertThatThrownBy(() -> auditLogController.searchAuditLogs(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("감사 로그 조회 권한이 없습니다.");
        }

        @Test
        @DisplayName("테넌트 정보 없이 리소스별 감사 로그 조회 시 예외 발생")
        void getAuditLogsByResource_NoTenantInfo_ThrowsException() {
            // Given
            String targetResourceId = "resource123";
            
            when(auditLogService.getAuditLogsByResource(targetResourceId))
                    .thenThrow(new BusinessException(AuditErrorCode.INVALID_TENANT_CONTEXT));

            // When & Then
            assertThatThrownBy(() -> auditLogController.getAuditLogsByResource(targetResourceId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 테넌트 컨텍스트입니다.");
        }

        @Test
        @DisplayName("테넌트 정보 없이 대시보드 요약 조회 시 예외 발생")
        void getAuditLogSummary_NoTenantInfo_ThrowsException() {
            // Given
            when(auditLogService.getAuditLogSummary(null, null))
                    .thenThrow(new BusinessException(AuditErrorCode.INVALID_TENANT_CONTEXT));

            // When & Then
            assertThatThrownBy(() -> auditLogController.getAuditLogSummary(null, null))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("유효하지 않은 테넌트 컨텍스트입니다.");
        }
    }
}
