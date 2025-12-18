package com.agenticcp.core.domain.cloud.port.outbound.nosql;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;
import com.agenticcp.core.domain.cloud.port.model.ResourceQuery;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * NoSQL 테이블 조회(Discovery) 포트
 *
 * 공통 리소스 조회용 ResourceDiscoveryPort를 보완하여,
 * NoSQL 테이블 도메인에 특화된 조회 기능을 제공합니다.
 */
public interface NoSqlDiscoveryPort {

    /**
     * NoSQL 테이블 목록을 조회합니다.
     *
     * @param query 조회 조건 (Provider, AccountScope, Region, ResourceType 등)
     * @return CloudResource 페이지 (빈 페이지 가능, null 금지)
     */
    Page<CloudResource> listTables(ResourceQuery query);

    /**
     * 특정 NoSQL 테이블을 조회합니다.
     *
     * @param id 리소스 식별 정보
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getTable(ResourceIdentity id);
}


