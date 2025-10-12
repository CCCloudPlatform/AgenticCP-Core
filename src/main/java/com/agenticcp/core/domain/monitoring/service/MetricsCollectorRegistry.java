package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.enums.CollectorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 메트릭 수집기 레지스트리
 * 
 * <p>메트릭 수집기를 동적으로 등록/해제하고 관리하는 중앙 레지스트리입니다.
 * 
 * <p>주요 기능:
 * <ul>
 *   <li>수집기 동적 등록/해제</li>
 *   <li>Spring Bean 자동 스캔 및 등록</li>
 *   <li>활성화된 수집기만 필터링</li>
 *   <li>수집기 타입별 조회</li>
 *   <li>Thread-safe 구현 (ConcurrentHashMap)</li>
 * </ul>
 * 
 * <p>플러그인 패턴:
 * <ul>
 *   <li>새로운 수집기 추가 시 코드 수정 불필요</li>
 *   <li>@Component만 붙이면 자동 등록</li>
 *   <li>런타임에 수집기 추가/제거 가능</li>
 * </ul>
 * 
 * <p>Issue #39: Task 6 - 메트릭 수집기 플러그인 시스템 구현
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
public class MetricsCollectorRegistry {
    
    /**
     * 등록된 수집기 맵 (Thread-safe)
     * Key: 수집기 고유 이름, Value: 수집기 인스턴스
     */
    private final ConcurrentHashMap<String, MetricsCollector> collectors = new ConcurrentHashMap<>();
    
    /**
     * Spring이 찾은 모든 MetricsCollector Bean을 자동 등록
     * 
     * <p>애플리케이션 시작 시 자동으로 호출되어 모든 수집기를 등록합니다.
     * 
     * @param collectorBeans Spring이 찾은 모든 MetricsCollector 구현체
     */
    @Autowired
    public void autoRegisterCollectors(List<MetricsCollector> collectorBeans) {
        log.info("메트릭 수집기 자동 등록 시작: {}개 발견", collectorBeans.size());
        
        for (MetricsCollector collector : collectorBeans) {
            String name = generateCollectorName(collector);
            registerCollector(name, collector);
        }
        
        log.info("메트릭 수집기 자동 등록 완료: 총 {}개, 활성화 {}개", 
                collectors.size(), 
                getEnabledCollectors().size());
    }
    
    /**
     * 수집기 등록
     * 
     * <p>새로운 수집기를 레지스트리에 등록합니다.
     * 
     * @param name 수집기 고유 이름
     * @param collector 수집기 인스턴스
     * @throws IllegalArgumentException name 또는 collector가 null인 경우
     */
    public void registerCollector(String name, MetricsCollector collector) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("수집기 이름은 필수입니다.");
        }
        
        if (collector == null) {
            throw new IllegalArgumentException("수집기 인스턴스는 필수입니다.");
        }
        
        collectors.put(name, collector);
        log.info("수집기 등록 완료: name={}, type={}, enabled={}", 
                name, collector.getCollectorType(), collector.isEnabled());
    }
    
    /**
     * 수집기 해제
     * 
     * <p>등록된 수집기를 레지스트리에서 제거합니다.
     * 
     * @param name 해제할 수집기 이름
     * @return 해제 성공 여부
     */
    public boolean unregisterCollector(String name) {
        MetricsCollector removed = collectors.remove(name);
        
        if (removed != null) {
            log.info("수집기 해제 완료: name={}, type={}", name, removed.getCollectorType());
            return true;
        }
        
        log.warn("수집기를 찾을 수 없습니다: name={}", name);
        return false;
    }
    
    /**
     * 수집기 조회
     * 
     * @param name 수집기 이름
     * @return 수집기 인스턴스 (Optional)
     */
    public Optional<MetricsCollector> getCollector(String name) {
        return Optional.ofNullable(collectors.get(name));
    }
    
    /**
     * 모든 수집기 조회
     * 
     * @return 등록된 모든 수집기 목록
     */
    public List<MetricsCollector> getAllCollectors() {
        return new ArrayList<>(collectors.values());
    }
    
    /**
     * 활성화된 수집기만 조회
     * 
     * <p>isEnabled()가 true인 수집기만 반환합니다.
     * 
     * @return 활성화된 수집기 목록
     */
    public List<MetricsCollector> getEnabledCollectors() {
        return collectors.values().stream()
                .filter(MetricsCollector::isEnabled)
                .collect(Collectors.toList());
    }
    
    /**
     * 타입별 수집기 조회
     * 
     * @param type 수집기 타입
     * @return 해당 타입의 수집기 목록
     */
    public List<MetricsCollector> getCollectorsByType(CollectorType type) {
        if (type == null) {
            return new ArrayList<>();
        }
        
        return collectors.values().stream()
                .filter(c -> c.getCollectorType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * 수집기 존재 여부 확인
     * 
     * @param name 수집기 이름
     * @return 존재 여부
     */
    public boolean hasCollector(String name) {
        return collectors.containsKey(name);
    }
    
    /**
     * 등록된 수집기 개수
     * 
     * @return 수집기 개수
     */
    public int getCollectorCount() {
        return collectors.size();
    }
    
    /**
     * 활성화된 수집기 개수
     * 
     * @return 활성화된 수집기 개수
     */
    public int getEnabledCollectorCount() {
        return (int) collectors.values().stream()
                .filter(MetricsCollector::isEnabled)
                .count();
    }
    
    /**
     * 모든 수집기 초기화
     * 
     * <p>주의: 모든 수집기가 제거됩니다.
     */
    public void clearAll() {
        int count = collectors.size();
        collectors.clear();
        log.info("모든 수집기 초기화 완료: {}개 수집기 제거됨", count);
    }
    
    /**
     * 수집기 상태 정보 조회
     * 
     * @return 수집기 이름과 활성화 상태 맵
     */
    public Map<String, Boolean> getCollectorStatus() {
        Map<String, Boolean> status = new HashMap<>();
        collectors.forEach((name, collector) -> 
                status.put(name, collector.isEnabled()));
        return status;
    }
    
    /**
     * 수집기 이름 자동 생성
     * 
     * <p>수집기 타입에 따라 고유한 이름을 생성합니다.
     * 
     * @param collector 수집기 인스턴스
     * @return 생성된 수집기 이름
     */
    private String generateCollectorName(MetricsCollector collector) {
        String typeName = collector.getCollectorType().name().toLowerCase();
        String className = collector.getClass().getSimpleName().toLowerCase();
        
        // 클래스 이름에서 "collector" 제거
        String cleanName = className.replace("collector", "").replace("metrics", "");
        
        if (cleanName.isEmpty()) {
            return typeName;
        }
        
        return cleanName.isEmpty() ? typeName : cleanName + "-" + typeName;
    }
    
    /**
     * 수집기 정보 조회
     * 
     * @return 수집기 이름, 타입, 상태 정보 목록
     */
    public List<CollectorInfo> getCollectorInfoList() {
        return collectors.entrySet().stream()
                .map(entry -> new CollectorInfo(
                        entry.getKey(),
                        entry.getValue().getCollectorType(),
                        entry.getValue().isEnabled()
                ))
                .collect(Collectors.toList());
    }
    
    /**
     * 수집기 정보 DTO
     */
    public static class CollectorInfo {
        private final String name;
        private final CollectorType type;
        private final boolean enabled;
        
        public CollectorInfo(String name, CollectorType type, boolean enabled) {
            this.name = name;
            this.type = type;
            this.enabled = enabled;
        }
        
        public String getName() {
            return name;
        }
        
        public CollectorType getType() {
            return type;
        }
        
        public boolean isEnabled() {
            return enabled;
        }
        
        @Override
        public String toString() {
            return String.format("CollectorInfo{name='%s', type=%s, enabled=%s}", 
                    name, type, enabled);
        }
    }
}

