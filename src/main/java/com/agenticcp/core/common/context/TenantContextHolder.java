package com.agenticcp.core.common.context;

import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Worker;
import lombok.extern.slf4j.Slf4j;

/**
 * 테넌트 컨텍스트를 ThreadLocal로 관리하는 클래스
 * 현재 요청의 테넌트 및 Worker 정보를 저장하고 조회할 수 있도록 함
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 * @since 2025-09-22
 */
@Slf4j
public class TenantContextHolder {
    
    private static final ThreadLocal<Tenant> TENANT_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_KEY_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Worker> WORKER_CONTEXT = new ThreadLocal<>();
    
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
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET);
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
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET);
        }
        return tenant;
    }
    
    /**
     * 현재 스레드에 Tenant와 Worker를 함께 설정
     * 
     * @param tenant 설정할 테넌트 객체
     * @param worker 설정할 Worker 객체
     */
    public static void setCurrentTenantAndWorker(Tenant tenant, Worker worker) {
        if (tenant != null) {
            TENANT_CONTEXT.set(tenant);
            TENANT_KEY_CONTEXT.set(tenant.getTenantKey());
            log.debug("Tenant context set: {}", tenant.getTenantKey());
        }
        if (worker != null) {
            WORKER_CONTEXT.set(worker);
            log.debug("Worker context set: {} (userId: {}, tenantId: {})", 
                worker.getWorkerKey(), worker.getUser().getId(), worker.getTenant().getId());
        }
    }

    /**
     * 현재 스레드의 Worker 정보를 조회
     * 
     * @return 현재 Worker 객체, 없으면 null
     */
    public static Worker getCurrentWorker() {
        return WORKER_CONTEXT.get();
    }

    /**
     * 현재 스레드의 Worker 정보를 조회 (null 체크 포함)
     * 
     * @return 현재 Worker 객체
     * @throws IllegalStateException Worker 컨텍스트가 설정되지 않은 경우
     */
    public static Worker getCurrentWorkerOrThrow() {
        Worker worker = getCurrentWorker();
        if (worker == null) {
            throw new BusinessException(CommonErrorCode.TENANT_CONTEXT_NOT_SET);
        }
        return worker;
    }

    /**
     * 현재 스레드의 Tenant와 Worker를 함께 조회
     * 
     * @return Tenant와 Worker를 담은 객체 (TenantWorkerContext)
     */
    public static TenantWorkerContext getCurrentTenantAndWorker() {
        Tenant tenant = getCurrentTenant();
        Worker worker = getCurrentWorker();
        return new TenantWorkerContext(tenant, worker);
    }

    /**
     * 현재 스레드의 Tenant와 Worker를 함께 조회 (null 체크 포함)
     * 
     * @return Tenant와 Worker를 담은 객체 (TenantWorkerContext)
     * @throws IllegalStateException Tenant 또는 Worker 컨텍스트가 설정되지 않은 경우
     */
    public static TenantWorkerContext getCurrentTenantAndWorkerOrThrow() {
        Tenant tenant = getCurrentTenantOrThrow();
        Worker worker = getCurrentWorkerOrThrow();
        return new TenantWorkerContext(tenant, worker);
    }

    /**
     * 현재 스레드의 테넌트 컨텍스트를 초기화
     */
    public static void clear() {
        TENANT_CONTEXT.remove();
        TENANT_KEY_CONTEXT.remove();
        WORKER_CONTEXT.remove();
        log.debug("Tenant and Worker context cleared");
    }
    
    /**
     * 현재 스레드에 테넌트 컨텍스트가 설정되어 있는지 확인
     * 
     * @return 테넌트 컨텍스트 설정 여부
     */
    public static boolean hasTenantContext() {
        return getCurrentTenantKey() != null;
    }

    /**
     * Tenant와 Worker를 함께 담는 컨텍스트 객체
     */
    public static class TenantWorkerContext {
        private final Tenant tenant;
        private final Worker worker;

        public TenantWorkerContext(Tenant tenant, Worker worker) {
            this.tenant = tenant;
            this.worker = worker;
        }

        public Tenant getTenant() {
            return tenant;
        }

        public Worker getWorker() {
            return worker;
        }

        public Long getTenantId() {
            return tenant != null ? tenant.getId() : null;
        }

        public Long getWorkerId() {
            return worker != null ? worker.getId() : null;
        }
    }
}
