package com.agenticcp.core.domain.cloud.port.model.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * NoSQL 테이블 태그 관리 도메인 커맨드
 */
@Getter
@Builder
public class NoSqlTagCommand {

    public enum OperationType {
        ADD_OR_UPDATE,
        REMOVE,
        REPLACE
    }

    private final CloudProvider.ProviderType providerType;
    private final String accountScope;

    private final String tableName;
    private final String region;

    /**
     * 태그 데이터
     * - ADD_OR_UPDATE: 추가/업데이트할 태그
     * - REMOVE: 제거할 태그 키만 사용 (값은 무시 가능)
     * - REPLACE: 전체 교체용 태그 세트
     */
    private final Map<String, String> tags;

    private final OperationType operationType;

    private final CloudSessionCredential session;
}


