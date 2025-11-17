package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.monitoring.dto.TenantCollectorConfigDto;
import com.agenticcp.core.domain.monitoring.entity.TenantCollectorConfig;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.QuotaExceededAction;
import com.agenticcp.core.domain.monitoring.repository.TenantCollectorConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantCollectorConfigService 단위 테스트
 * 
 * <p>테넌트별 수집기 설정 서비스의 핵심 비즈니스 로직을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantCollectorConfigService 단위 테스트")
class TenantCollectorConfigServiceTest {

    @Mock
    private TenantCollectorConfigRepository repository;

    @Mock
    private TenantDataRetentionService retentionService;

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

    @Nested
    @DisplayName("수집기 설정 조회")
    class GetConfigTest {
        
        @Test
        @DisplayName("테넌트별 활성화된 수집기 설정 조회 - 성공")
        void getEnabledConfigsByTenant_WhenCalled_ReturnsEnabledConfigs() {
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
        void getAllConfigsByTenant_WhenCalled_ReturnsAllConfigs() {
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
        void getConfigByTenantAndType_WhenExists_ReturnsConfig() {
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
        void getConfigByTenantAndType_WhenNotExists_ThrowsResourceNotFoundException() {
            // Given
        when(repository.findByTenantIdAndCollectorType(testTenantId, testCollectorType))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getConfigByTenantAndType(testTenantId, testCollectorType))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("수집기 설정 생성")
    class CreateConfigTest {
        
        @Test
        @DisplayName("수집기 설정 생성 - 성공")
        void createConfig_WhenValidRequest_ReturnsCreatedConfig() {
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
        void createConfig_WhenDuplicateExists_ThrowsBusinessException() {
            // Given
        when(repository.existsByTenantIdAndCollectorType(testTenantId, testCollectorType)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createConfig(testConfigDto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 존재하는 수집기 설정입니다");
        }
        
        @Test
        @DisplayName("수집기 설정 생성 - 유효하지 않은 테넌트 ID")
        void createConfig_WhenInvalidTenantId_ThrowsBusinessException() {
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
        void createConfig_WhenInvalidCollectionInterval_ThrowsBusinessException() {
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
    }
    
    @Nested
    @DisplayName("수집기 설정 수정")
    class UpdateConfigTest {
        
        @Test
        @DisplayName("수집기 설정 수정 - 성공")
        void updateConfig_WhenValidRequest_ReturnsUpdatedConfig() {
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
        void updateConfig_WhenNotExists_ThrowsResourceNotFoundException() {
            // Given
        Long configId = 1L;
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.updateConfig(configId, testConfigDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("수집기 설정 삭제")
    class DeleteConfigTest {
        
        @Test
        @DisplayName("수집기 설정 삭제 - 성공")
        void deleteConfig_WhenExists_DeletesConfig() {
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
        void deleteConfig_WhenNotExists_ThrowsResourceNotFoundException() {
            // Given
        Long configId = 1L;
        when(repository.existsById(configId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> service.deleteConfig(configId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("수집기 활성화/비활성화")
    class ToggleConfigTest {
        
        @Test
        @DisplayName("수집기 활성화/비활성화 - 성공")
        void toggleConfig_WhenCalled_ReturnsUpdatedConfig() {
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
        void toggleConfig_WhenNotExists_ThrowsResourceNotFoundException() {
            // Given
        Long configId = 1L;
        boolean enabled = false;
        when(repository.findById(configId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.toggleConfig(configId, enabled))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("수집기 설정을 찾을 수 없습니다");
        }
    }
    
    @Nested
    @DisplayName("수집기 타입 조회")
    class GetCollectorTypeTest {
        
        @Test
        @DisplayName("테넌트별 활성화된 수집기 타입 목록 조회 - 성공")
        void getEnabledCollectorTypesByTenant_WhenCalled_ReturnsCollectorTypes() {
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
        void getTenantIdsByCollectorType_WhenCalled_ReturnsTenantIds() {
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
        void countEnabledByTenant_WhenCalled_ReturnsCount() {
            // Given
        long count = 3L;
        when(repository.countEnabledByTenantId(testTenantId)).thenReturn(count);

        // When
        long result = service.countEnabledByTenant(testTenantId);

        // Then
        assertThat(result).isEqualTo(count);
        }
    }
    
    @Nested
    @DisplayName("설정 검증")
    class ValidateConfigTest {
        
        @Test
        @DisplayName("설정 검증 - 성공")
        void validateConfig_WhenValidConfig_DoesNotThrowException() {
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
        void validateConfig_WhenInvalidRetryCount_ThrowsBusinessException() {
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
        void validateConfig_WhenInvalidTimeout_ThrowsBusinessException() {
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
    }
    
    @Nested
    @DisplayName("할당량 관리")
    class QuotaManagementTest {
        
        @Test
        @DisplayName("테넌트별 할당량 설정 - 성공")
        void setQuotaForTenant_WhenCalled_SetsQuota() {
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
        void getQuotaForTenant_WhenExists_ReturnsQuota() {
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
        void getQuotaForTenant_WhenNoConfig_ThrowsBusinessException() {
            // Given
        when(repository.findAllByTenantId(testTenantId)).thenReturn(Collections.emptyList());
        
        // When & Then
        assertThatThrownBy(() -> service.getQuotaForTenant(testTenantId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("테넌트 설정을 찾을 수 없습니다");
        }
        
        @Test
        @DisplayName("할당량 초과 여부 확인 - 초과")
        void isQuotaExceeded_WhenExceeded_ReturnsTrue() {
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
        void isQuotaExceeded_WhenNotExceeded_ReturnsFalse() {
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
        void incrementDailyUsage_WhenCalled_IncrementsUsage() {
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
        void incrementStorageUsage_WhenCalled_IncrementsStorageUsage() {
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
        void handleQuotaExceeded_WhenBlockCollection_DisablesCollector() {
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
        void handleQuotaExceeded_WhenThrottleCollection_ThrottlesCollection() {
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
        void handleQuotaExceeded_WhenWarnOnly_DoesNotChangeConfig() {
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
}
