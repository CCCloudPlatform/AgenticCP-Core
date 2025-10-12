package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.domain.monitoring.enums.MonitoringErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * SystemMetricsCollector 단위 테스트
 * 시스템 리소스 메트릭 수집기의 핵심 로직을 검증
 */
@ExtendWith(MockitoExtension.class)
class SystemMetricsCollectorTest {
    
    private SystemMetricsCollector systemMetricsCollector;
    
    @BeforeEach
    void setUp() {
        systemMetricsCollector = new SystemMetricsCollector();
    }
    
    /**
     * 시나리오 1: 정상적인 시스템 메트릭 수집
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 유효한 SystemMetrics 객체 반환
     */
    @Test
    @DisplayName("시스템 메트릭 수집 성공 시 유효한 메트릭 객체 반환")
    void collectSystemMetrics_shouldReturnValidMetrics() {
        // Given: SystemMetricsCollector가 초기화됨
        // (setUp에서 이미 초기화됨)
        
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 유효한 SystemMetrics 객체 반환
        assertThat(result).isNotNull();
        assertThat(result.getCollectedAt()).isNotNull();
        assertThat(result.getSystemInfo()).isNotNull();
        
        // CPU 사용률 검증 (0-100% 범위)
        if (result.getCpuUsage() != null) {
            assertThat(result.getCpuUsage()).isBetween(0.0, 100.0);
        }
        
        // 메모리 사용률 검증 (0-100% 범위)
        if (result.getMemoryUsage() != null) {
            assertThat(result.getMemoryUsage()).isBetween(0.0, 100.0);
        }
        
        // 디스크 사용률 검증 (0-100% 범위)
        if (result.getDiskUsage() != null) {
            assertThat(result.getDiskUsage()).isBetween(0.0, 100.0);
        }
    }
    
    /**
     * 시나리오 2: 시스템 정보 수집 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 시스템 정보가 올바르게 수집됨
     */
    @Test
    @DisplayName("시스템 정보 수집 검증")
    void collectSystemMetrics_shouldCollectSystemInfo() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 시스템 정보가 올바르게 수집됨
        SystemMetrics.SystemInfo systemInfo = result.getSystemInfo();
        assertThat(systemInfo).isNotNull();
        assertThat(systemInfo.getHostname()).isNotBlank();
        assertThat(systemInfo.getOsName()).isNotBlank();
        assertThat(systemInfo.getJavaVersion()).isNotBlank();
        assertThat(systemInfo.getAvailableProcessors()).isPositive();
    }
    
    /**
     * 시나리오 3: 메모리 사용량 계산 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 메모리 사용량과 총량이 올바르게 계산됨
     */
    @Test
    @DisplayName("메모리 사용량 계산 검증")
    void collectSystemMetrics_shouldCalculateMemoryUsage() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 메모리 사용량이 올바르게 계산됨
        if (result.getMemoryUsedMB() != null && result.getMemoryTotalMB() != null) {
            assertThat(result.getMemoryUsedMB()).isPositive();
            assertThat(result.getMemoryTotalMB()).isPositive();
            assertThat(result.getMemoryUsedMB()).isLessThanOrEqualTo(result.getMemoryTotalMB());
        }
    }
    
    /**
     * 시나리오 4: 디스크 사용량 계산 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 디스크 사용량과 총량이 올바르게 계산됨
     */
    @Test
    @DisplayName("디스크 사용량 계산 검증")
    void collectSystemMetrics_shouldCalculateDiskUsage() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 디스크 사용량이 올바르게 계산됨
        if (result.getDiskUsedGB() != null && result.getDiskTotalGB() != null) {
            assertThat(result.getDiskUsedGB()).isPositive();
            assertThat(result.getDiskTotalGB()).isPositive();
            assertThat(result.getDiskUsedGB()).isLessThanOrEqualTo(result.getDiskTotalGB());
        }
    }
    
    /**
     * 시나리오 5: 메타데이터 구성 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 메타데이터가 올바르게 구성됨
     */
    @Test
    @DisplayName("메타데이터 구성 검증")
    void collectSystemMetrics_shouldBuildMetadata() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 메타데이터가 올바르게 구성됨
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).isNotEmpty();
        
        // 시스템 정보가 메타데이터에 포함되어 있는지 확인
        SystemMetrics.SystemInfo systemInfo = result.getSystemInfo();
        assertThat(result.getMetadata()).containsKey("hostname");
        assertThat(result.getMetadata()).containsKey("os_name");
        assertThat(result.getMetadata()).containsKey("java_version");
        assertThat(result.getMetadata()).containsKey("available_processors");
    }
    
    /**
     * 시나리오 6: 수집 시간 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 수집 시간이 현재 시간과 유사함
     */
    @Test
    @DisplayName("수집 시간 검증")
    void collectSystemMetrics_shouldSetCurrentTime() {
        // Given: 현재 시간 기록
        LocalDateTime beforeCollection = LocalDateTime.now();
        
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 수집 시간이 현재 시간과 유사함
        LocalDateTime afterCollection = LocalDateTime.now();
        assertThat(result.getCollectedAt()).isBetween(beforeCollection, afterCollection);
    }
    
    /**
     * 시나리오 7: CPU 사용률 범위 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: CPU 사용률이 0-100% 범위 내에 있음
     */
    @Test
    @DisplayName("CPU 사용률 범위 검증")
    void collectSystemMetrics_shouldReturnValidCpuUsage() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: CPU 사용률이 유효한 범위 내에 있음
        if (result.getCpuUsage() != null) {
            assertThat(result.getCpuUsage()).isBetween(0.0, 100.0);
        }
    }
    
    /**
     * 시나리오 8: 메모리 사용률 범위 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 메모리 사용률이 0-100% 범위 내에 있음
     */
    @Test
    @DisplayName("메모리 사용률 범위 검증")
    void collectSystemMetrics_shouldReturnValidMemoryUsage() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 메모리 사용률이 유효한 범위 내에 있음
        if (result.getMemoryUsage() != null) {
            assertThat(result.getMemoryUsage()).isBetween(0.0, 100.0);
        }
    }
    
    /**
     * 시나리오 9: 디스크 사용률 범위 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 디스크 사용률이 0-100% 범위 내에 있음
     */
    @Test
    @DisplayName("디스크 사용률 범위 검증")
    void collectSystemMetrics_shouldReturnValidDiskUsage() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 디스크 사용률이 유효한 범위 내에 있음
        if (result.getDiskUsage() != null) {
            assertThat(result.getDiskUsage()).isBetween(0.0, 100.0);
        }
    }
    
    /**
     * 시나리오 10: 시스템 정보 유효성 검증
     * Given: SystemMetricsCollector가 초기화됨
     * When: collectSystemMetrics() 호출
     * Then: 시스템 정보가 유효한 값들을 포함함
     */
    @Test
    @DisplayName("시스템 정보 유효성 검증")
    void collectSystemMetrics_shouldReturnValidSystemInfo() {
        // When: collectSystemMetrics() 호출
        SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
        
        // Then: 시스템 정보가 유효한 값들을 포함함
        SystemMetrics.SystemInfo systemInfo = result.getSystemInfo();
        assertThat(systemInfo.getHostname()).isNotBlank();
        assertThat(systemInfo.getOsName()).isNotBlank();
        assertThat(systemInfo.getOsVersion()).isNotBlank();
        assertThat(systemInfo.getJavaVersion()).isNotBlank();
        assertThat(systemInfo.getAvailableProcessors()).isPositive();
    }
}
