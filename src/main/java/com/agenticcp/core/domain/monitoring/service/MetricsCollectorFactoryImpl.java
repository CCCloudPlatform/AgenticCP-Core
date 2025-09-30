package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 메트릭 수집기 팩토리 구현체
 * 
 * <p>다양한 타입의 메트릭 수집기를 생성하고 관리하는 팩토리 구현체입니다.
 * 시스템 메트릭 수집기와 애플리케이션 메트릭 수집기를 지원하며,
 * 수집기별 활성화/비활성화 상태를 관리합니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // 시스템 메트릭 수집기 생성
 * MetricsCollector systemCollector = factory.createCollector(CollectorType.SYSTEM);
 * 
 * // 모든 활성화된 수집기 생성
 * List<MetricsCollector> collectors = factory.createAllCollectors();
 * }</pre>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCollectorFactoryImpl implements MetricsCollectorFactory {

    private final SystemMetricsCollector systemMetricsCollector;
    private final ApplicationMetricsCollector applicationMetricsCollector;
    
    /**
     * 수집기별 활성화 상태 관리
     * 기본적으로 모든 수집기가 활성화됨
     */
    private final Map<CollectorType, Boolean> collectorStatus = new ConcurrentHashMap<>();
    
    /**
     * 수집기별 설정 정보 관리
     */
    private final Map<CollectorType, CollectorConfig> collectorConfigs = new ConcurrentHashMap<>();
    
    
    /**
     * 기본 설정 초기화
     */
    @PostConstruct
    public void initializeDefaultConfigs() {
        // 모든 수집기 활성화
        collectorStatus.put(CollectorType.SYSTEM, true);
        collectorStatus.put(CollectorType.APPLICATION, true);
        
        // 기본 설정
        collectorConfigs.put(CollectorType.SYSTEM, CollectorConfig.builder()
                .enabled(true)
                .collectionInterval(60000L) // 1분
                .retryCount(3)
                .timeout(30000L) // 30초
                .build());
                
        collectorConfigs.put(CollectorType.APPLICATION, CollectorConfig.builder()
                .enabled(true)
                .collectionInterval(60000L) // 1분
                .retryCount(3)
                .timeout(30000L) // 30초
                .build());
                
        log.info("메트릭 수집기 팩토리 초기화 완료: 활성화된 수집기={}", 
                collectorStatus.entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey)
                        .toList());
    }
    
    /**
     * 수집기 타입에 따른 MetricsCollector 생성
     * 
     * @param type 수집기 타입
     * @return 생성된 메트릭 수집기
     * @throws BusinessException 수집기 타입이 지원되지 않거나 비활성화된 경우
     */
    @Override
    public MetricsCollector createCollector(CollectorType type) {
        log.debug("메트릭 수집기 생성 요청: type={}", type);
        
        try {
            // 수집기 타입 유효성 검증
            validateCollectorType(type);
            
            // 수집기 활성화 상태 확인
            if (!isCollectorEnabled(type)) {
                log.warn("비활성화된 수집기 생성 시도: type={}", type);
                throw new BusinessException(MonitoringErrorCode.COLLECTOR_DISABLED, 
                    "수집기가 비활성화되어 있습니다: " + type);
            }
            
            MetricsCollector collector = createCollectorByType(type);
            
            log.debug("메트릭 수집기 생성 완료: type={}, enabled={}", 
                    type, collector.isEnabled());
            
            return collector;
            
        } catch (BusinessException e) {
            log.error("메트릭 수집기 생성 실패: type={}, error={}", type, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메트릭 수집기 생성 중 예상치 못한 오류: type={}", type, e);
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CREATION_FAILED, 
                "메트릭 수집기 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 활성화된 모든 수집기 생성
     * 
     * @return 활성화된 수집기 목록
     */
    @Override
    public List<MetricsCollector> createAllCollectors() {
        log.debug("모든 활성화된 수집기 생성 요청");
        
        List<MetricsCollector> collectors = new ArrayList<>();
        
        try {
            for (CollectorType type : CollectorType.values()) {
                if (isCollectorEnabled(type)) {
                    try {
                        MetricsCollector collector = createCollector(type);
                        collectors.add(collector);
                        log.debug("수집기 추가됨: type={}", type);
                    } catch (BusinessException e) {
                        log.warn("수집기 생성 실패 (건너뜀): type={}, error={}", type, e.getMessage());
                        // 개별 수집기 생성 실패는 전체 프로세스를 중단시키지 않음
                    }
                } else {
                    log.debug("비활성화된 수집기 건너뜀: type={}", type);
                }
            }
            
            log.info("활성화된 수집기 생성 완료: 총 {}개", collectors.size());
            return collectors;
            
        } catch (Exception e) {
            log.error("모든 수집기 생성 중 오류 발생", e);
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_CREATION_FAILED, 
                "수집기 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 특정 타입의 수집기 존재 여부 확인
     * 
     * @param type 수집기 타입
     * @return 존재 여부
     */
    @Override
    public boolean hasCollector(CollectorType type) {
        if (type == null) {
            log.warn("null 수집기 타입으로 존재 여부 확인 시도");
            return false;
        }
        
        boolean exists = switch (type) {
            case SYSTEM -> systemMetricsCollector != null;
            case APPLICATION -> applicationMetricsCollector != null;
        };
        
        log.debug("수집기 존재 여부 확인: type={}, exists={}", type, exists);
        return exists;
    }
    
    /**
     * 수집기 활성화/비활성화 설정
     * 
     * @param type 수집기 타입
     * @param enabled 활성화 여부
     */
    public void setCollectorEnabled(CollectorType type, boolean enabled) {
        log.info("수집기 활성화 상태 변경: type={}, enabled={}", type, enabled);
        
        collectorStatus.put(type, enabled);
        
        // 설정 정보도 업데이트
        CollectorConfig config = collectorConfigs.get(type);
        if (config != null) {
            CollectorConfig updatedConfig = config.toBuilder()
                    .enabled(enabled)
                    .build();
            collectorConfigs.put(type, updatedConfig);
        }
    }
    
    /**
     * 수집기 설정 정보 조회
     * 
     * @param type 수집기 타입
     * @return 수집기 설정 정보
     */
    public CollectorConfig getCollectorConfig(CollectorType type) {
        return collectorConfigs.get(type);
    }
    
    /**
     * 수집기 설정 정보 업데이트
     * 
     * @param type 수집기 타입
     * @param config 새로운 설정 정보
     */
    public void updateCollectorConfig(CollectorType type, CollectorConfig config) {
        log.info("수집기 설정 업데이트: type={}, config={}", type, config);
        
        collectorConfigs.put(type, config);
        collectorStatus.put(type, config.isEnabled());
    }
    
    /**
     * 수집기 타입 유효성 검증
     */
    private void validateCollectorType(CollectorType type) {
        if (type == null) {
            throw new BusinessException(MonitoringErrorCode.INVALID_COLLECTOR_TYPE, 
                "수집기 타입이 null입니다.");
        }
        
        if (!hasCollector(type)) {
            throw new BusinessException(MonitoringErrorCode.COLLECTOR_NOT_FOUND, 
                "지원되지 않는 수집기 타입입니다: " + type);
        }
    }
    
    /**
     * 수집기 활성화 상태 확인
     */
    private boolean isCollectorEnabled(CollectorType type) {
        return collectorStatus.getOrDefault(type, false);
    }
    
    
    /**
     * 수집기 타입별 생성
     */
    private MetricsCollector createCollectorByType(CollectorType type) {
        return switch (type) {
            case SYSTEM -> (MetricsCollector) systemMetricsCollector;
            case APPLICATION -> (MetricsCollector) applicationMetricsCollector;
        };
    }
    
    /**
     * 수집기 설정 정보 클래스
     */
    public static class CollectorConfig {
        private final boolean enabled;
        private final long collectionInterval;
        private final int retryCount;
        private final long timeout;
        
        private CollectorConfig(Builder builder) {
            this.enabled = builder.enabled;
            this.collectionInterval = builder.collectionInterval;
            this.retryCount = builder.retryCount;
            this.timeout = builder.timeout;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public Builder toBuilder() {
            return new Builder()
                    .enabled(enabled)
                    .collectionInterval(collectionInterval)
                    .retryCount(retryCount)
                    .timeout(timeout);
        }
        
        // Getters
        public boolean isEnabled() { return enabled; }
        public long getCollectionInterval() { return collectionInterval; }
        public int getRetryCount() { return retryCount; }
        public long getTimeout() { return timeout; }
        
        public static class Builder {
            private boolean enabled = true;
            private long collectionInterval = 60000L;
            private int retryCount = 3;
            private long timeout = 30000L;
            
            public Builder enabled(boolean enabled) {
                this.enabled = enabled;
                return this;
            }
            
            public Builder collectionInterval(long collectionInterval) {
                this.collectionInterval = collectionInterval;
                return this;
            }
            
            public Builder retryCount(int retryCount) {
                this.retryCount = retryCount;
                return this;
            }
            
            public Builder timeout(long timeout) {
                this.timeout = timeout;
                return this;
            }
            
            public CollectorConfig build() {
                return new CollectorConfig(this);
            }
        }
    }
}
