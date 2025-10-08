package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.monitoring.dto.QuotaRequestDto;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import com.agenticcp.core.domain.monitoring.service.TenantCollectorConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TenantCollectorConfigController HTTP 상태 코드 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantCollectorConfigController HTTP 상태 코드 테스트")
class TenantCollectorConfigControllerHttpStatusTest {

    @Mock
    private TenantCollectorConfigService configService;

    @InjectMocks
    private TenantCollectorConfigController controller;

    private TenantCollectorConfigDto testConfigDto;
    private String testTenantId;

    @BeforeEach
    void setUp() {
        testTenantId = "test-tenant";
        testConfigDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(CollectorType.SYSTEM)
                .isEnabled(true)
                .collectionInterval(60000L)
                .retryCount(3)
                .timeout(30000L)
                .priority(1)
                .dailyMetricLimit(10000L)
                .storageQuotaMb(1000L)
                .build();
    }

    @Test
    @DisplayName("수집기 생성 - 201 Created 상태 코드 반환")
    void createConfig_ValidData_ShouldReturnCreated() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.createConfig(any(TenantCollectorConfigDto.class))).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = 
                    controller.createConfig(testConfigDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
        }
    }

    @Test
    @DisplayName("수집기 삭제 - 204 No Content 상태 코드 반환")
    void deleteConfig_ValidId_ShouldReturnNoContent() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            doNothing().when(configService).deleteConfig(configId);

            // When
            ResponseEntity<Void> response = controller.deleteConfig(configId);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(response.getBody()).isNull();
            verify(configService).deleteConfig(configId);
        }
    }

    @Test
    @DisplayName("수집기 설정 수정 - 200 OK 상태 코드 반환")
    void updateConfig_ValidData_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.updateConfig(any(Long.class), any(TenantCollectorConfigDto.class)))
                    .thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = 
                    controller.updateConfig(configId, testConfigDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
        }
    }

    @Test
    @DisplayName("활성화된 수집기 설정 조회 - 200 OK 상태 코드 반환")
    void getEnabledConfigs_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            List<TenantCollectorConfigDto> configs = Arrays.asList(testConfigDto);
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getEnabledConfigsByTenant(anyString())).thenReturn(configs);

            // When
            ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> response = 
                    controller.getEnabledConfigs();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(configs);
        }
    }

    @Test
    @DisplayName("모든 수집기 설정 조회 - 200 OK 상태 코드 반환")
    void getAllConfigs_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            List<TenantCollectorConfigDto> configs = Arrays.asList(testConfigDto);
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getAllConfigsByTenant(anyString())).thenReturn(configs);

            // When
            ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> response = 
                    controller.getAllConfigs();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(configs);
        }
    }

    @Test
    @DisplayName("특정 수집기 설정 조회 - 200 OK 상태 코드 반환")
    void getConfigByType_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            CollectorType collectorType = CollectorType.SYSTEM;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getConfigByTenantAndType(anyString(), any(CollectorType.class)))
                    .thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = 
                    controller.getConfigByType(collectorType);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
        }
    }

    @Test
    @DisplayName("수집기 활성화/비활성화 - 200 OK 상태 코드 반환")
    void toggleConfig_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            boolean enabled = true;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.toggleConfig(any(Long.class), any(Boolean.class)))
                    .thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = 
                    controller.toggleConfig(configId, enabled);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
        }
    }

    @Test
    @DisplayName("활성화된 수집기 타입 조회 - 200 OK 상태 코드 반환")
    void getEnabledCollectorTypes_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            List<CollectorType> types = Arrays.asList(CollectorType.SYSTEM, CollectorType.APPLICATION);
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getEnabledCollectorTypesByTenant(anyString())).thenReturn(types);

            // When
            ResponseEntity<ApiResponse<List<CollectorType>>> response = 
                    controller.getEnabledCollectorTypes();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(types);
        }
    }

    @Test
    @DisplayName("활성화된 수집기 수 조회 - 200 OK 상태 코드 반환")
    void getEnabledCollectorCount_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long count = 3L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.countEnabledByTenant(anyString())).thenReturn(count);

            // When
            ResponseEntity<ApiResponse<Long>> response = 
                    controller.getEnabledCollectorCount();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(count);
        }
    }

    @Test
    @DisplayName("할당량 설정 - 200 OK 상태 코드 반환")
    void setQuota_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            doNothing().when(configService).setQuotaForTenant(anyString(), any(Long.class), any(Long.class), any());

            // 유효한 QuotaRequestDto 생성
            QuotaRequestDto quotaRequest = QuotaRequestDto.builder()
                    .dailyMetricLimit(10000L)
                    .storageQuotaMb(1000L)
                    .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                    .build();

            // When
            ResponseEntity<ApiResponse<String>> response = controller.setQuota(quotaRequest);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo("할당량 설정이 완료되었습니다.");
            
            verify(configService).setQuotaForTenant(eq(testTenantId), eq(10000L), eq(1000L), eq(QuotaExceededAction.WARN_ONLY));
        }
    }

    @Test
    @DisplayName("할당량 조회 - 200 OK 상태 코드 반환")
    void getQuota_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getQuotaForTenant(anyString())).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = 
                    controller.getQuota();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
        }
    }

    @Test
    @DisplayName("할당량 초과 여부 확인 - 200 OK 상태 코드 반환")
    void isQuotaExceeded_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            boolean isExceeded = false;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.isQuotaExceeded(anyString())).thenReturn(isExceeded);

            // When
            ResponseEntity<ApiResponse<Boolean>> response = 
                    controller.isQuotaExceeded();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(isExceeded);
        }
    }

    @Test
    @DisplayName("할당량 초과 처리 - 200 OK 상태 코드 반환")
    void handleQuotaExceeded_ShouldReturnOk() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            doNothing().when(configService).handleQuotaExceeded(anyString());

            // When
            ResponseEntity<ApiResponse<String>> response = 
                    controller.handleQuotaExceeded();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo("할당량 초과 처리가 완료되었습니다.");
        }
    }

    @Test
    @DisplayName("RESTful API 상태 코드 규칙 준수 확인")
    void verifyRestfulStatusCodeCompliance() {
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
            // When & Then
            // POST (생성) - 201 Created
            when(configService.createConfig(any())).thenReturn(testConfigDto);
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> createResponse = 
                    controller.createConfig(testConfigDto);
            assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            
            // GET (조회) - 200 OK
            when(configService.getEnabledConfigsByTenant(anyString())).thenReturn(Arrays.asList(testConfigDto));
            ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> getResponse = 
                    controller.getEnabledConfigs();
            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            // PUT (수정) - 200 OK
            when(configService.updateConfig(any(Long.class), any())).thenReturn(testConfigDto);
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> updateResponse = 
                    controller.updateConfig(1L, testConfigDto);
            assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            
            // DELETE (삭제) - 204 No Content
            doNothing().when(configService).deleteConfig(any(Long.class));
            ResponseEntity<Void> deleteResponse = controller.deleteConfig(1L);
            assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }
}
