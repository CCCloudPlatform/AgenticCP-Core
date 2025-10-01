package com.agenticcp.core.domain.monitoring.storage;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import com.agenticcp.core.domain.monitoring.enums.StorageType;
import com.agenticcp.core.domain.monitoring.storage.impl.InfluxDBStorage;
import com.agenticcp.core.domain.monitoring.storage.impl.TimescaleDBStorage;
import com.agenticcp.core.domain.monitoring.storage.impl.PrometheusStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메트릭 저장소 팩토리 구현체
 * 
 * <p>다양한 타입의 메트릭 저장소를 생성하고 관리하는 팩토리 구현체입니다.
 * InfluxDB, TimescaleDB, Prometheus 등의 시계열 데이터베이스를 지원하며,
 * 저장소별 활성화/비활성화 상태를 관리합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // InfluxDB 저장소 생성
 * MetricsStorage influxStorage = factory.createStorage(StorageType.INFLUXDB);
 * 
 * // 모든 활성화된 저장소 생성
 * List<MetricsStorage> storages = factory.createAllStorages();
 * }</pre>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
public class MetricsStorageFactoryImpl implements MetricsStorageFactory {

    /**
     * 메트릭 저장소 팩토리 생성자
     * 
     * <p>Note: 저장소는 필요할 때 동적으로 생성됩니다 (Factory 패턴)
     */
    public MetricsStorageFactoryImpl() {
        // 의존성 주입 없음 - Factory가 직접 생성
    }
    
    /**
     * 저장소별 활성화 상태 관리
     * 기본적으로 InfluxDB만 활성화됨
     */
    private final Map<StorageType, Boolean> storageStatus = new ConcurrentHashMap<>();
    
    /**
     * 저장소별 설정 정보 관리
     */
    private final Map<StorageType, StorageConfig> storageConfigs = new ConcurrentHashMap<>();
    
    /**
     * 기본 설정 초기화
     */
    @PostConstruct
    public void initializeDefaultConfigs() {
        // InfluxDB만 기본 활성화
        storageStatus.put(StorageType.INFLUXDB, true);
        storageStatus.put(StorageType.TIMESCALEDB, false);
        storageStatus.put(StorageType.PROMETHEUS, false);
        
        // InfluxDB 기본 설정
        storageConfigs.put(StorageType.INFLUXDB, StorageConfig.builder()
                .enabled(true)
                .url("http://localhost:8086")
                .username("admin")
                .password("admin")
                .database("metrics")
                .timeout(30000)
                .retryCount(3)
                .build());
                
        // TimescaleDB 기본 설정 (비활성화)
        storageConfigs.put(StorageType.TIMESCALEDB, StorageConfig.builder()
                .enabled(false)
                .url("jdbc:postgresql://localhost:5432/metrics")
                .username("postgres")
                .password("postgres")
                .database("metrics")
                .timeout(30000)
                .retryCount(3)
                .build());
                
        // Prometheus 기본 설정 (비활성화)
        storageConfigs.put(StorageType.PROMETHEUS, StorageConfig.builder()
                .enabled(false)
                .url("http://localhost:9090")
                .username("")
                .password("")
                .database("metrics")
                .timeout(30000)
                .retryCount(3)
                .build());
                
        log.info("메트릭 저장소 팩토리 초기화 완료: 활성화된 저장소={}", 
                storageStatus.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .toList());
    }
    
    /**
     * 저장소 타입에 따른 MetricsStorage 생성
     * 
     * @param type 저장소 타입
     * @return 생성된 메트릭 저장소
     * @throws BusinessException 저장소 타입이 지원되지 않거나 비활성화된 경우
     */
    @Override
    public MetricsStorage createStorage(StorageType type) {
        log.debug("메트릭 저장소 생성 요청: type={}", type);
        
        try {
            // 저장소 타입 유효성 검증
            validateStorageType(type);
            
            // 저장소 활성화 상태 확인
            if (!isStorageEnabled(type)) {
                log.warn("비활성화된 저장소 생성 시도: type={}", type);
                throw new BusinessException(MonitoringErrorCode.COLLECTOR_DISABLED, 
                    "저장소가 비활성화되어 있습니다: " + type);
            }
            
            MetricsStorage storage = createStorageByType(type);
            
            log.debug("메트릭 저장소 생성 완료: type={}, enabled={}", 
                    type, storage.isEnabled());
            
            return storage;
            
        } catch (BusinessException e) {
            log.error("메트릭 저장소 생성 실패: type={}, error={}", type, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메트릭 저장소 생성 중 예상치 못한 오류: type={}", type, e);
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CREATION_FAILED, 
                "메트릭 저장소 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 활성화된 모든 저장소 생성
     * 
     * @return 활성화된 저장소 목록
     */
    @Override
    public List<MetricsStorage> createAllStorages() {
        log.debug("모든 활성화된 저장소 생성 요청");
        
        List<MetricsStorage> storages = new ArrayList<>();
        
        try {
            for (StorageType type : StorageType.values()) {
                if (isStorageEnabled(type)) {
                    try {
                        MetricsStorage storage = createStorage(type);
                        storages.add(storage);
                        log.debug("저장소 추가됨: type={}", type);
                    } catch (BusinessException e) {
                        log.warn("저장소 생성 실패 (건너뜀): type={}, error={}", type, e.getMessage());
                        // 개별 저장소 생성 실패는 전체 프로세스를 중단시키지 않음
                    }
                } else {
                    log.debug("비활성화된 저장소 건너뜀: type={}", type);
                }
            }
            
            log.info("활성화된 저장소 생성 완료: 총 {}개", storages.size());
            return storages;
            
        } catch (Exception e) {
            log.error("모든 저장소 생성 중 오류 발생", e);
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CREATION_FAILED, 
                "저장소 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 타입의 저장소 존재 여부 확인
     * 
     * @param type 저장소 타입
     * @return 존재 여부
     */
    @Override
    public boolean hasStorage(StorageType type) {
        if (type == null) {
            log.warn("null 저장소 타입으로 존재 여부 확인 시도");
            return false;
        }
        
        // 모든 StorageType은 지원됨 (동적 생성)
        boolean exists = true;
        
        log.debug("저장소 존재 여부 확인: type={}, exists={}", type, exists);
        return exists;
    }
    
    /**
     * 저장소 활성화/비활성화 설정
     * 
     * @param type 저장소 타입
     * @param enabled 활성화 여부
     */
    @Override
    public void setStorageEnabled(StorageType type, boolean enabled) {
        log.info("저장소 활성화 상태 변경: type={}, enabled={}", type, enabled);
        
        storageStatus.put(type, enabled);
        
        // 설정 정보도 업데이트
        StorageConfig config = storageConfigs.get(type);
        if (config != null) {
            StorageConfig updatedConfig = config.toBuilder()
                    .enabled(enabled)
                    .build();
            storageConfigs.put(type, updatedConfig);
        }
    }
    
    /**
     * 저장소 설정 정보 조회
     * 
     * @param type 저장소 타입
     * @return 저장소 설정 정보
     */
    @Override
    public StorageConfig getStorageConfig(StorageType type) {
        return storageConfigs.get(type);
    }
    
    /**
     * 저장소 설정 정보 업데이트
     * 
     * @param type 저장소 타입
     * @param config 새로운 설정 정보
     */
    @Override
    public void updateStorageConfig(StorageType type, StorageConfig config) {
        log.info("저장소 설정 업데이트: type={}, config={}", type, config);
        
        storageConfigs.put(type, config);
        storageStatus.put(type, config.isEnabled());
    }
    
    /**
     * 저장소 타입 유효성 검증
     */
    private void validateStorageType(StorageType type) {
        if (type == null) {
            throw new BusinessException(MonitoringErrorCode.INVALID_COLLECTOR_TYPE, 
                "저장소 타입이 null입니다.");
        }
        
        if (!hasStorage(type)) {
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_NOT_FOUND, 
                "지원되지 않는 저장소 타입입니다: " + type);
        }
    }
    
    /**
     * 저장소 활성화 상태 확인
     */
    private boolean isStorageEnabled(StorageType type) {
        return storageStatus.getOrDefault(type, false);
    }
    
    /**
     * 저장소 타입별 생성
     */
    private MetricsStorage createStorageByType(StorageType type) {
        StorageConfig config = getStorageConfig(type);
        
        return switch (type) {
            case INFLUXDB -> new InfluxDBStorage(config);
            case TIMESCALEDB -> new TimescaleDBStorage(config);
            case PROMETHEUS -> new PrometheusStorage(config);
        };
    }
}
