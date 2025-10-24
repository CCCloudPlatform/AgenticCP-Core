package com.agenticcp.core.common.dto.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * 감사 로그 검색 요청 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public record AuditLogSearchRequest(
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "KST")
    Instant startDate,
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "KST")
    Instant endDate,
    
    String action,
    AuditResourceType resourceType,
    AuditSeverity severity,
    String userId,
    Boolean success,
    String targetResourceId,
    
    @NotNull
    @Min(0)
    Integer page,
    
    @NotNull
    @Min(1)
    @Max(100)
    Integer size,
    
    String sortBy,
    String sortDirection
) {
    public AuditLogSearchRequest {
        // 기본값 설정
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "timestamp";
        if (sortDirection == null) sortDirection = "desc";
    }
}
