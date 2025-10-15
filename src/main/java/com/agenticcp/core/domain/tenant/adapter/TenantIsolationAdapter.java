package com.agenticcp.core.domain.tenant.adapter;

import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.adapter.dto.IsolationResult;
import com.agenticcp.core.domain.tenant.adapter.dto.IsolationStatus;

/**
 * 테넌트 격리 정책을 적용하는 핵심 인터페이스
 * Adapter 패턴의 Target Interface 역할
 */
public interface TenantIsolationAdapter {
    
    /**
     * 테넌트에 격리 정책을 적용합니다.
     * 
     * @param tenantKey 테넌트 키
     * @param isolation 격리 정책 정보
     * @return 격리 정책 적용 결과
     */
    IsolationResult applyIsolationPolicy(String tenantKey, TenantIsolation isolation);
    
    /**
     * 테넌트의 격리 정책을 제거합니다.
     * 
     * @param tenantKey 테넌트 키
     */
    void removeIsolationPolicy(String tenantKey);
    
    /**
     * 테넌트의 현재 격리 상태를 조회합니다.
     * 
     * @param tenantKey 테넌트 키
     * @return 격리 상태 정보
     */
    IsolationStatus getIsolationStatus(String tenantKey);
    
    /**
     * 특정 격리 레벨을 지원하는지 확인합니다.
     * 
     * @param isolationLevel 격리 레벨
     * @return 지원 여부
     */
    boolean supportsIsolationLevel(TenantIsolation.IsolationLevel isolationLevel);
    
    /**
     * 이 Adapter가 지원하는 클라우드 프로바이더 타입을 반환합니다.
     * 
     * @return 클라우드 프로바이더 타입
     */
    String getSupportedCloudProvider();
}