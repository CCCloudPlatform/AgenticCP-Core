package com.agenticcp.core.common.entity;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * 테넌트 인식 엔티티 리스너
 * 
 * <p>
 * JPA 엔티티 생명주기 콜백을 통해 TenantAwareEntity를 상속받은 엔티티에 대해 
 * 자동으로 현재 테넌트 정보를 주입합니다.
 * </p>
 * 
 * <p>
 * 멀티 테넌시 환경에서 데이터 격리를 보장하기 위해 엔티티 저장/수정 시 
 * 반드시 테넌트 컨텍스트가 설정되어 있어야 합니다.
 * </p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@Slf4j
public class TenantAwareEntityListener {

    /**
     * 엔티티 저장 전에 테넌트 정보를 자동으로 설정합니다.
     * 
     * <p>
     * 엔티티에 테넌트 정보가 설정되지 않은 경우, 
     * TenantContextHolder에서 현재 테넌트 정보를 가져와 자동으로 설정합니다.
     * </p>
     * 
     * @param entity 저장할 엔티티 (TenantAwareEntity를 상속받은 객체)
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof TenantAwareEntity tenantAwareEntity) {
            setTenantIfNotSet(tenantAwareEntity, "prePersist");
        }
    }

    /**
     * 엔티티 수정 전에 테넌트 정보를 검증합니다.
     * 
     * <p>
     * 엔티티에 테넌트 정보가 설정되지 않은 경우, 
     * TenantContextHolder에서 현재 테넌트 정보를 가져와 자동으로 설정합니다.
     * (일반적으로 수정 시에는 이미 테넌트가 설정되어 있어야 합니다)
     * </p>
     * 
     * @param entity 수정할 엔티티 (TenantAwareEntity를 상속받은 객체)
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof TenantAwareEntity tenantAwareEntity) {
            setTenantIfNotSet(tenantAwareEntity, "preUpdate");
        }
    }

    /**
     * 엔티티에 테넌트 정보가 설정되지 않은 경우 현재 컨텍스트의 테넌트 정보를 설정합니다.
     * 
     * <p>
     * 이미 테넌트가 설정된 경우 건너뛰며, 테넌트 컨텍스트가 설정되지 않은 경우 
     * BusinessException을 발생시킵니다.
     * </p>
     * 
     * @param tenantAwareEntity 테넌트 정보를 설정할 엔티티
     * @param operation 수행 중인 작업 (prePersist 또는 preUpdate, 로깅용)
     * @throws BusinessException 테넌트 컨텍스트가 설정되지 않은 경우 (TENANT_CONTEXT_NOT_SET)
     */
    private void setTenantIfNotSet(TenantAwareEntity tenantAwareEntity, String operation) {
        // 이미 테넌트가 설정되어 있으면 건너뛰기
        if (tenantAwareEntity.getTenant() != null) {
            log.debug("테넌트가 이미 설정됨: entity={}, operation={}", 
                tenantAwareEntity.getClass().getSimpleName(), operation);
            return;
        }

        try {
            // 현재 테넌트 컨텍스트에서 테넌트 정보 조회
            Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
            
            // 테넌트 정보 설정
            tenantAwareEntity.setTenant(currentTenant);
            
            log.debug("테넌트 자동 설정 완료: tenantKey={}, entity={}, operation={}", 
                currentTenant.getTenantKey(), 
                tenantAwareEntity.getClass().getSimpleName(), 
                operation);
                
        } catch (BusinessException e) {
            // 테넌트 컨텍스트가 설정되지 않은 경우
            log.error("테넌트 설정 실패: entity={}, operation={}, error={}", 
                tenantAwareEntity.getClass().getSimpleName(), 
                operation, 
                e.getMessage());
            
            // 테넌트 컨텍스트가 필수인 경우 예외 발생
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "Tenant context is required for entity operations");
        }
    }
}
