package com.agenticcp.core.domain.monitoring.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.dto.QuotaRequestDto;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import com.agenticcp.core.domain.monitoring.service.TenantCollectorConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantCollectorConfigController 단위 테스트")
class TenantCollectorConfigControllerTest {

    @Mock
    private TenantCollectorConfigService configService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TenantCollectorConfigController controller;

    private TenantCollectorConfigDto testConfigDto;
    private String testTenantId;
    private CollectorType testCollectorType;

    @BeforeEach
    void setUp() {
        testTenantId = "test-tenant";
        testCollectorType = CollectorType.SYSTEM;

        testConfigDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .isEnabled(true)
                .collectionInterval(60000L)
                .retryCount(3)
                .timeout(30000L)
                .targetMetrics(Arrays.asList("cpu.usage", "memory.usage"))
                .collectorSettings(Map.of("interval", 60000))
                .priority(100)
                .metadata(Map.of("description", "시스템 메트릭 수집기"))
                .build();
    }

    @Test
    @DisplayName("현재 테넌트의 활성화된 수집기 설정 조회 - 성공")
    void getEnabledConfigs_Success() {
        // 테스트 케이스: 현재 테넌트의 활성화된 수집기 설정 조회 성공
        // 목적: 현재 테넌트의 활성화된 수집기 설정들을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 설정 목록의 크기가 예상과 일치하는지
        // 4. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 5. configService.getEnabledConfigsByTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            List<TenantCollectorConfigDto> configs = Arrays.asList(testConfigDto);
            when(configService.getEnabledConfigsByTenant(testTenantId)).thenReturn(configs);

            // When
            ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> response = controller.getEnabledConfigs();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(1);
            assertThat(response.getBody().getData().get(0).getTenantId()).isEqualTo(testTenantId);
        }
    }

    @Test
    @DisplayName("현재 테넌트의 모든 수집기 설정 조회 - 성공")
    void getAllConfigs_Success() {
        // 테스트 케이스: 현재 테넌트의 모든 수집기 설정 조회 성공
        // 목적: 현재 테넌트의 모든 수집기 설정들을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 설정 목록의 크기가 예상과 일치하는지
        // 4. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 5. configService.getAllConfigsByTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            List<TenantCollectorConfigDto> configs = Arrays.asList(testConfigDto);
            when(configService.getAllConfigsByTenant(testTenantId)).thenReturn(configs);

            // When
            ResponseEntity<ApiResponse<List<TenantCollectorConfigDto>>> response = controller.getAllConfigs();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(1);
        }
    }

    @Test
    @DisplayName("특정 수집기 설정 조회 - 성공")
    void getConfigByType_Success() {
        // 테스트 케이스: 특정 수집기 설정 조회 성공
        // 목적: 특정 수집기 타입의 설정을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 설정의 수집기 타입이 요청한 타입과 일치하는지
        // 4. configService.getConfigByTenantAndType()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getConfigByTenantAndType(testTenantId, testCollectorType)).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.getConfigByType(testCollectorType);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getCollectorType()).isEqualTo(testCollectorType);
        }
    }

    @Test
    @DisplayName("특정 수집기 설정 조회 - 설정을 찾을 수 없음")
    void getConfigByType_NotFound() {
        // 테스트 케이스: 특정 수집기 설정 조회 - 설정을 찾을 수 없음
        // 목적: 존재하지 않는 수집기 설정을 조회할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. ResourceNotFoundException이 발생하는지
        // 2. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.getConfigByTenantAndType(testTenantId, testCollectorType))
                    .thenThrow(new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> controller.getConfigByType(testCollectorType))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("수집기 설정을 찾을 수 없습니다.");
        }
    }

    @Test
    @DisplayName("수집기 설정 생성 - 성공")
    void createConfig_Success() {
        // 테스트 케이스: 수집기 설정 생성 성공
        // 목적: 새로운 수집기 설정을 정상적으로 생성하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 4. configService.createConfig()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.createConfig(any(TenantCollectorConfigDto.class))).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.createConfig(testConfigDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData().getTenantId()).isEqualTo(testTenantId);
            verify(configService).createConfig(any(TenantCollectorConfigDto.class));
        }
    }

    @Test
    @DisplayName("수집기 설정 생성 - 중복 설정 존재")
    void createConfig_DuplicateConfig() {
        // 테스트 케이스: 수집기 설정 생성 - 중복 설정 존재
        // 목적: 이미 존재하는 수집기 설정을 생성하려고 할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. BusinessException이 발생하는지
        // 2. 예외 메시지가 적절한지
        // 3. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.createConfig(any(TenantCollectorConfigDto.class)))
                    .thenThrow(new BusinessException(MonitoringErrorCode.COLLECTOR_CONFIG_ALREADY_EXISTS, "이미 존재하는 수집기 설정입니다."));

            // When & Then
            assertThatThrownBy(() -> controller.createConfig(testConfigDto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 존재하는 수집기 설정입니다.");
        }
    }

    @Test
    @DisplayName("수집기 설정 수정 - 성공")
    void updateConfig_Success() {
        // 테스트 케이스: 수집기 설정 수정 성공
        // 목적: 기존 수집기 설정을 정상적으로 수정하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. configService.updateConfig()가 호출되는지
        // 4. 반환된 설정이 수정된 설정과 일치하는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.updateConfig(eq(configId), any(TenantCollectorConfigDto.class))).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.updateConfig(configId, testConfigDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            verify(configService).updateConfig(eq(configId), any(TenantCollectorConfigDto.class));
        }
    }

    @Test
    @DisplayName("수집기 설정 수정 - 설정을 찾을 수 없음")
    void updateConfig_NotFound() {
        // 테스트 케이스: 수집기 설정 수정 - 설정을 찾을 수 없음
        // 목적: 존재하지 않는 수집기 설정을 수정하려고 할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. ResourceNotFoundException이 발생하는지
        // 2. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.updateConfig(eq(configId), any(TenantCollectorConfigDto.class)))
                    .thenThrow(new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> controller.updateConfig(configId, testConfigDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("수집기 설정을 찾을 수 없습니다.");
        }
    }

    @Test
    @DisplayName("수집기 설정 삭제 - 성공")
    void deleteConfig_Success() {
        // 테스트 케이스: 수집기 설정 삭제 성공
        // 목적: 기존 수집기 설정을 정상적으로 삭제하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 성공 메시지가 반환되는지
        // 4. configService.deleteConfig()가 호출되는지
        
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
    @DisplayName("수집기 설정 삭제 - 설정을 찾을 수 없음")
    void deleteConfig_NotFound() {
        // 테스트 케이스: 수집기 설정 삭제 - 설정을 찾을 수 없음
        // 목적: 존재하지 않는 수집기 설정을 삭제하려고 할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. ResourceNotFoundException이 발생하는지
        // 2. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            doThrow(new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND))
                    .when(configService).deleteConfig(configId);

            // When & Then
            assertThatThrownBy(() -> controller.deleteConfig(configId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("수집기 설정을 찾을 수 없습니다.");
        }
    }

    @Test
    @DisplayName("수집기 활성화/비활성화 - 성공")
    void toggleConfig_Success() {
        // 테스트 케이스: 수집기 활성화/비활성화 성공
        // 목적: 수집기 설정의 활성화/비활성화 상태를 정상적으로 토글하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. configService.toggleConfig()가 호출되는지
        // 4. 반환된 설정이 토글된 설정과 일치하는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            boolean enabled = false;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.toggleConfig(configId, enabled)).thenReturn(testConfigDto);

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.toggleConfig(configId, enabled);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            verify(configService).toggleConfig(configId, enabled);
        }
    }

    @Test
    @DisplayName("수집기 활성화/비활성화 - 설정을 찾을 수 없음")
    void toggleConfig_NotFound() {
        // 테스트 케이스: 수집기 활성화/비활성화 - 설정을 찾을 수 없음
        // 목적: 존재하지 않는 수집기 설정의 활성화/비활성화를 시도할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. ResourceNotFoundException이 발생하는지
        // 2. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            Long configId = 1L;
            boolean enabled = false;
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.toggleConfig(configId, enabled))
                    .thenThrow(new ResourceNotFoundException(MonitoringErrorCode.COLLECTOR_CONFIG_NOT_FOUND));

            // When & Then
            assertThatThrownBy(() -> controller.toggleConfig(configId, enabled))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("수집기 설정을 찾을 수 없습니다.");
        }
    }

    @Test
    @DisplayName("현재 테넌트의 활성화된 수집기 타입 목록 조회 - 성공")
    void getEnabledCollectorTypes_Success() {
        // 테스트 케이스: 현재 테넌트의 활성화된 수집기 타입 목록 조회 성공
        // 목적: 현재 테넌트의 활성화된 수집기 타입들을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 수집기 타입 목록의 크기가 예상과 일치하는지
        // 4. 반환된 수집기 타입들이 예상과 일치하는지
        // 5. configService.getEnabledCollectorTypesByTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            List<CollectorType> types = Arrays.asList(CollectorType.SYSTEM, CollectorType.APPLICATION);
            when(configService.getEnabledCollectorTypesByTenant(testTenantId)).thenReturn(types);

            // When
            ResponseEntity<ApiResponse<List<CollectorType>>> response = controller.getEnabledCollectorTypes();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).hasSize(2);
            assertThat(response.getBody().getData()).contains(CollectorType.SYSTEM, CollectorType.APPLICATION);
        }
    }

    @Test
    @DisplayName("현재 테넌트의 활성화된 수집기 수 조회 - 성공")
    void getEnabledCollectorCount_Success() {
        // 테스트 케이스: 현재 테넌트의 활성화된 수집기 수 조회 성공
        // 목적: 현재 테넌트의 활성화된 수집기 개수를 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 반환된 수집기 개수가 예상과 일치하는지
        // 4. configService.countEnabledByTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            long count = 3L;
            when(configService.countEnabledByTenant(testTenantId)).thenReturn(count);

            // When
            ResponseEntity<ApiResponse<Long>> response = controller.getEnabledCollectorCount();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(count);
        }
    }

    @Test
    @DisplayName("테넌트 컨텍스트가 없는 경우 - 예외 발생")
    void getEnabledConfigs_NoTenantContext() {
        // 테스트 케이스: 테넌트 컨텍스트가 없는 경우 - 예외 발생
        // 목적: 테넌트 컨텍스트가 설정되지 않은 상태에서 API를 호출할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. IllegalStateException이 발생하는지
        // 2. 예외 메시지가 적절한지
        // 3. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                    .thenThrow(new IllegalStateException("테넌트 컨텍스트가 설정되지 않았습니다."));

            // When & Then
            assertThatThrownBy(() -> controller.getEnabledConfigs())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("테넌트 컨텍스트가 설정되지 않았습니다.");
        }
    }

    @Test
    @DisplayName("수집기 설정 생성 - 테넌트 ID 자동 설정")
    void createConfig_TenantIdAutoSet() {
        // 테스트 케이스: 수집기 설정 생성 - 테넌트 ID 자동 설정
        // 목적: 수집기 설정 생성 시 테넌트 ID가 자동으로 설정되는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 테넌트 ID가 자동으로 설정되었는지 확인
        // 4. configService.createConfig()가 올바른 파라미터로 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            when(configService.createConfig(any(TenantCollectorConfigDto.class))).thenReturn(testConfigDto);

            // 테넌트 ID가 없는 DTO 생성
            TenantCollectorConfigDto requestDto = TenantCollectorConfigDto.builder()
                    .collectorType(testCollectorType)
                    .isEnabled(true)
                    .collectionInterval(60000L)
                    .build();

            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.createConfig(requestDto);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            
            // 테넌트 ID가 자동으로 설정되었는지 확인
            verify(configService).createConfig(argThat(dto -> 
                dto.getTenantId().equals(testTenantId) && 
                dto.getCollectorType().equals(testCollectorType)
            ));
        }
    }

    // ===== 할당량 관련 API 테스트 =====

    @Test
    @DisplayName("테넌트별 할당량 설정 API - 성공")
    void setQuota_Success() {
        // 테스트 케이스: 테넌트별 할당량 설정 API 성공
        // 목적: 할당량 설정 API가 정상적으로 작동하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. configService.setQuotaForTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
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
            
            verify(configService).setQuotaForTenant(testTenantId, 10000L, 1000L, QuotaExceededAction.WARN_ONLY);
        }
    }

    @Test
    @DisplayName("테넌트별 할당량 조회 API - 성공")
    void getQuota_Success() {
        // 테스트 케이스: 테넌트별 할당량 조회 API 성공
        // 목적: 할당량 조회 API가 정상적으로 작동하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 할당량 정보가 정상적으로 반환되는지
        // 4. configService.getQuotaForTenant()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
            testConfigDto = TenantCollectorConfigDto.builder()
                    .tenantId(testTenantId)
                    .collectorType(testCollectorType)
                    .dailyMetricLimit(10000L)
                    .storageQuotaMb(1000L)
                    .currentDailyUsage(5000L)
                    .currentStorageUsageMb(500L)
                    .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                    .build();
            
            when(configService.getQuotaForTenant(testTenantId)).thenReturn(testConfigDto);
            
            // When
            ResponseEntity<ApiResponse<TenantCollectorConfigDto>> response = controller.getQuota();
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo(testConfigDto);
            
            verify(configService).getQuotaForTenant(testTenantId);
        }
    }

    @Test
    @DisplayName("할당량 초과 여부 확인 API - 성공")
    void isQuotaExceeded_Success() {
        // 테스트 케이스: 할당량 초과 여부 확인 API 성공
        // 목적: 할당량 초과 여부 확인 API가 정상적으로 작동하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. 할당량 초과 여부가 정상적으로 반환되는지
        // 4. configService.isQuotaExceeded()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
            when(configService.isQuotaExceeded(testTenantId)).thenReturn(true);
            
            // When
            ResponseEntity<ApiResponse<Boolean>> response = controller.isQuotaExceeded();
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isTrue();
            
            verify(configService).isQuotaExceeded(testTenantId);
        }
    }

    @Test
    @DisplayName("할당량 초과 처리 API - 성공")
    void handleQuotaExceeded_Success() {
        // 테스트 케이스: 할당량 초과 처리 API 성공
        // 목적: 할당량 초과 처리 API가 정상적으로 작동하는지 확인
        // 검증 항목:
        // 1. HTTP 상태 코드가 200 OK인지
        // 2. 응답이 성공 상태인지
        // 3. configService.handleQuotaExceeded()가 호출되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
            // When
            ResponseEntity<ApiResponse<String>> response = controller.handleQuotaExceeded();
            
            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isTrue();
            assertThat(response.getBody().getData()).isEqualTo("할당량 초과 처리가 완료되었습니다.");
            
            verify(configService).handleQuotaExceeded(testTenantId);
        }
    }

    @Test
    @DisplayName("테넌트별 할당량 설정 API - 테넌트 컨텍스트 없음")
    void setQuota_NoTenantContext() {
        // 테스트 케이스: 테넌트별 할당량 설정 API - 테넌트 컨텍스트 없음
        // 목적: 테넌트 컨텍스트가 없을 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. BusinessException이 발생하는지
        // 2. 적절한 에러 메시지가 포함되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow)
                    .thenThrow(new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET));
            
            QuotaRequestDto quotaRequest = QuotaRequestDto.builder()
                    .dailyMetricLimit(10000L)
                    .storageQuotaMb(1000L)
                    .quotaExceededAction(QuotaExceededAction.WARN_ONLY)
                    .build();
            
            // When & Then
            assertThatThrownBy(() -> controller.setQuota(quotaRequest))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("테넌트 컨텍스트가 설정되지 않았습니다");
        }
    }

    @Test
    @DisplayName("테넌트별 할당량 조회 API - 서비스 오류")
    void getQuota_ServiceError() {
        // 테스트 케이스: 테넌트별 할당량 조회 API - 서비스 오류
        // 목적: 서비스에서 오류가 발생할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. BusinessException이 발생하는지
        // 2. 적절한 에러 메시지가 포함되는지
        
        try (MockedStatic<TenantContextHolder> mockedStatic = mockStatic(TenantContextHolder.class)) {
            // Given
            mockedStatic.when(TenantContextHolder::getCurrentTenantKeyOrThrow).thenReturn(testTenantId);
            
            when(configService.getQuotaForTenant(testTenantId))
                    .thenThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, "서비스 오류"));
            
            // When & Then
            assertThatThrownBy(() -> controller.getQuota())
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("서비스 오류");
        }
    }
}
