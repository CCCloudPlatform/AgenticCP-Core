package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SystemResourceHealthIndicator 테스트 클래스
 * 
 * 실제 시스템 리소스(CPU, 메모리, 디스크)를 수집하여 헬스체크를 수행하는 인디케이터의 기능을 테스트합니다.
 * 
 * 테스트 시나리오:
 * - 실제 시스템 리소스 수집 및 검증
 * - CPU, 메모리, 디스크 사용률이 0-100% 범위 내에 있는지 확인
 * - 시스템 상태에 따른 헬스 상태 반환 검증
 * - 시스템 리소스 상세 정보 포함 여부 확인
 * 
 * 주의사항:
 * - 실제 시스템 리소스를 수집하므로 Mock 테스트가 아님
 * - 테스트 실행 시점의 실제 시스템 상태에 따라 결과가 달라질 수 있음
 * - CPU, 메모리, 디스크 사용률은 실제 시스템 환경에 의존
 */
@ExtendWith(MockitoExtension.class)
class SystemResourceHealthIndicatorTest {

    private SystemResourceHealthIndicator systemResourceHealthIndicator;

    @BeforeEach
    void setUp() {
        systemResourceHealthIndicator = new SystemResourceHealthIndicator();
    }

    /**
     * 인디케이터 이름 테스트
     * 
     * SystemResourceHealthIndicator의 이름이 "system"으로 반환되는지 확인합니다.
     * 이는 헬스체크 시스템에서 컴포넌트를 식별하는 데 사용됩니다.
     */
    @Test
    void getName_ShouldReturnSystem() {
        // When
        String name = systemResourceHealthIndicator.getName();

        // Then
        assertThat(name).isEqualTo("system");
    }

    /**
     * 시스템 리소스 헬스체크 기본 테스트
     * 
     * 실제 시스템 리소스를 수집하여 헬스체크를 수행하고, 결과에 필요한 모든 정보가 포함되어 있는지 확인합니다.
     * 
     * 검증 항목:
     * - 헬스 상태 (HEALTHY, WARNING, CRITICAL 중 하나)
     * - 메시지 존재 여부
     * - 타임스탬프 존재 여부
     * - 상세 정보 포함 여부 (CPU, 메모리, 디스크 사용률)
     */
    @Test
    void check_ShouldReturnResultWithDetails() {
        // When
        HealthIndicatorResult result = systemResourceHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isIn(HealthStatus.HEALTHY, HealthStatus.WARNING, HealthStatus.CRITICAL);
        assertThat(result.getMessage()).isNotNull();
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDetails()).isNotNull();
        assertThat(result.getDetails()).containsKeys("cpuUsage", "memoryUsage", "diskUsage");
    }

    /**
     * 시스템 리소스 사용률 유효성 테스트
     * 
     * 실제 시스템에서 수집된 CPU, 메모리, 디스크 사용률이 유효한 범위(0-100%) 내에 있는지 확인합니다.
     * 
     * 검증 항목:
     * - CPU 사용률: 0.0% ~ 100.0% 범위
     * - 메모리 사용률: 0.0% ~ 100.0% 범위  
     * - 디스크 사용률: 0.0% ~ 100.0% 범위
     * 
     * 주의사항:
     * - 실제 시스템 환경에 따라 사용률이 달라질 수 있음
     * - 테스트 실행 시점의 시스템 부하에 의존
     */
    @Test
    void check_ShouldHaveValidResourceUsage() {
        // When
        HealthIndicatorResult result = systemResourceHealthIndicator.check();

        // Then
        assertThat(result.getDetails()).isNotNull();
        
        Double cpuUsage = (Double) result.getDetails().get("cpuUsage");
        Double memoryUsage = (Double) result.getDetails().get("memoryUsage");
        Double diskUsage = (Double) result.getDetails().get("diskUsage");
        
        assertThat(cpuUsage).isBetween(0.0, 100.0);
        assertThat(memoryUsage).isBetween(0.0, 100.0);
        assertThat(diskUsage).isBetween(0.0, 100.0);
    }
}
