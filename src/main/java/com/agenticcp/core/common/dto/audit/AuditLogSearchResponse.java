package com.agenticcp.core.common.dto.audit;

import java.util.List;

/**
 * 감사 로그 검색 응답 DTO입니다.
 * 페이지 정보와 결과 목록을 포함해 REST 표준 응답으로 래핑됩니다.
 *
 * @author AgenticCP Team
 * @since 2025-10-01
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
