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
 */
@Data
@Builder
public class ObjectStorageContainerQuery {

    /**
     * 프로바이더 / 계정 스코프
     */
    private CloudProvider.ProviderType providerType;
    private String accountScope;

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
    public static ObjectStorageContainerQuery defaultQuery() {
        return ObjectStorageContainerQuery.builder()
                .page(0)
                .size(20)
                .sortBy("name")
                .sortDirection("asc")
                .build();
    }
}
