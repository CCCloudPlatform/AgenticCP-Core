package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * NoSQL 테이블 업데이트 도메인 커맨드 (CSP 중립적)
 *
 * 테이블의 용량, 인덱스, 옵션, 태그 등을 변경할 때 사용합니다.
 * 실제로 어떤 필드가 지원되는지는 CSP 어댑터에서 판단합니다.
 */
@Getter
@Builder
public class NoSqlUpdateTableCommand {

    private final CloudProvider.ProviderType providerType;
    private final String accountScope;

    /**
     * 대상 테이블 이름
     */
    private final String tableName;

    /**
     * 리전/위치 (선택, null 이면 기본 또는 기존 리전 사용)
     */
    private final String region;

    /**
     * 결제/용량 모드 변경 (선택)
     */
    private final NoSqlCreateTableCommand.BillingMode billingMode;

    /**
     * 용량 조정 (선택)
     */
    private final Long readCapacityUnits;
    private final Long writeCapacityUnits;

    /**
     * 인덱스 추가/수정/삭제 정의
     *
     * 벤더 중립 표현만 유지하고, 실제 diff 계산/적용은 어댑터에서 처리합니다.
     */
    private final List<Map<String, Object>> globalSecondaryIndexes;

    /**
     * 테이블 옵션(TTL 등) 변경
     */
    private final Map<String, Object> tableOptions;

    /**
     * 스트림 설정 변경
     */
    private final Map<String, Object> streamOptions;

    /**
     * 태그 변경
     * - null: 변경 없음
     * - 빈 Map: 모든 태그 제거
     * - 값 존재: 태그 덮어쓰기 또는 CSP 정책에 따름
     */
    private final Map<String, String> tags;

    /**
     * 세션 자격증명
     */
    private final CloudSessionCredential session;
}


