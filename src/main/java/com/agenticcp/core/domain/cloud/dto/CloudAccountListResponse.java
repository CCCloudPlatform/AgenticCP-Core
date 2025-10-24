package com.agenticcp.core.domain.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 클라우드 계정 목록 응답 DTO
 * 
 * 페이지네이션된 계정 목록을 반환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "클라우드 계정 목록 응답")
public class CloudAccountListResponse {
    
    @Schema(description = "계정 목록")
    private List<CloudAccountDto> accounts;
    
    @Schema(description = "전체 개수", example = "15")
    private long totalCount;
    
    @Schema(description = "현재 페이지", example = "0")
    private int currentPage;
    
    @Schema(description = "페이지 크기", example = "10")
    private int pageSize;
    
    @Schema(description = "전체 페이지 수", example = "2")
    private int totalPages;
}

