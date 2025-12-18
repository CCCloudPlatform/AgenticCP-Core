package com.agenticcp.core.domain.cloud.port.model.cdn;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 캐시 무효화 결과
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record InvalidationResult(
    String invalidationId,
    String distributionId,
    String status,  // InProgress, Completed
    LocalDateTime createTime,
    List<String> paths
) {
}

