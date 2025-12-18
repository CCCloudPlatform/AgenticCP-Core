package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.port.model.nosql.NoSqlTagCommand;

import java.util.Map;

/**
 * NoSQL 태그 관리 포트
 *
 * 테이블 태그/라벨의 추가, 제거, 교체 및 조회를 담당합니다.
 */
public interface NoSqlTaggingPort {

    /**
     * 태그를 추가하거나 업데이트합니다.
     */
    void putTags(NoSqlTagCommand command);

    /**
     * 태그를 제거합니다.
     */
    void removeTags(NoSqlTagCommand command);

    /**
     * 태그를 조회합니다.
     *
     * @param providerType 공급자 타입
     * @param accountScope 계정 스코프
     * @param tableName    테이블 이름
     * @param region       리전 (선택)
     * @return 태그 맵
     */
    Map<String, String> getTags(
            com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType providerType,
            String accountScope,
            String tableName,
            String region
    );
}


