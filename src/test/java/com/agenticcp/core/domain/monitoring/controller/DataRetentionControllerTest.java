package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.monitoring.entity.TenantDataRetentionPolicy;
import com.agenticcp.core.domain.monitoring.service.TenantDataRetentionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DataRetentionController 컨트롤러 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
<<<<<<<<< Temporary merge branch 1
 * @since 2025-10-20
=========
 * @since 2025-11-13
>>>>>>>>> Temporary merge branch 2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataRetentionController 테스트")
class DataRetentionControllerTest {

    @Mock
    private TenantDataRetentionService retentionService;

    @InjectMocks
    private DataRetentionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private String testTenantId;
    private TenantDataRetentionPolicy samplePolicy;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
        testTenantId = "tenant-001";
        
        samplePolicy = TenantDataRetentionPolicy.builder()
                .tenantId(testTenantId)
                .dataType("metrics")
                .retentionDays(30)
                .isEnabled(true)
                .deletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.DELETE)
                .priority(50)
                .build();
    }

    @Nested
    @DisplayName("기본 보관 정책 생성 테스트")
    class CreateDefaultPolicyTest {

        @Test
        @DisplayName("기본 보관 정책 생성 성공 - 201 Created")
        void createDefaultRetentionPolicy_WhenValidTenant_ReturnsCreatedResponse() {
            // Given - 유효한 테넌트 컨텍스트가 설정된 상황
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.createDefaultRetentionPolicy(testTenantId))
                        .thenReturn(samplePolicy);

                // When - 기본 보관 정책을 생성하는 API를 호출하는 경우
                ResponseEntity<ApiResponse<TenantDataRetentionPolicy>> response = 
                        controller.createDefaultRetentionPolicy();

                // Then - 201 Created 상태코드와 함께 보관 정책이 반환되어야 함
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().isSuccess()).isTrue();
                assertThat(response.getBody().getData()).isEqualTo(samplePolicy);
                assertThat(response.getBody().getMessage()).contains("기본 보관 정책이 설정되었습니다");

                verify(retentionService).createDefaultRetentionPolicy(testTenantId);
            }
        }

        @Test
        @DisplayName("기본 보관 정책 생성 - 테넌트 컨텍스트 없음")
        void createDefaultRetentionPolicy_WhenNoTenantContext_ThrowsException() {
            // Given
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                        .thenThrow(new IllegalStateException("테넌트 컨텍스트가 없습니다"));

                // When & Then
                assertThatThrownBy(() -> controller.createDefaultRetentionPolicy())
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessage("테넌트 컨텍스트가 없습니다");

                verify(retentionService, never()).createDefaultRetentionPolicy(anyString());
            }
        }
    }

    @Nested
    @DisplayName("보관 정책 조회 테스트")
    class GetPolicyTest {

        @Test
        @DisplayName("보관 정책 목록 조회 성공")
        void getRetentionPolicies_WhenValidRequest_ReturnsPolicyList() throws Exception {
            // Given - 테넌트에 보관 정책이 존재하고 API 호출을 위한 Mock 설정이 완료된 상황
            List<TenantDataRetentionPolicy> policies = Arrays.asList(samplePolicy);  // 테스트용 보관 정책 목록
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);  // 테넌트 컨텍스트 Mock
                
                when(retentionService.getRetentionPolicies(testTenantId))
                        .thenReturn(policies);  // 서비스에서 보관 정책 목록을 반환하도록 Mock

                // When & Then - 보관 정책 목록 조회 API를 호출하고 응답을 검증하는 경우
                mockMvc.perform(get("/api/monitoring/retention/policies")  // GET /policies 엔드포인트 호출
                                .header("X-Tenant-Id", testTenantId))  // 테넌트 ID 헤더 설정
                        .andExpect(status().isOk())  // HTTP 200 OK 상태코드 확인
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))  // JSON 응답 확인
                        .andExpect(jsonPath("$.success").value(true))  // 성공 응답 확인
                        .andExpect(jsonPath("$.data").isArray())  // 데이터가 배열 형태인지 확인
                        .andExpect(jsonPath("$.data[0].tenantId").value(testTenantId))  // 첫 번째 정책의 테넌트 ID 확인
                        .andExpect(jsonPath("$.data[0].dataType").value("metrics"));  // 첫 번째 정책의 데이터 타입 확인

                verify(retentionService).getRetentionPolicies(testTenantId);  // 서비스 메서드 호출 확인
            }
        }

        @Test
        @DisplayName("활성화된 보관 정책 목록 조회 성공")
        void getEnabledRetentionPolicies_WhenValidRequest_ReturnsEnabledPolicyList() throws Exception {
            // Given
            List<TenantDataRetentionPolicy> enabledPolicies = Arrays.asList(samplePolicy);
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.getEnabledRetentionPolicies(testTenantId))
                        .thenReturn(enabledPolicies);

                // When & Then
                mockMvc.perform(get("/api/monitoring/retention/policies/enabled")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").isArray())
                        .andExpect(jsonPath("$.data[0].isEnabled").value(true));

                verify(retentionService).getEnabledRetentionPolicies(testTenantId);
            }
        }

        @Test
        @DisplayName("특정 데이터 타입 보관 정책 조회 성공")
        void getRetentionPolicy_WhenValidDataType_ReturnsPolicy() throws Exception {
            // Given
            String dataType = "metrics";
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.getRetentionPolicy(testTenantId, dataType))
                        .thenReturn(samplePolicy);

                // When & Then
                mockMvc.perform(get("/api/monitoring/retention/policies/{dataType}", dataType)
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.tenantId").value(testTenantId))
                        .andExpect(jsonPath("$.data.dataType").value(dataType));

                verify(retentionService).getRetentionPolicy(testTenantId, dataType);
            }
        }

        @Test
        @DisplayName("보관 정책이 없을 때 조회")
        void getRetentionPolicy_WhenPolicyNotFound_ReturnsNull() throws Exception {
            // Given
            String dataType = "metrics";
            
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.getRetentionPolicy(testTenantId, dataType))
                        .thenReturn(null);

                // When & Then
                mockMvc.perform(get("/api/monitoring/retention/policies/{dataType}", dataType)
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").doesNotExist());

                verify(retentionService).getRetentionPolicy(testTenantId, dataType);
            }
        }
    }

    @Nested
    @DisplayName("보관 정책 업데이트 테스트")
    class UpdatePolicyTest {

        @Test
        @DisplayName("보관 정책 업데이트 성공")
        void updateRetentionPolicy_WhenValidRequest_ReturnsUpdatedPolicy() throws Exception {
            // Given
            String dataType = "metrics";
            DataRetentionController.RetentionPolicyUpdateRequest request = 
                    new DataRetentionController.RetentionPolicyUpdateRequest();
            request.setRetentionDays(60);
            request.setDeletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE);
            request.setDescription("업데이트된 정책");
            
            TenantDataRetentionPolicy updatedPolicy = TenantDataRetentionPolicy.builder()
                    .tenantId(testTenantId)
                    .dataType(dataType)
                    .retentionDays(60)
                    .deletionStrategy(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE)
                    .build();

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.updateRetentionPolicy(eq(testTenantId), eq(dataType), 
                        any(Integer.class), any(), anyString()))
                        .thenReturn(updatedPolicy);

                // When & Then
                mockMvc.perform(put("/api/monitoring/retention/policies/{dataType}", dataType)
                                .header("X-Tenant-Id", testTenantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.retentionDays").value(60))
                        .andExpect(jsonPath("$.message").value("보관 정책이 업데이트되었습니다."));

                verify(retentionService).updateRetentionPolicy(eq(testTenantId), eq(dataType), 
                        eq(60), eq(TenantDataRetentionPolicy.DeletionStrategy.ARCHIVE), eq("업데이트된 정책"));
            }
        }

        @Test
        @DisplayName("보관 정책 업데이트 - 잘못된 요청 데이터")
        void updateRetentionPolicy_WhenInvalidRequest_ReturnsBadRequest() throws Exception {
            // Given
            String dataType = "metrics";
            DataRetentionController.RetentionPolicyUpdateRequest request = 
                    new DataRetentionController.RetentionPolicyUpdateRequest();
            request.setRetentionDays(null); // 잘못된 데이터

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);

                // When & Then
                mockMvc.perform(put("/api/monitoring/retention/policies/{dataType}", dataType)
                                .header("X-Tenant-Id", testTenantId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest());

                verify(retentionService, never()).updateRetentionPolicy(any(), any(), any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("보관 정책 토글 테스트")
    class TogglePolicyTest {

        @Test
        @DisplayName("보관 정책 활성화 성공")
        void toggleRetentionPolicy_WhenEnabled_ReturnsSuccessMessage() throws Exception {
            // Given
            String dataType = "metrics";
            boolean enabled = true;

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                doNothing().when(retentionService).toggleRetentionPolicy(testTenantId, dataType, enabled);

                // When & Then
                mockMvc.perform(patch("/api/monitoring/retention/policies/{dataType}/toggle", dataType)
                                .header("X-Tenant-Id", testTenantId)
                                .param("enabled", "true"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").value("보관 정책이 활성화되었습니다."));

                verify(retentionService).toggleRetentionPolicy(testTenantId, dataType, enabled);
            }
        }

        @Test
        @DisplayName("보관 정책 비활성화 성공")
        void toggleRetentionPolicy_WhenDisabled_ReturnsSuccessMessage() throws Exception {
            // Given
            String dataType = "metrics";
            boolean enabled = false;

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                doNothing().when(retentionService).toggleRetentionPolicy(testTenantId, dataType, enabled);

                // When & Then
                mockMvc.perform(patch("/api/monitoring/retention/policies/{dataType}/toggle", dataType)
                                .header("X-Tenant-Id", testTenantId)
                                .param("enabled", "false"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").value("보관 정책이 비활성화되었습니다."));

                verify(retentionService).toggleRetentionPolicy(testTenantId, dataType, enabled);
            }
        }
    }

    @Nested
    @DisplayName("수동 데이터 정리 테스트")
    class ManualCleanupTest {

        @Test
        @DisplayName("수동 데이터 정리 성공")
        void manualCleanup_WhenValidRequest_ReturnsCleanedCount() throws Exception {
            // Given - 테넌트에 활성화된 보관 정책이 존재하고 수동 정리 요청을 위한 Mock 설정이 완료된 상황
            String dataType = "metrics";  // 메트릭 데이터 타입
            int cleanedCount = 1000;  // 정리될 예상 데이터 개수

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);  // 테넌트 컨텍스트 Mock
                
                when(retentionService.manualCleanupTenantData(testTenantId, dataType))
                        .thenReturn(cleanedCount);  // 서비스에서 1000개의 정리된 데이터 개수를 반환하도록 Mock

                // When & Then - 수동 데이터 정리 API를 호출하고 응답을 검증하는 경우
                mockMvc.perform(post("/api/monitoring/retention/cleanup/{dataType}", dataType)  // POST /cleanup/{dataType} 엔드포인트 호출
                                .header("X-Tenant-Id", testTenantId))  // 테넌트 ID 헤더 설정
                        .andExpect(status().isOk())  // HTTP 200 OK 상태코드 확인
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))  // JSON 응답 확인
                        .andExpect(jsonPath("$.success").value(true))  // 성공 응답 확인
                        .andExpect(jsonPath("$.data").value("1000개의 오래된 레코드가 정리되었습니다."));  // 정리 결과 메시지 확인 (ApiResponse.data 필드)

                verify(retentionService).manualCleanupTenantData(testTenantId, dataType);  // 서비스 메서드 호출 확인
            }
        }

        @Test
        @DisplayName("수동 데이터 정리 - 정리된 데이터 없음")
        void manualCleanup_WhenNoData_ReturnsZeroCount() throws Exception {
            // Given
            String dataType = "metrics";
            int cleanedCount = 0;

            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                when(retentionService.manualCleanupTenantData(testTenantId, dataType))
                        .thenReturn(cleanedCount);

                // When & Then
                mockMvc.perform(post("/api/monitoring/retention/cleanup/{dataType}", dataType)
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").value("0개의 오래된 레코드가 정리되었습니다."));

                verify(retentionService).manualCleanupTenantData(testTenantId, dataType);
            }
        }
    }

    @Nested
    @DisplayName("통계 조회 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("보관 정책 통계 조회 성공")
        void getRetentionStatistics_WhenValidRequest_ReturnsStatistics() throws Exception {
            // Given - 통계 조회를 위한 Mock 설정
            try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
                mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
                
                // 서비스에서 통계 객체를 반환하도록 Mock 설정
                when(retentionService.getRetentionPolicyStatistics())
                        .thenReturn(mock(TenantDataRetentionService.RetentionPolicyStatistics.class));

                // When & Then - 통계 조회 API 호출
                mockMvc.perform(get("/api/monitoring/retention/statistics")
                                .header("X-Tenant-Id", testTenantId))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true));

                verify(retentionService).getRetentionPolicyStatistics();
            }
        }
    }
}
