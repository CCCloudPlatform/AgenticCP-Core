package com.agenticcp.core.domain.platform.dto;

import java.time.LocalDateTime;

public record ConfigHistoryResponse(
        String action,
        String actor,
        String reason,
        String valueType,
        String prevValue,
        String newValue,
        LocalDateTime at
) {}



