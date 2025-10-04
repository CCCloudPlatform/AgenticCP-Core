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
 * 엔티티 생성/수정 시 자동으로 현재 테넌트 정보를 주입
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-01
 */
@Slf4j
public class TenantAwareEntityListener {

    /**
     * 엔티티 저장 전에 테넌트 정보를 자동으로 설정
     * 
     * @param entity 저장할 엔티티 (BaseEntity를 상속받은 객체)
     */
    @PrePersist
    public void prePersist(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            setTenantIfNotSet(baseEntity, "prePersist");
        }
    }

    /**
     * 엔티티 수정 전에 테넌트 정보를 자동으로 설정
     * 
     * @param entity 수정할 엔티티 (BaseEntity를 상속받은 객체)
     */
    @PreUpdate
    public void preUpdate(Object entity) {
        if (entity instanceof BaseEntity baseEntity) {
            setTenantIfNotSet(baseEntity, "preUpdate");
        }
    }

    /**
     * 엔티티에 테넌트 정보가 설정되지 않은 경우 현재 컨텍스트의 테넌트 정보를 설정
     * 
     * @param baseEntity 테넌트 정보를 설정할 엔티티
     * @param operation 수행 중인 작업 (로깅용)
     */
    private void setTenantIfNotSet(BaseEntity baseEntity, String operation) {
        // 이미 테넌트가 설정되어 있으면 건너뛰기
        if (baseEntity.getTenant() != null) {
            log.debug("Tenant already set for entity {} in {}", baseEntity.getClass().getSimpleName(), operation);
            return;
        }

        try {
            // 현재 테넌트 컨텍스트에서 테넌트 정보 조회
            Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
            
            // 테넌트 정보 설정
            baseEntity.setTenant(currentTenant);
            
            log.debug("Tenant {} set for entity {} in {}", 
                currentTenant.getTenantKey(), 
                baseEntity.getClass().getSimpleName(), 
                operation);
                
        } catch (BusinessException e) {
            // 테넌트 컨텍스트가 설정되지 않은 경우
            log.error("Failed to set tenant for entity {} in {}: {}", 
                baseEntity.getClass().getSimpleName(), 
                operation, 
                e.getMessage());
            
            // 테넌트 컨텍스트가 필수인 경우 예외 발생
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET, 
                "Tenant context is required for entity operations");
        }
    }
}
