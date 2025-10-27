package com.agenticcp.core.domain.cloud.port.model.aws;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * S3 버킷 조회 조건 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Data
@Builder
public class S3BucketQuery {

    /**
     * 페이징 정보
     */
    private int page;
    private int size;

    /**
     * 필터링 조건
     */
    private String nameContains;
    private Map<String, String> tagsEquals;

    /**
     * 정렬 조건
     */
    private String sortBy;
    private String sortDirection;

    /**
     * 기본값으로 페이징 설정
     */
    public static S3BucketQuery defaultQuery() {
        return S3BucketQuery.builder()
                .page(0)
                .size(20)
                .sortBy("name")
                .sortDirection("asc")
                .build();
    }
}
