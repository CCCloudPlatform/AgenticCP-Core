package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;

public interface HealthIndicator {
    String getName();
    HealthIndicatorResult check();
}
