package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import com.agenticcp.core.domain.platform.entity.PlatformHealth.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ApplicationHealthIndicatorTest {

    private ApplicationHealthIndicator applicationHealthIndicator;

    @BeforeEach
    void setUp() {
        applicationHealthIndicator = new ApplicationHealthIndicator();
    }

    @Test
    void getName_ShouldReturnApplication() {
        // When
        String name = applicationHealthIndicator.getName();

        // Then
        assertThat(name).isEqualTo("application");
    }

    @Test
    void check_ShouldReturnResultWithDetails() {
        // When
        HealthIndicatorResult result = applicationHealthIndicator.check();

        // Then
        assertThat(result.getStatus()).isIn(HealthStatus.HEALTHY, HealthStatus.WARNING, HealthStatus.CRITICAL);
        assertThat(result.getMessage()).isNotNull();
        assertThat(result.getTimestamp()).isNotNull();
        assertThat(result.getDetails()).isNotNull();
        assertThat(result.getDetails()).containsKeys("totalMemory", "usedMemory", "freeMemory", "threadCount", "uptime");
    }

    @Test
    void check_ShouldHaveValidMemoryUsage() {
        // When
        HealthIndicatorResult result = applicationHealthIndicator.check();

        // Then
        assertThat(result.getDetails()).isNotNull();
        
        Long totalMemory = (Long) result.getDetails().get("totalMemory");
        Long usedMemory = (Long) result.getDetails().get("usedMemory");
        Long freeMemory = (Long) result.getDetails().get("freeMemory");
        Integer threadCount = (Integer) result.getDetails().get("threadCount");
        Long uptime = (Long) result.getDetails().get("uptime");
        
        assertThat(totalMemory).isGreaterThan(0);
        assertThat(usedMemory).isGreaterThanOrEqualTo(0);
        assertThat(freeMemory).isGreaterThanOrEqualTo(0);
        assertThat(threadCount).isGreaterThan(0);
        assertThat(uptime).isGreaterThan(0);
    }
}
