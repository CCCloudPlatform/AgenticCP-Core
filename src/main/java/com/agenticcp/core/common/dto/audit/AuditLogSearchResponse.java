package com.agenticcp.core.common.dto.audit;

import java.util.List;

/**
 * 감사 로그 검색 응답 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public record AuditLogSearchResponse(
    List<AuditLogResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {}
