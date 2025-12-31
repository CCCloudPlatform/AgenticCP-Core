package com.agenticcp.core.domain.cloud.port.outbound.cdn;

import com.agenticcp.core.domain.cloud.port.model.cdn.CreateInvalidationCommand;
import com.agenticcp.core.domain.cloud.port.model.cdn.InvalidationResult;

import java.util.Optional;

/**
 * CDN 캐시 무효화 포트
 * 
 * CDN Distribution의 캐시 무효화 생성 및 조회 기능을 정의합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface CDNInvalidationPort {
    
    /**
     * 캐시 무효화를 생성합니다.
     * 
     * @param command 무효화 생성 명령 (세션, Distribution ID, 경로 목록 포함)
     * @return 생성된 무효화 결과 (무효화 ID, 상태 포함)
     */
    InvalidationResult createInvalidation(CreateInvalidationCommand command);
    
    /**
     * 캐시 무효화 상태를 조회합니다.
     * 
     * @param accountScope 조회 대상 Cloud 계정 범위
     * @param distributionId Distribution ID
     * @param invalidationId 무효화 ID
     * @return 무효화 결과 (존재하지 않으면 Optional.empty())
     */
    Optional<InvalidationResult> getInvalidation(
        String accountScope, 
        String distributionId, 
        String invalidationId
    );
}

