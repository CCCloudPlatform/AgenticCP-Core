package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorConfig;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorMetadata;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import com.agenticcp.core.domain.monitoring.repository.TenantCollectorConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantCollectorConfigService 단위 테스트")
class TenantCollectorConfigServiceTest {

    @Mock
    private TenantCollectorConfigRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TenantCollectorConfigService service;

    private TenantCollectorConfig testConfig;
    private TenantCollectorConfigDto testConfigDto;
    private String testTenantId;
    private CollectorType testCollectorType;

    @BeforeEach
    void setUp() {
        testTenantId = "test-tenant";
        testCollectorType = CollectorType.SYSTEM;

        // 테스트용 엔티티 생성
        testConfig = TenantCollectorConfig.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .isEnabled(true)
                .collectionInterval(60000L)
                .retryCount(3)
                .timeout(30000L)
                .targetMetrics("[\"cpu.usage\", \"memory.usage\"]")
                .collectorSettings("{\"interval\": 60000}")
                .priority(100)
                .build();

        // 테스트용 DTO 생성
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
    @DisplayName("테넌트별 활성화된 수집기 설정 조회 - 성공")
    void getEnabledConfigsByTenant_Success() {
        // 테스트 케이스: 테넌트별 활성화된 수집기 설정 조회 성공
        // 목적: 특정 테넌트의 활성화된 수집기 설정들을 정상적으로 조회하는지 확인
        // 검증 항목: 
        // 1. 반환된 설정 목록의 크기가 예상과 일치하는지
        // 2. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 3. 반환된 설정의 수집기 타입이 예상과 일치하는지
        // 4. 반환된 설정이 활성화 상태인지
        
        // Given
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findEnabledByTenantId(testTenantId)).thenReturn(configs);
        // ObjectMapper 모킹 - 실제 JSON 파싱 대신 직접 값 반환
        try {
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenAnswer(invocation -> {
                        TypeReference<?> typeRef = invocation.getArgument(1);
                        if (typeRef.getType().toString().contains("List")) {
                            return Arrays.asList("cpu.usage", "memory.usage");
                        } else {
                            return Map.of("interval", 60000);
                        }
                    });
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        List<TenantCollectorConfigDto> result = service.getEnabledConfigsByTenant(testTenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(testTenantId);
        assertThat(result.get(0).getCollectorType()).isEqualTo(testCollectorType);
        assertThat(result.get(0).getIsEnabled()).isTrue();
    }

    @Test
    @DisplayName("테넌트별 모든 수집기 설정 조회 - 성공")
    void getAllConfigsByTenant_Success() {
        // 테스트 케이스: 테넌트별 모든 수집기 설정 조회 성공
        // 목적: 특정 테넌트의 모든 수집기 설정(활성화/비활성화 포함)을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. 반환된 설정 목록의 크기가 예상과 일치하는지
        // 2. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 3. 활성화/비활성화 상태와 관계없이 모든 설정이 조회되는지
        
        // Given
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        // ObjectMapper 모킹 - 실제 JSON 파싱 대신 직접 값 반환
        try {
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenAnswer(invocation -> {
                        TypeReference<?> typeRef = invocation.getArgument(1);
                        if (typeRef.getType().toString().contains("List")) {
                            return Arrays.asList("cpu.usage", "memory.usage");
                        } else {
                            return Map.of("interval", 60000);
                        }
                    });
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        List<TenantCollectorConfigDto> result = service.getAllConfigsByTenant(testTenantId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(testTenantId);
    }

    @Test
    @DisplayName("특정 수집기 설정 조회 - 성공")
    void getConfigByTenantAndType_Success() {
        // 테스트 케이스: 특정 수집기 설정 조회 성공
        // 목적: 특정 테넌트의 특정 수집기 타입에 대한 설정을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. 반환된 설정이 null이 아닌지
        // 2. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 3. 반환된 설정의 수집기 타입이 요청한 타입과 일치하는지
        
        // Given
        when(repository.findByTenantIdAndCollectorType(testTenantId, testCollectorType))
                .thenReturn(Optional.of(testConfig));
        // ObjectMapper 모킹 - 실제 JSON 파싱 대신 직접 값 반환
        try {
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenAnswer(invocation -> {
                        TypeReference<?> typeRef = invocation.getArgument(1);
                        if (typeRef.getType().toString().contains("List")) {
                            return Arrays.asList("cpu.usage", "memory.usage");
                        } else {
                            return Map.of("interval", 60000);
                        }
                    });
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        TenantCollectorConfigDto result = service.getConfigByTenantAndType(testTenantId, testCollectorType);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(testTenantId);
        assertThat(result.getCollectorType()).isEqualTo(testCollectorType);
    }

    @Test
    @DisplayName("특정 수집기 설정 조회 - 설정을 찾을 수 없음")
    void getConfigByTenantAndType_NotFound() {
        // 테스트 케이스: 특정 수집기 설정 조회 실패 (설정을 찾을 수 없음)
        // 목적: 존재하지 않는 수집기 설정을 조회할 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. ResourceNotFoundException이 발생하는지
        // 2. 예외 메시지가 적절한지
        // 3. 예외가 발생한 후 서비스가 정상적으로 종료되는지
        
        // Given
        when(repository.findByTenantIdAndCollectorType(testTenantId, testCollectorType))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getConfigByTenantAndType(testTenantId, testCollectorType))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("수집기 설정 생성 - 성공")
    void createConfig_Success() {
        // 테스트 케이스: 수집기 설정 생성 성공
        // 목적: 새로운 수집기 설정을 정상적으로 생성하는지 확인
        // 검증 항목:
        // 1. 반환된 설정이 null이 아닌지
        // 2. 반환된 설정의 테넌트 ID가 요청한 테넌트 ID와 일치하는지
        // 3. 반환된 설정의 수집기 타입이 요청한 타입과 일치하는지
        // 4. repository.save()가 호출되는지
        
        // Given
        when(repository.existsByTenantIdAndCollectorType(testTenantId, testCollectorType)).thenReturn(false);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("[\"cpu.usage\", \"memory.usage\"]");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"interval\": 60000}");
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        TenantCollectorConfigDto result = service.createConfig(testConfigDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(testTenantId);
        assertThat(result.getCollectorType()).isEqualTo(testCollectorType);
        verify(repository).save(any(TenantCollectorConfig.class));
    }

    @Test
    @DisplayName("수집기 설정 생성 - 중복 설정 존재")
    void createConfig_DuplicateConfig() {
        // Given
        when(repository.existsByTenantIdAndCollectorType(testTenantId, testCollectorType)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createConfig(testConfigDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 수집기 설정입니다");
    }

    @Test
    @DisplayName("수집기 설정 생성 - 유효하지 않은 테넌트 ID")
    void createConfig_InvalidTenantId() {
        // Given
        TenantCollectorConfigDto invalidDto = TenantCollectorConfigDto.builder()
                .tenantId("")
                .collectorType(testCollectorType)
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createConfig(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("테넌트 ID가 유효하지 않습니다");
    }

    @Test
    @DisplayName("수집기 설정 생성 - 유효하지 않은 수집 주기")
    void createConfig_InvalidCollectionInterval() {
        // Given
        TenantCollectorConfigDto invalidDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .collectionInterval(-1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createConfig(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("수집 주기가 유효하지 않습니다");
    }

    @Test
    @DisplayName("수집기 설정 수정 - 성공")
    void updateConfig_Success() {
        // Given
        Long configId = 1L;
        when(repository.findById(configId)).thenReturn(Optional.of(testConfig));
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        try {
            when(objectMapper.writeValueAsString(any())).thenReturn("[\"cpu.usage\", \"memory.usage\"]");
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"interval\": 60000}");
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        TenantCollectorConfigDto result = service.updateConfig(configId, testConfigDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(testTenantId);
        verify(repository).save(any(TenantCollectorConfig.class));
    }

    @Test
    @DisplayName("수집기 설정 수정 - 설정을 찾을 수 없음")
    void updateConfig_NotFound() {
        // Given
        Long configId = 1L;
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.updateConfig(configId, testConfigDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("수집기 설정 삭제 - 성공")
    void deleteConfig_Success() {
        // Given
        Long configId = 1L;
        when(repository.existsById(configId)).thenReturn(true);

        // When
        service.deleteConfig(configId);

        // Then
        verify(repository).deleteById(configId);
    }

    @Test
    @DisplayName("수집기 설정 삭제 - 설정을 찾을 수 없음")
    void deleteConfig_NotFound() {
        // Given
        Long configId = 1L;
        when(repository.existsById(configId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.deleteConfig(configId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("수집기 활성화/비활성화 - 성공")
    void toggleConfig_Success() {
        // Given
        Long configId = 1L;
        boolean enabled = false;
        when(repository.findById(configId)).thenReturn(Optional.of(testConfig));
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        // ObjectMapper 모킹 - 실제 JSON 파싱 대신 직접 값 반환
        try {
            when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                    .thenAnswer(invocation -> {
                        TypeReference<?> typeRef = invocation.getArgument(1);
                        if (typeRef.getType().toString().contains("List")) {
                            return Arrays.asList("cpu.usage", "memory.usage");
                        } else {
                            return Map.of("interval", 60000);
                        }
                    });
        } catch (JsonProcessingException e) {
            // Mock setup - exception won't actually be thrown
        }

        // When
        TenantCollectorConfigDto result = service.toggleConfig(configId, enabled);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsEnabled()).isEqualTo(enabled);
        verify(repository).save(any(TenantCollectorConfig.class));
    }

    @Test
    @DisplayName("수집기 활성화/비활성화 - 설정을 찾을 수 없음")
    void toggleConfig_NotFound() {
        // Given
        Long configId = 1L;
        boolean enabled = false;
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.toggleConfig(configId, enabled))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("테넌트별 활성화된 수집기 타입 목록 조회 - 성공")
    void getEnabledCollectorTypesByTenant_Success() {
        // Given
        List<CollectorType> types = Arrays.asList(CollectorType.SYSTEM, CollectorType.APPLICATION);
        when(repository.findEnabledCollectorTypesByTenantId(testTenantId)).thenReturn(types);

        // When
        List<CollectorType> result = service.getEnabledCollectorTypesByTenant(testTenantId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains(CollectorType.SYSTEM, CollectorType.APPLICATION);
    }

    @Test
    @DisplayName("특정 수집기 타입을 사용하는 테넌트 목록 조회 - 성공")
    void getTenantIdsByCollectorType_Success() {
        // Given
        List<String> tenantIds = Arrays.asList("tenant1", "tenant2");
        when(repository.findTenantIdsByCollectorType(testCollectorType)).thenReturn(tenantIds);

        // When
        List<String> result = service.getTenantIdsByCollectorType(testCollectorType);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).contains("tenant1", "tenant2");
    }

    @Test
    @DisplayName("테넌트별 활성화된 수집기 수 조회 - 성공")
    void countEnabledByTenant_Success() {
        // Given
        long count = 3L;
        when(repository.countEnabledByTenantId(testTenantId)).thenReturn(count);

        // When
        long result = service.countEnabledByTenant(testTenantId);

        // Then
        assertThat(result).isEqualTo(count);
    }

    @Test
    @DisplayName("설정 검증 - 성공")
    void validateConfig_Success() {
        // Given
        TenantCollectorConfigDto validDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .collectionInterval(60000L)
                .retryCount(3)
                .timeout(30000L)
                .build();

        // Mock repository.save() to return a valid config
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        when(repository.existsByTenantIdAndCollectorType(testTenantId, testCollectorType)).thenReturn(false);

        // When & Then
        assertThatCode(() -> service.createConfig(validDto))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("설정 검증 - 유효하지 않은 재시도 횟수")
    void validateConfig_InvalidRetryCount() {
        // Given
        TenantCollectorConfigDto invalidDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .retryCount(-1)
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createConfig(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재시도 횟수가 유효하지 않습니다");
    }

    @Test
    @DisplayName("설정 검증 - 유효하지 않은 타임아웃")
    void validateConfig_InvalidTimeout() {
        // Given
        TenantCollectorConfigDto invalidDto = TenantCollectorConfigDto.builder()
                .tenantId(testTenantId)
                .collectorType(testCollectorType)
                .timeout(-1L)
                .build();

        // When & Then
        assertThatThrownBy(() -> service.createConfig(invalidDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("타임아웃이 유효하지 않습니다");
    }

    // ===== 할당량 관련 테스트 =====

    @Test
    @DisplayName("테넌트별 할당량 설정 - 성공")
    void setQuotaForTenant_Success() {
        // 테스트 케이스: 테넌트별 할당량 설정 성공
        // 목적: 테넌트별 할당량을 정상적으로 설정하는지 확인
        // 검증 항목:
        // 1. 할당량 설정이 정상적으로 저장되는지
        // 2. 모든 설정에 할당량이 적용되는지
        // 3. repository.save()가 호출되는지
        
        // Given
        Long dailyMetricLimit = 10000L;
        Long storageQuotaMb = 1000L;
        QuotaExceededAction quotaExceededAction = QuotaExceededAction.WARN_ONLY;
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.setQuotaForTenant(testTenantId, dailyMetricLimit, storageQuotaMb, quotaExceededAction);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getDailyMetricLimit()).isEqualTo(dailyMetricLimit);
        assertThat(testConfig.getStorageQuotaMb()).isEqualTo(storageQuotaMb);
        assertThat(testConfig.getQuotaExceededAction()).isEqualTo(quotaExceededAction);
    }

    @Test
    @DisplayName("테넌트별 할당량 조회 - 성공")
    void getQuotaForTenant_Success() {
        // 테스트 케이스: 테넌트별 할당량 조회 성공
        // 목적: 테넌트별 할당량을 정상적으로 조회하는지 확인
        // 검증 항목:
        // 1. 할당량 정보가 정상적으로 반환되는지
        // 2. DTO 변환이 정상적으로 이루어지는지
        // 3. repository.findAllByTenantId()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(10000L);
        testConfig.setStorageQuotaMb(1000L);
        testConfig.setCurrentDailyUsage(5000L);
        testConfig.setCurrentStorageUsageMb(500L);
        testConfig.setQuotaExceededAction(QuotaExceededAction.WARN_ONLY);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        
        // When
        TenantCollectorConfigDto result = service.getQuotaForTenant(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        assertThat(result.getTenantId()).isEqualTo(testTenantId);
        assertThat(result.getDailyMetricLimit()).isEqualTo(10000L);
        assertThat(result.getStorageQuotaMb()).isEqualTo(1000L);
        assertThat(result.getCurrentDailyUsage()).isEqualTo(5000L);
        assertThat(result.getCurrentStorageUsageMb()).isEqualTo(500L);
        assertThat(result.getQuotaExceededAction()).isEqualTo(QuotaExceededAction.WARN_ONLY);
    }

    @Test
    @DisplayName("테넌트별 할당량 조회 - 설정 없음")
    void getQuotaForTenant_NoConfig() {
        // 테스트 케이스: 테넌트별 할당량 조회 시 설정이 없는 경우
        // 목적: 설정이 없을 때 적절한 예외가 발생하는지 확인
        // 검증 항목:
        // 1. BusinessException이 발생하는지
        // 2. 적절한 에러 메시지가 포함되는지
        
        // Given
        when(repository.findAllByTenantId(testTenantId)).thenReturn(Collections.emptyList());
        
        // When & Then
        assertThatThrownBy(() -> service.getQuotaForTenant(testTenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("테넌트 설정을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("할당량 초과 여부 확인 - 초과")
    void isQuotaExceeded_Exceeded() {
        // 테스트 케이스: 할당량 초과 여부 확인 (초과)
        // 목적: 할당량이 초과되었을 때 true를 반환하는지 확인
        // 검증 항목:
        // 1. 할당량 초과 시 true를 반환하는지
        // 2. repository.findAllByTenantId()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(1000L);
        testConfig.setCurrentDailyUsage(1500L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        
        // When
        boolean result = service.isQuotaExceeded(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("할당량 초과 여부 확인 - 미초과")
    void isQuotaExceeded_NotExceeded() {
        // 테스트 케이스: 할당량 초과 여부 확인 (미초과)
        // 목적: 할당량이 초과되지 않았을 때 false를 반환하는지 확인
        // 검증 항목:
        // 1. 할당량 미초과 시 false를 반환하는지
        // 2. repository.findAllByTenantId()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(1000L);
        testConfig.setCurrentDailyUsage(500L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        
        // When
        boolean result = service.isQuotaExceeded(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("일일 사용량 증가 - 성공")
    void incrementDailyUsage_Success() {
        // 테스트 케이스: 일일 사용량 증가 성공
        // 목적: 일일 사용량을 정상적으로 증가시키는지 확인
        // 검증 항목:
        // 1. 사용량이 정상적으로 증가하는지
        // 2. repository.save()가 호출되는지
        
        // Given
        Long amount = 100L;
        testConfig.setCurrentDailyUsage(500L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.incrementDailyUsage(testTenantId, amount);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getCurrentDailyUsage()).isEqualTo(600L);
    }

    @Test
    @DisplayName("저장 공간 사용량 증가 - 성공")
    void incrementStorageUsage_Success() {
        // 테스트 케이스: 저장 공간 사용량 증가 성공
        // 목적: 저장 공간 사용량을 정상적으로 증가시키는지 확인
        // 검증 항목:
        // 1. 저장 공간 사용량이 정상적으로 증가하는지
        // 2. repository.save()가 호출되는지
        
        // Given
        Long amountMb = 50L;
        testConfig.setCurrentStorageUsageMb(200L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.incrementStorageUsage(testTenantId, amountMb);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getCurrentStorageUsageMb()).isEqualTo(250L);
    }

    @Test
    @DisplayName("할당량 초과 처리 - 수집기 비활성화")
    void handleQuotaExceeded_BlockCollection() {
        // 테스트 케이스: 할당량 초과 처리 (수집기 비활성화)
        // 목적: 할당량 초과 시 수집기를 비활성화하는지 확인
        // 검증 항목:
        // 1. 수집기가 비활성화되는지
        // 2. repository.save()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(1000L);
        testConfig.setCurrentDailyUsage(1500L);
        testConfig.setQuotaExceededAction(QuotaExceededAction.BLOCK_COLLECTION);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.handleQuotaExceeded(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getIsEnabled()).isFalse();
    }

    @Test
    @DisplayName("할당량 초과 처리 - 수집 주기 조절")
    void handleQuotaExceeded_ThrottleCollection() {
        // 테스트 케이스: 할당량 초과 처리 (수집 주기 조절)
        // 목적: 할당량 초과 시 수집 주기를 조절하는지 확인
        // 검증 항목:
        // 1. 수집 주기가 2배로 늘어나는지
        // 2. repository.save()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(1000L);
        testConfig.setCurrentDailyUsage(1500L);
        testConfig.setQuotaExceededAction(QuotaExceededAction.THROTTLE_COLLECTION);
        testConfig.setCollectionInterval(60000L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.handleQuotaExceeded(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getCollectionInterval()).isEqualTo(120000L);
    }

    @Test
    @DisplayName("할당량 초과 처리 - 경고만")
    void handleQuotaExceeded_WarnOnly() {
        // 테스트 케이스: 할당량 초과 처리 (경고만)
        // 목적: 할당량 초과 시 경고만 출력하는지 확인
        // 검증 항목:
        // 1. 설정이 변경되지 않는지
        // 2. repository.save()가 호출되는지
        
        // Given
        testConfig.setDailyMetricLimit(1000L);
        testConfig.setCurrentDailyUsage(1500L);
        testConfig.setQuotaExceededAction(QuotaExceededAction.WARN_ONLY);
        testConfig.setIsEnabled(true);
        testConfig.setCollectionInterval(60000L);
        
        List<TenantCollectorConfig> configs = Arrays.asList(testConfig);
        when(repository.findAllByTenantId(testTenantId)).thenReturn(configs);
        when(repository.save(any(TenantCollectorConfig.class))).thenReturn(testConfig);
        
        // When
        service.handleQuotaExceeded(testTenantId);
        
        // Then
        verify(repository, times(1)).findAllByTenantId(testTenantId);
        verify(repository, times(1)).save(testConfig);
        assertThat(testConfig.getIsEnabled()).isTrue();
        assertThat(testConfig.getCollectionInterval()).isEqualTo(60000L);
    }
}
