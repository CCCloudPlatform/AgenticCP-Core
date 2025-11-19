package com.agenticcp.core.domain.cloud.port.outbound.account;

import com.agenticcp.core.domain.cloud.entity.CloudAccount;

/**
 * 클라우드 계정 정보 동기화를 위한 포트
 * 
 * 클라우드 프로바이더로부터 최신 계정 정보를 가져와 
 * 로컬 데이터베이스와 동기화하는 기능을 제공합니다.
 * 
 * 이 포트는 헥사고날 아키텍처의 아웃바운드 포트로,
 * 도메인 계층이 외부 클라우드 프로바이더와 통신하기 위한 인터페이스입니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public interface AccountSyncPort {
    
    /**
     * 클라우드 계정 정보를 동기화합니다.
     * 
     * 클라우드 프로바이더의 최신 정보를 조회하여 
     * 로컬 데이터베이스의 계정 정보를 업데이트합니다.
     * 
     * 동기화 항목:
     * - 계정 상태
     * - 리전 정보
     * - 메타데이터 (계정 타입, 권한 등)
     * - 마지막 동기화 시간
     * 
     * @param accountId 동기화할 계정 ID
     * @return 동기화된 계정 정보
     * @throws com.agenticcp.core.common.exception.BusinessException 동기화 실패 시
     *         (계정 접근 불가, 네트워크 오류 등)
     */
    CloudAccount syncAccountInfo(Long accountId);
}

