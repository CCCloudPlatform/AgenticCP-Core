package com.agenticcp.core.domain.cloud.port.model.storage;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Object Storage Container 조회 조건 DTO
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Data
@Builder
public class ObjectStorageContainerQueryRequest {

    /**
     * 클라우드 프로바이더 타입
     */
    private CloudProvider.ProviderType providerType;
    
    /**
     * 계정 스코프
     */
    private String accountScope;

    /**
     * 페이지 번호 (0부터 시작)
     */
    private int page;
    
    /**
     * 페이지 크기
     */
    private int size;

    /**
     * 이름 포함 필터 (부분 일치)
     */
    private String nameContains;
    
    /**
     * 태그 일치 필터 (정확히 일치하는 태그만 조회)
     */
    private Map<String, String> tagsEquals;

    /**
     * 정렬 기준 필드명
     * 예: "name", "createdAt"
     */
    private String sortBy;
    
    /**
     * 정렬 방향
     * "asc": 오름차순, "desc": 내림차순
     */
    private String sortDirection;

    /**
     * 기본값으로 페이징 설정된 조회 요청을 생성합니다.
     * 
     * 기본값:
     * - page: 0
     * - size: 20
     * - sortBy: "name"
     * - sortDirection: "asc"
     * 
     * @return 기본값이 설정된 조회 요청 객체
     */
    public static ObjectStorageContainerQueryRequest defaultQuery() {
        return ObjectStorageContainerQueryRequest.builder()
                .page(0)
                .size(20)
                .sortBy("name")
                .sortDirection("asc")
                .build();
    }
}
