package com.agenticcp.core.common.context;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import lombok.extern.slf4j.Slf4j;

/**
 * 테넌트 컨텍스트를 ThreadLocal로 관리하는 클래스
 * 현재 요청의 테넌트 정보를 저장하고 조회할 수 있도록 함
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
public class TenantContextHolder {
    
    private static final ThreadLocal<Tenant> TENANT_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_KEY_CONTEXT = new ThreadLocal<>();
    
    /**
     * 현재 스레드에 테넌트 정보를 설정
     * 
     * @param tenant 설정할 테넌트 객체
     */
    public static void setTenant(Tenant tenant) {
        if (tenant != null) {
            TENANT_CONTEXT.set(tenant);
            TENANT_KEY_CONTEXT.set(tenant.getTenantKey());
            log.debug("Tenant context set: {}", tenant.getTenantKey());
        }
    }
    
    /**
     * 현재 스레드에 테넌트 키를 설정
     * 
     * @param tenantKey 설정할 테넌트 키
     */
    public static void setTenantKey(String tenantKey) {
        TENANT_KEY_CONTEXT.set(tenantKey);
        log.debug("Tenant key context set: {}", tenantKey);
    }
    
    /**
     * 현재 스레드의 테넌트 정보를 조회
     * 
     * @return 현재 테넌트 객체, 없으면 null
     */
    public static Tenant getCurrentTenant() {
        return TENANT_CONTEXT.get();
    }
    
    /**
     * 현재 스레드의 테넌트 키를 조회
     * 
     * @return 현재 테넌트 키, 없으면 null
     */
    public static String getCurrentTenantKey() {
        return TENANT_KEY_CONTEXT.get();
    }
    
    /**
     * 현재 스레드의 테넌트 키를 조회 (null 체크 포함)
     * 
     * @return 현재 테넌트 키
     * @throws IllegalStateException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    public static String getCurrentTenantKeyOrThrow() {
        String tenantKey = getCurrentTenantKey();
        if (tenantKey == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return tenantKey;
    }
    
    /**
     * 현재 스레드의 테넌트 정보를 조회 (null 체크 포함)
     * 
     * @return 현재 테넌트 객체
     * @throws IllegalStateException 테넌트 컨텍스트가 설정되지 않은 경우
     */
    public static Tenant getCurrentTenantOrThrow() {
        Tenant tenant = getCurrentTenant();
        if (tenant == null) {
            throw new IllegalStateException("Tenant context is not set");
        }
        return tenant;
    }
    
    /**
     * 현재 스레드의 테넌트 컨텍스트를 초기화
     */
    public static void clear() {
        TENANT_CONTEXT.remove();
        TENANT_KEY_CONTEXT.remove();
        log.debug("Tenant context cleared");
    }
    
    /**
     * 현재 스레드에 테넌트 컨텍스트가 설정되어 있는지 확인
     * 
     * @return 테넌트 컨텍스트 설정 여부
     */
    public static boolean hasTenantContext() {
        return getCurrentTenantKey() != null;
    }
}
