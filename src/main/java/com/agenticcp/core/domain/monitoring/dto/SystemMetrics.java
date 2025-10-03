package com.agenticcp.core.domain.monitoring.dto;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 시스템 리소스 메트릭을 담는 DTO
 * CPU, 메모리, 디스크 사용량 정보를 포함
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class SystemMetrics {
    
    /**
     * CPU 사용률 (%)
     */
    private final Double cpuUsage;
    
    /**
     * 메모리 사용률 (%)
     */
    private final Double memoryUsage;
    
    /**
     * 메모리 사용량 (MB)
     */
    private final Long memoryUsedMB;
    
    /**
     * 메모리 총량 (MB)
     */
    private final Long memoryTotalMB;
    
    /**
     * 디스크 사용률 (%)
     */
    private final Double diskUsage;
    
    /**
     * 디스크 사용량 (GB)
     */
    private final Long diskUsedGB;
    
    /**
     * 디스크 총량 (GB)
     */
    private final Long diskTotalGB;
    
    /**
     * 메트릭 수집 시간
     */
    private final LocalDateTime collectedAt;
    
    /**
     * 추가 메타데이터
     */
    private final Map<String, Object> metadata;
    
    /**
     * 시스템 정보
     */
    private final SystemInfo systemInfo;
    
    @Getter
    @Builder
    @EqualsAndHashCode
    @ToString
    public static class SystemInfo {
        private final String hostname;
        private final String osName;
        private final String osVersion;
        private final String javaVersion;
        private final Integer availableProcessors;
    }
}
