package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * NoSQL 테이블 생성 요청 DTO
 *
 * CSP 중립적인 NoSQL 테이블 생성 요청을 정의합니다.
 * AWS DynamoDB, GCP Firestore, Azure Cosmos DB 등에서 공통으로 사용됩니다.
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
public class NoSqlCreateTableRequest {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프 (Account ID 등)
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String accountScope;

    /**
     * 테이블 이름
     */
    @NotBlank(message = "테이블 이름은 필수입니다")
    @Size(min = 3, max = 255, message = "테이블 이름은 3~255자 사이여야 합니다")
    private String tableName;

    /**
     * 리전/위치 (예: ap-northeast-2, europe-west1 등)
     */
    @NotBlank(message = "리전은 필수입니다")
    private String region;

    /**
     * 파티션 키 이름
     */
    @NotBlank(message = "파티션 키 이름은 필수입니다")
    private String partitionKeyName;

    /**
     * 정렬 키 이름 (선택)
     */
    private String sortKeyName;

    /**
     * 파티션/정렬 키 타입 정보
     * 예: { "pk": "STRING", "sk": "NUMBER" }
     */
    private Map<String, String> keyTypes;

    /**
     * 결제/용량 모드
     * - PROVISIONED: RCU/WCU 기반 프로비저닝
     * - PAY_PER_REQUEST: 온디맨드
     */
    @Builder.Default
    private BillingMode billingMode = BillingMode.PAY_PER_REQUEST;

    /**
     * 프로비저닝 모드에서의 읽기 용량 단위 (RCU)
     */
    private Long readCapacityUnits;

    /**
     * 프로비저닝 모드에서의 쓰기 용량 단위 (WCU)
     */
    private Long writeCapacityUnits;

    /**
     * GSI/보조 인덱스 정의
     */
    private List<GlobalSecondaryIndex> globalSecondaryIndexes;

    /**
     * TTL 설정 등 테이블 레벨 옵션
     */
    private TableOptions tableOptions;

    /**
     * 스트림 설정
     */
    private StreamOptions streamOptions;

    /**
     * 태그/라벨
     */
    private Map<String, String> tags;

    /**
     * NoSQL 결제 모드
     */
    public enum BillingMode {
        PROVISIONED,
        PAY_PER_REQUEST
    }

    /**
     * GSI 정의
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class GlobalSecondaryIndex {
        private String indexName;
        private String partitionKey;
        private String sortKey;
        private String projectionType; // ALL, KEYS_ONLY, INCLUDE
        private List<String> nonKeyAttributes;
        private Long readCapacityUnits;
        private Long writeCapacityUnits;
    }

    /**
     * 테이블 옵션
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class TableOptions {
        private String ttlAttribute;
        private Boolean ttlEnabled;
    }

    /**
     * 스트림 옵션
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Jacksonized
    public static class StreamOptions {
        private Boolean enabled;
        private String viewType; // NEW_IMAGE, OLD_IMAGE, NEW_AND_OLD_IMAGES, KEYS_ONLY
    }
}

