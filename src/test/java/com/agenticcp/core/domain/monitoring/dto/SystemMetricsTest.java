package com.agenticcp.core.domain.monitoring.dto;

import com.agenticcp.core.domain.monitoring.TestDataBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * SystemMetrics DTO 단위 테스트
 * 핵심 DTO의 데이터 검증 로직을 검증
 * 테스트 가이드라인에 따라 @Nested 클래스로 그룹화
 */
class SystemMetricsTest {

    /**
     * 빌더 패턴 테스트 그룹
     * 시나리오 1: 정상적인 SystemMetrics 생성
     * 시나리오 2: 다양한 시스템 메트릭으로 생성
     * 시나리오 3: TestDataBuilder를 사용한 생성
     */
    @Nested
    @DisplayName("빌더 패턴 테스트")
    class BuilderPatternTest {

        @Test
        @DisplayName("정상적인 SystemMetrics 생성")
        void buildSystemMetrics_WhenValidData_ShouldCreateMetrics() {
            // Given
            Double cpuUsage = 45.2;
            Double memoryUsage = 67.8;
            Long memoryUsedMB = 2048L;
            Long memoryTotalMB = 4096L;
            Double diskUsage = 23.1;
            Long diskUsedGB = 100L;
            Long diskTotalGB = 500L;
            LocalDateTime collectedAt = LocalDateTime.now();

            // When
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(cpuUsage)
                    .memoryUsage(memoryUsage)
                    .memoryUsedMB(memoryUsedMB)
                    .memoryTotalMB(memoryTotalMB)
                    .diskUsage(diskUsage)
                    .diskUsedGB(diskUsedGB)
                    .diskTotalGB(diskTotalGB)
                    .collectedAt(collectedAt)
                    .build();

            // Then
            assertThat(metrics.getCpuUsage()).isEqualTo(cpuUsage);
            assertThat(metrics.getMemoryUsage()).isEqualTo(memoryUsage);
            assertThat(metrics.getMemoryUsedMB()).isEqualTo(memoryUsedMB);
            assertThat(metrics.getMemoryTotalMB()).isEqualTo(memoryTotalMB);
            assertThat(metrics.getDiskUsage()).isEqualTo(diskUsage);
            assertThat(metrics.getDiskUsedGB()).isEqualTo(diskUsedGB);
            assertThat(metrics.getDiskTotalGB()).isEqualTo(diskTotalGB);
            assertThat(metrics.getCollectedAt()).isEqualTo(collectedAt);
        }

        @Test
        @DisplayName("다양한 시스템 메트릭으로 생성")
        void buildSystemMetrics_WithDifferentValues_ShouldCreateMetrics() {
            // Given & When
            SystemMetrics lowUsageMetrics = SystemMetrics.builder()
                    .cpuUsage(25.0)
                    .memoryUsage(40.0)
                    .memoryUsedMB(1024L)
                    .memoryTotalMB(4096L)
                    .diskUsage(15.0)
                    .diskUsedGB(50L)
                    .diskTotalGB(500L)
                    .collectedAt(LocalDateTime.now())
                    .build();

            SystemMetrics highUsageMetrics = SystemMetrics.builder()
                    .cpuUsage(85.0)
                    .memoryUsage(90.0)
                    .memoryUsedMB(8192L)
                    .memoryTotalMB(16384L)
                    .diskUsage(75.0)
                    .diskUsedGB(400L)
                    .diskTotalGB(1000L)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(lowUsageMetrics.getCpuUsage()).isEqualTo(25.0);
            assertThat(highUsageMetrics.getCpuUsage()).isEqualTo(85.0);
            assertThat(lowUsageMetrics.getMemoryUsage()).isEqualTo(40.0);
            assertThat(highUsageMetrics.getMemoryUsage()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("TestDataBuilder를 사용한 SystemMetrics 생성")
        void buildSystemMetrics_UsingTestDataBuilder_ShouldCreateMetrics() {
            // When
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // Then
            assertThat(metrics.getCpuUsage()).isEqualTo(45.2);
            assertThat(metrics.getMemoryUsage()).isEqualTo(67.8);
            assertThat(metrics.getMemoryUsedMB()).isEqualTo(2048L);
            assertThat(metrics.getMemoryTotalMB()).isEqualTo(4096L);
            assertThat(metrics.getDiskUsage()).isEqualTo(23.1);
            assertThat(metrics.getDiskUsedGB()).isEqualTo(100L);
            assertThat(metrics.getDiskTotalGB()).isEqualTo(500L);
        }
    }

    /**
     * SystemInfo 내부 클래스 테스트 그룹
     * 시나리오 4: SystemInfo 생성 및 검증
     * 시나리오 5: SystemInfo의 다양한 필드 검증
     * 시나리오 6: SystemInfo의 null 값 처리
     */
    @Nested
    @DisplayName("SystemInfo 내부 클래스 테스트")
    class SystemInfoTest {

        @Test
        @DisplayName("SystemInfo 생성 및 검증")
        void buildSystemInfo_WhenValidData_ShouldCreateSystemInfo() {
            // Given
            String hostname = "test-server";
            String osName = "Windows 10";
            String osVersion = "10.0";
            String javaVersion = "17.0.1";
            Integer availableProcessors = 8;

            // When
            SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                    .hostname(hostname)
                    .osName(osName)
                    .osVersion(osVersion)
                    .javaVersion(javaVersion)
                    .availableProcessors(availableProcessors)
                    .build();

            // Then
            assertThat(systemInfo.getHostname()).isEqualTo(hostname);
            assertThat(systemInfo.getOsName()).isEqualTo(osName);
            assertThat(systemInfo.getOsVersion()).isEqualTo(osVersion);
            assertThat(systemInfo.getJavaVersion()).isEqualTo(javaVersion);
            assertThat(systemInfo.getAvailableProcessors()).isEqualTo(availableProcessors);
        }

        @Test
        @DisplayName("SystemInfo의 다양한 필드 검증")
        void buildSystemInfo_WithDifferentValues_ShouldCreateSystemInfo() {
            // Given & When
            SystemMetrics.SystemInfo linuxInfo = SystemMetrics.SystemInfo.builder()
                    .hostname("linux-server")
                    .osName("Linux")
                    .osVersion("Ubuntu 20.04")
                    .javaVersion("11.0.2")
                    .availableProcessors(16)
                    .build();

            SystemMetrics.SystemInfo windowsInfo = SystemMetrics.SystemInfo.builder()
                    .hostname("windows-server")
                    .osName("Windows Server 2019")
                    .osVersion("10.0.17763")
                    .javaVersion("17.0.1")
                    .availableProcessors(32)
                    .build();

            // Then
            assertThat(linuxInfo.getOsName()).isEqualTo("Linux");
            assertThat(windowsInfo.getOsName()).isEqualTo("Windows Server 2019");
            assertThat(linuxInfo.getAvailableProcessors()).isEqualTo(16);
            assertThat(windowsInfo.getAvailableProcessors()).isEqualTo(32);
        }

        @Test
        @DisplayName("SystemInfo의 null 값 처리")
        void buildSystemInfo_WithNullValues_ShouldHandleGracefully() {
            // When
            SystemMetrics.SystemInfo systemInfo = SystemMetrics.SystemInfo.builder()
                    .hostname("test-server")
                    .osName("Windows 10")
                    .build();

            // Then
            assertThat(systemInfo.getHostname()).isEqualTo("test-server");
            assertThat(systemInfo.getOsName()).isEqualTo("Windows 10");
            assertThat(systemInfo.getOsVersion()).isNull();
            assertThat(systemInfo.getJavaVersion()).isNull();
            assertThat(systemInfo.getAvailableProcessors()).isNull();
        }
    }

    /**
     * 유효성 검증 테스트 그룹
     * 시나리오 7: null 값 처리
     * 시나리오 8: 음수 값 처리
     * 시나리오 9: 100% 초과 값 처리
     */
    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("null 값으로 SystemMetrics 생성")
        void buildSystemMetrics_WithNullValues_ShouldCreateMetrics() {
            // When
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(null)
                    .memoryUsage(null)
                    .memoryUsedMB(null)
                    .memoryTotalMB(null)
                    .diskUsage(null)
                    .diskUsedGB(null)
                    .diskTotalGB(null)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(metrics.getCpuUsage()).isNull();
            assertThat(metrics.getMemoryUsage()).isNull();
            assertThat(metrics.getMemoryUsedMB()).isNull();
            assertThat(metrics.getMemoryTotalMB()).isNull();
            assertThat(metrics.getDiskUsage()).isNull();
            assertThat(metrics.getDiskUsedGB()).isNull();
            assertThat(metrics.getDiskTotalGB()).isNull();
        }

        @Test
        @DisplayName("음수 값으로 SystemMetrics 생성")
        void buildSystemMetrics_WithNegativeValues_ShouldCreateMetrics() {
            // When
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(-10.0)
                    .memoryUsage(-5.0)
                    .memoryUsedMB(-100L)
                    .memoryTotalMB(-200L)
                    .diskUsage(-15.0)
                    .diskUsedGB(-50L)
                    .diskTotalGB(-100L)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(metrics.getCpuUsage()).isEqualTo(-10.0);
            assertThat(metrics.getMemoryUsage()).isEqualTo(-5.0);
            assertThat(metrics.getMemoryUsedMB()).isEqualTo(-100L);
            assertThat(metrics.getMemoryTotalMB()).isEqualTo(-200L);
            assertThat(metrics.getDiskUsage()).isEqualTo(-15.0);
            assertThat(metrics.getDiskUsedGB()).isEqualTo(-50L);
            assertThat(metrics.getDiskTotalGB()).isEqualTo(-100L);
        }

        @Test
        @DisplayName("100% 초과 값으로 SystemMetrics 생성")
        void buildSystemMetrics_WithExcessiveValues_ShouldCreateMetrics() {
            // When
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(150.0)
                    .memoryUsage(200.0)
                    .diskUsage(300.0)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // Then
            assertThat(metrics.getCpuUsage()).isEqualTo(150.0);
            assertThat(metrics.getMemoryUsage()).isEqualTo(200.0);
            assertThat(metrics.getDiskUsage()).isEqualTo(300.0);
        }
    }

    /**
     * equals/hashCode 테스트 그룹
     * 시나리오 10: 동일한 데이터로 생성된 SystemMetrics는 equals true
     * 시나리오 11: 다른 데이터로 생성된 SystemMetrics는 equals false
     * 시나리오 12: hashCode 일관성 검증
     */
    @Nested
    @DisplayName("equals/hashCode 테스트")
    class EqualsHashCodeTest {

        @Test
        @DisplayName("동일한 데이터로 생성된 SystemMetrics는 equals true")
        void equals_WhenSameData_ShouldReturnTrue() {
            // Given
            LocalDateTime now = LocalDateTime.now();
            SystemMetrics metrics1 = SystemMetrics.builder()
                    .cpuUsage(45.2)
                    .memoryUsage(67.8)
                    .memoryUsedMB(2048L)
                    .memoryTotalMB(4096L)
                    .diskUsage(23.1)
                    .diskUsedGB(100L)
                    .diskTotalGB(500L)
                    .collectedAt(now)
                    .build();

            SystemMetrics metrics2 = SystemMetrics.builder()
                    .cpuUsage(45.2)
                    .memoryUsage(67.8)
                    .memoryUsedMB(2048L)
                    .memoryTotalMB(4096L)
                    .diskUsage(23.1)
                    .diskUsedGB(100L)
                    .diskTotalGB(500L)
                    .collectedAt(now)
                    .build();

            // When & Then
            assertThat(metrics1).isEqualTo(metrics2);
            assertThat(metrics1.hashCode()).isEqualTo(metrics2.hashCode());
        }

        @Test
        @DisplayName("다른 데이터로 생성된 SystemMetrics는 equals false")
        void equals_WhenDifferentData_ShouldReturnFalse() {
            // Given
            SystemMetrics metrics1 = SystemMetrics.builder()
                    .cpuUsage(45.2)
                    .memoryUsage(67.8)
                    .collectedAt(LocalDateTime.now())
                    .build();

            SystemMetrics metrics2 = SystemMetrics.builder()
                    .cpuUsage(85.0)
                    .memoryUsage(90.0)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // When & Then
            assertThat(metrics1).isNotEqualTo(metrics2);
        }

        @Test
        @DisplayName("null 값과의 equals 비교")
        void equals_WhenComparedWithNull_ShouldReturnFalse() {
            // Given
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // When & Then
            assertThat(metrics.equals(null)).isFalse();
        }
    }

    /**
     * toString 테스트 그룹
     * 시나리오 13: toString 메서드가 모든 필드를 포함하는지 검증
     * 시나리오 14: null 값이 포함된 경우 toString 동작 검증
     */
    @Nested
    @DisplayName("toString 테스트")
    class ToStringTest {

        @Test
        @DisplayName("toString 메서드가 모든 필드를 포함")
        void toString_ShouldContainAllFields() {
            // Given
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // When
            String toString = metrics.toString();

            // Then
            assertThat(toString).contains("cpuUsage=45.2");
            assertThat(toString).contains("memoryUsage=67.8");
            assertThat(toString).contains("memoryUsedMB=2048");
            assertThat(toString).contains("memoryTotalMB=4096");
            assertThat(toString).contains("diskUsage=23.1");
            assertThat(toString).contains("diskUsedGB=100");
            assertThat(toString).contains("diskTotalGB=500");
        }

        @Test
        @DisplayName("null 값이 포함된 경우 toString 동작")
        void toString_WithNullValues_ShouldHandleGracefully() {
            // Given
            SystemMetrics metrics = SystemMetrics.builder()
                    .cpuUsage(45.2)
                    .memoryUsage(null)
                    .collectedAt(LocalDateTime.now())
                    .build();

            // When
            String toString = metrics.toString();

            // Then
            assertThat(toString).contains("cpuUsage=45.2");
            assertThat(toString).contains("memoryUsage=null");
        }
    }

    /**
     * 비즈니스 로직 테스트 그룹
     * 시나리오 15: 메트릭 값 검증
     * 시나리오 16: SystemInfo 연관관계 검증
     * 시나리오 17: 수집 시간 검증
     */
    @Nested
    @DisplayName("비즈니스 로직 테스트")
    class BusinessLogicTest {

        @Test
        @DisplayName("메트릭 값 검증")
        void validateMetricValues_ShouldReturnCorrectValues() {
            // Given
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // When & Then
            assertThat(metrics.getCpuUsage()).isBetween(0.0, 100.0);
            assertThat(metrics.getMemoryUsage()).isBetween(0.0, 100.0);
            assertThat(metrics.getDiskUsage()).isBetween(0.0, 100.0);
            assertThat(metrics.getMemoryUsedMB()).isLessThanOrEqualTo(metrics.getMemoryTotalMB());
            assertThat(metrics.getDiskUsedGB()).isLessThanOrEqualTo(metrics.getDiskTotalGB());
        }

        @Test
        @DisplayName("SystemInfo 연관관계 검증")
        void validateSystemInfo_ShouldHaveCorrectAssociation() {
            // Given
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // When & Then
            assertThat(metrics.getSystemInfo()).isNotNull();
            assertThat(metrics.getSystemInfo().getHostname()).isNotNull();
            assertThat(metrics.getSystemInfo().getOsName()).isNotNull();
            assertThat(metrics.getSystemInfo().getJavaVersion()).isNotNull();
            assertThat(metrics.getSystemInfo().getAvailableProcessors()).isPositive();
        }

        @Test
        @DisplayName("수집 시간 검증")
        void validateCollectedAt_ShouldBeRecent() {
            // Given
            SystemMetrics metrics = TestDataBuilder.systemMetrics();

            // When & Then
            assertThat(metrics.getCollectedAt()).isNotNull();
            assertThat(metrics.getCollectedAt()).isBeforeOrEqualTo(LocalDateTime.now());
        }
    }
}
