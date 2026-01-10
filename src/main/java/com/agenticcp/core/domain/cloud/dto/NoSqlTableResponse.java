package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * NoSQL 테이블 응답 DTO
 *
 * NoSQL 테이블 조회/생성/수정 작업의 응답을 정의합니다.
 *
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2026-01-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
public class NoSqlTableResponse {

    /**
     * 테이블 ARN 또는 고유 식별자
     */
    private String tableId;

    /**
     * 테이블 이름
     */
    private String tableName;

    /**
     * 클라우드 프로바이더 타입
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프
     */
    private String accountScope;

    /**
     * 리전
     */
    private String region;

    /**
     * 테이블 상태
     */
    private String status;

    /**
     * 결제 모드
     */
    private String billingMode;

    /**
     * 읽기 용량 단위 (RCU)
     */
    private Long readCapacityUnits;

    /**
     * 쓰기 용량 단위 (WCU)
     */
    private Long writeCapacityUnits;

    /**
     * 테이블 크기 (바이트)
     */
    private Long tableSizeBytes;

    /**
     * 아이템 수
     */
    private Long itemCount;

    /**
     * 파티션 키 정보
     */
    private KeySchema partitionKey;

    /**
     * 정렬 키 정보 (선택)
     */
    private KeySchema sortKey;

    /**
     * GSI 목록
     */
    private List<IndexInfo> globalSecondaryIndexes;

    /**
     * LSI 목록
     */
    private List<IndexInfo> localSecondaryIndexes;

    /**
     * 스트림 정보
     */
    private StreamInfo streamInfo;

    /**
     * TTL 설정
     */
    private TtlInfo ttlInfo;

    /**
     * 태그
     */
    private Map<String, String> tags;

    /**
     * 테이블 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 마지막 수정 시간
     */
    private LocalDateTime updatedAt;

    /**
     * 키 스키마 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class KeySchema {
        private String attributeName;
        private String attributeType; // STRING, NUMBER, BINARY
        private String keyType; // HASH, RANGE
    }

    /**
     * 인덱스 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class IndexInfo {
        private String indexName;
        private String indexArn;
        private String status;
        private KeySchema partitionKey;
        private KeySchema sortKey;
        private String projectionType;
        private List<String> nonKeyAttributes;
        private Long readCapacityUnits;
        private Long writeCapacityUnits;
        private Long itemCount;
        private Long indexSizeBytes;
    }

    /**
     * 스트림 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class StreamInfo {
        private Boolean enabled;
        private String streamArn;
        private String viewType;
        private String streamLabel;
    }

    /**
     * TTL 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class TtlInfo {
        private Boolean enabled;
        private String attributeName;
        private String status;
    }

    /**
     * CloudResource에서 NoSqlTableResponse로 변환
     */
    public static NoSqlTableResponse from(CloudResource resource) {
        return NoSqlTableResponse.builder()
                .tableId(resource.getResourceId())
                .tableName(resource.getResourceName())
                .providerType(resource.getProvider() != null ? resource.getProvider().getProviderType() : null)
                .region(resource.getRegion() != null ? resource.getRegion().getRegionKey() : null)
                .status(resource.getLifecycleState() != null ? resource.getLifecycleState().name() : null)
                .tags(resource.getTags())
                .createdAt(resource.getCreatedInCloud())
                .updatedAt(resource.getLastModifiedInCloud())
                .build();
    }
}

