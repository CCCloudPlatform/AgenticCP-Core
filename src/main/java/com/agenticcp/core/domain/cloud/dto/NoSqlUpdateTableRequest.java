package com.agenticcp.core.domain.cloud.dto;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Map;

/**
 * NoSQL 테이블 수정 요청 DTO
 *
 * CSP 중립적인 NoSQL 테이블 수정 요청을 정의합니다.
 * 용량, 인덱스, 옵션, 태그 등을 변경할 때 사용합니다.
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
public class NoSqlUpdateTableRequest {

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
     * 대상 테이블 이름
     * Controller에서 PathVariable로 주입됩니다.
     */
    private String tableName;

    /**
     * 리전/위치 (선택)
     */
    private String region;

    /**
     * 결제/용량 모드 변경 (선택)
     */
    private NoSqlCreateTableRequest.BillingMode billingMode;

    /**
     * 읽기 용량 단위 변경 (선택)
     */
    private Long readCapacityUnits;

    /**
     * 쓰기 용량 단위 변경 (선택)
     */
    private Long writeCapacityUnits;

    /**
     * GSI 추가/수정/삭제 정의
     */
    private List<NoSqlCreateTableRequest.GlobalSecondaryIndex> globalSecondaryIndexes;

    /**
     * 테이블 옵션(TTL 등) 변경
     */
    private NoSqlCreateTableRequest.TableOptions tableOptions;

    /**
     * 스트림 설정 변경
     */
    private NoSqlCreateTableRequest.StreamOptions streamOptions;

    /**
     * 태그 변경
     * - null: 변경 없음
     * - 빈 Map: 모든 태그 제거
     * - 값 존재: 태그 덮어쓰기 또는 CSP 정책에 따름
     */
    private Map<String, String> tags;
}

