package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemMetricsCollector 단위 테스트
 * 
 * <p>시스템 리소스 메트릭 수집기의 핵심 로직을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemMetricsCollector 단위 테스트")
class SystemMetricsCollectorTest {
    
    private SystemMetricsCollector systemMetricsCollector;
    
    @BeforeEach
    void setUp() {
        systemMetricsCollector = new SystemMetricsCollector();
    }
    
    @Nested
    @DisplayName("시스템 메트릭 수집")
    class CollectSystemMetricsTest {
        
        @Test
        @DisplayName("시스템 메트릭 수집 성공 시 유효한 메트릭 객체 반환")
        void collectSystemMetrics_WhenCalled_ReturnsValidMetrics() {
            // Given
            // (setUp에서 이미 초기화됨)
            
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
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
        
        @Test
        @DisplayName("시스템 정보 수집 검증")
        void collectSystemMetrics_WhenCalled_CollectsSystemInfo() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            SystemMetrics.SystemInfo systemInfo = result.getSystemInfo();
            assertThat(systemInfo).isNotNull();
            assertThat(systemInfo.getHostname()).isNotBlank();
            assertThat(systemInfo.getOsName()).isNotBlank();
            assertThat(systemInfo.getJavaVersion()).isNotBlank();
            assertThat(systemInfo.getAvailableProcessors()).isPositive();
        }
        
        @Test
        @DisplayName("수집 시간 검증")
        void collectSystemMetrics_WhenCalled_SetsCurrentTime() {
            // Given
            LocalDateTime beforeCollection = LocalDateTime.now();
            
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            LocalDateTime afterCollection = LocalDateTime.now();
            assertThat(result.getCollectedAt()).isBetween(beforeCollection, afterCollection);
        }
    }
    
    @Nested
    @DisplayName("메모리 사용량 계산")
    class MemoryUsageCalculationTest {
        
        @Test
        @DisplayName("메모리 사용량 계산 검증")
        void collectSystemMetrics_WhenCalled_CalculatesMemoryUsage() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            if (result.getMemoryUsedMB() != null && result.getMemoryTotalMB() != null) {
                assertThat(result.getMemoryUsedMB()).isPositive();
                assertThat(result.getMemoryTotalMB()).isPositive();
                assertThat(result.getMemoryUsedMB()).isLessThanOrEqualTo(result.getMemoryTotalMB());
            }
        }
        
        @Test
        @DisplayName("메모리 사용률 범위 검증")
        void collectSystemMetrics_WhenCalled_ReturnsValidMemoryUsage() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            if (result.getMemoryUsage() != null) {
                assertThat(result.getMemoryUsage()).isBetween(0.0, 100.0);
            }
        }
    }
    
    @Nested
    @DisplayName("디스크 사용량 계산")
    class DiskUsageCalculationTest {
        
        @Test
        @DisplayName("디스크 사용량 계산 검증")
        void collectSystemMetrics_WhenCalled_CalculatesDiskUsage() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            if (result.getDiskUsedGB() != null && result.getDiskTotalGB() != null) {
                assertThat(result.getDiskUsedGB()).isPositive();
                assertThat(result.getDiskTotalGB()).isPositive();
                assertThat(result.getDiskUsedGB()).isLessThanOrEqualTo(result.getDiskTotalGB());
            }
        }
        
        @Test
        @DisplayName("디스크 사용률 범위 검증")
        void collectSystemMetrics_WhenCalled_ReturnsValidDiskUsage() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            if (result.getDiskUsage() != null) {
                assertThat(result.getDiskUsage()).isBetween(0.0, 100.0);
            }
        }
    }
    
    @Nested
    @DisplayName("CPU 사용률 검증")
    class CpuUsageValidationTest {
        
        @Test
        @DisplayName("CPU 사용률 범위 검증")
        void collectSystemMetrics_WhenCalled_ReturnsValidCpuUsage() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            if (result.getCpuUsage() != null) {
                assertThat(result.getCpuUsage()).isBetween(0.0, 100.0);
            }
        }
    }
    
    @Nested
    @DisplayName("메타데이터 구성")
    class MetadataBuildTest {
        
        @Test
        @DisplayName("메타데이터 구성 검증")
        void collectSystemMetrics_WhenCalled_BuildsMetadata() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            assertThat(result.getMetadata()).isNotNull();
            assertThat(result.getMetadata()).isNotEmpty();
            
            // 시스템 정보가 메타데이터에 포함되어 있는지 확인
            assertThat(result.getMetadata()).containsKey("hostname");
            assertThat(result.getMetadata()).containsKey("os_name");
            assertThat(result.getMetadata()).containsKey("java_version");
            assertThat(result.getMetadata()).containsKey("available_processors");
        }
    }
    
    @Nested
    @DisplayName("시스템 정보 유효성")
    class SystemInfoValidationTest {
        
        @Test
        @DisplayName("시스템 정보 유효성 검증")
        void collectSystemMetrics_WhenCalled_ReturnsValidSystemInfo() {
            // When
            SystemMetrics result = systemMetricsCollector.collectSystemMetrics();
            
            // Then
            SystemMetrics.SystemInfo systemInfo = result.getSystemInfo();
            assertThat(systemInfo.getHostname()).isNotBlank();
            assertThat(systemInfo.getOsName()).isNotBlank();
            assertThat(systemInfo.getOsVersion()).isNotBlank();
            assertThat(systemInfo.getJavaVersion()).isNotBlank();
            assertThat(systemInfo.getAvailableProcessors()).isPositive();
        }
    }
}
