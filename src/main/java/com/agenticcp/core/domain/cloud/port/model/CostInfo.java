package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class CostInfo {
    String currency;
    BigDecimal hourlyRate;
    BigDecimal usageAmount;
    String usageUnit;
    Instant periodStart;
    Instant periodEnd;
    Map<String, Object> attributions; // e.g., service/category/labels
}
