package com.agenticcp.core.domain.cloud.port.outbound.cdn;

import com.agenticcp.core.domain.cloud.dto.CDNDistributionQueryRequest;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import org.springframework.data.domain.Page;

import java.util.Optional;

/**
 * CDN Distribution 조회 포트
 * 
 * CDN Distribution 조회 기능을 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface CDNDiscoveryPort {
    
    /**
     * CDN Distribution 목록을 조회합니다.
     * 
     * @param query 조회 조건 (세션, 페이징, 필터링, 태그 포함)
     * @return CloudResource 페이지 (빈 페이지 가능, null 반환 금지)
     */
    Page<CloudResource> listDistributions(CDNDistributionQueryRequest query);
    
    /**
     * 특정 CDN Distribution을 조회합니다.
     * 
     * @param distributionId Distribution ID
     * @param accountScope 조회 대상 Cloud 계정 범위
     * @return CloudResource (존재하지 않으면 Optional.empty())
     */
    Optional<CloudResource> getDistribution(String accountScope, String distributionId);
    
    /**
     * Distribution 존재 여부를 확인합니다.
     * 
     * @param accountScope 확인 대상 Cloud 계정 범위
     * @param distributionId Distribution ID
     * @return 존재 여부
     */
    boolean distributionExists(String accountScope, String distributionId);
}

