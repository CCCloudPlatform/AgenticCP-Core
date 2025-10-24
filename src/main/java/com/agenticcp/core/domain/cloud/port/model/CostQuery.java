package com.agenticcp.core.domain.cloud.port.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
public class CostQuery {
    ResourceIdentity identity;
    Set<String> dimensions;
    Instant from;
    Instant to;
    int page;
    int size;
}
