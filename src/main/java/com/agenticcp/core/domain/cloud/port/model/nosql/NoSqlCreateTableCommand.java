package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * NoSQL 테이블 생성 도메인 커맨드 (CSP 중립적)
 *
 * VM / Object Storage와 동일하게, 헥사고날 아키텍처의 포트 계층에서
 * 어댑터로 전달되는 벤더 중립 명령 모델입니다.
 *
 * 각 CSP 어댑터의 Mapper에서 DynamoDB / Firestore / Cosmos DB 등
 * 구체적인 요청 객체로 변환합니다.
 */
@Getter
@Builder
public class NoSqlCreateTableCommand {

    /**
     * 클라우드 프로바이더 타입 (AWS, GCP, AZURE 등)
     */
    private final CloudProvider.ProviderType providerType;

    /**
     * 계정 스코프 (계정 ID, logical account key 등)
     */
    private final String accountScope;

    /**
     * 테이블 논리 이름
     */
    private final String tableName;

    /**
     * 리전/위치 (예: ap-northeast-2, europe-west1 등)
     */
    private final String region;

    /**
     * 파티션 키 이름
     */
    private final String partitionKeyName;

    /**
     * 정렬 키 이름 (선택)
     */
    private final String sortKeyName;

    /**
     * 파티션/정렬 키 타입 정보 (예: { "pk": "STRING", "sk": "NUMBER" })
     */
    private final Map<String, String> keyTypes;

    /**
     * 결제/용량 모드
     * - PROVISIONED: RCU/WCU 기반 프로비저닝
     * - PAY_PER_REQUEST: 온디맨드
     */
    private final BillingMode billingMode;

    /**
     * 프로비저닝 모드에서의 읽기 용량 단위 (RCU)
     */
    private final Long readCapacityUnits;

    /**
     * 프로비저닝 모드에서의 쓰기 용량 단위 (WCU)
     */
    private final Long writeCapacityUnits;

    /**
     * GSI/보조 인덱스 정의
     *
     * 구조 예:
     *  [
     *    {
     *      "indexName": "...",
     *      "partitionKey": "...",
     *      "sortKey": "...",
     *      "projectionType": "ALL|KEYS_ONLY|INCLUDE",
     *      "nonKeyAttributes": ["col1", "col2"],
     *      "readCapacityUnits": 5,
     *      "writeCapacityUnits": 5
     *    }
     *  ]
     */
    private final List<Map<String, Object>> globalSecondaryIndexes;

    /**
     * TTL 설정 등 테이블 레벨 옵션
     *
     * 예:
     *  {
     *    "ttlAttribute": "expireAt",
     *    "ttlEnabled": true
     *  }
     */
    private final Map<String, Object> tableOptions;

    /**
     * 스트림 설정 (벤더 중립)
     *
     * 예:
     *  {
     *    "enabled": true,
     *    "viewType": "NEW_IMAGE|OLD_IMAGE|NEW_AND_OLD_IMAGES|KEYS_ONLY"
     *  }
     */
    private final Map<String, Object> streamOptions;

    /**
     * 태그/라벨
     */
    private final Map<String, String> tags;

    /**
     * 세션 자격증명 (JIT 세션 관리 패턴)
     */
    private final CloudSessionCredential session;

    /**
     * NoSQL 결제 모드
     */
    public enum BillingMode {
        PROVISIONED,
        PAY_PER_REQUEST
    }
}


