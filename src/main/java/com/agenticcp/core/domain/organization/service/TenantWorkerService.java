package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.TenantWorkerMap;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.TenantWorkerMapRepository;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * TenantWorkerService
 * 
 * <p>테넌트와 Worker 간의 관계를 관리하는 서비스입니다.
 * Shared Tenant에 Worker를 할당하고, 접근 권한을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TenantWorkerService {
    
    private final TenantWorkerMapRepository tenantWorkerMapRepository;
    private final WorkerRepository workerRepository;
    private final TenantRepository tenantRepository;
    private final WorkerRoleService workerRoleService;
    private final RoleRepository roleRepository;
    
    /**
     * 테넌트에 Worker 할당 (Shared Tenant용)
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     * @param accessScope 접근 범위
     * @return 생성된 TenantWorkerMap
     * @throws BusinessException 테넌트, Worker를 찾을 수 없거나 이미 할당된 경우
     */
    @Transactional
    public TenantWorkerMap assignWorkerToTenant(Long tenantId, Long workerId, String accessScope) {
        log.info("[TenantWorkerService] assignWorkerToTenant - tenantId={}, workerId={}, accessScope={}", 
                tenantId, workerId, accessScope);
        
        // 테넌트 존재 확인
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.TENANT_NOT_FOUND));
        
        // Worker 존재 확인
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
        
        // 이미 할당되어 있는지 확인
        if (tenantWorkerMapRepository.existsByTenantIdAndWorkerId(tenantId, workerId)) {
            throw new BusinessException(WorkerErrorCode.TENANT_WORKER_MAP_ALREADY_EXISTS, 
                    "이미 할당된 Worker입니다.");
        }
        
        // TenantWorkerMap 생성
        TenantWorkerMap tenantWorkerMap = TenantWorkerMap.builder()
                .tenant(tenant)
                .worker(worker)
                .accessScope(accessScope)
                .build();
        
        TenantWorkerMap savedMap = tenantWorkerMapRepository.save(tenantWorkerMap);
        
        log.info("[TenantWorkerService] assignWorkerToTenant - success tenantId={}, workerId={}", 
                tenantId, workerId);
        
        return savedMap;
    }
    
    /**
     * 테넌트에서 Worker 제거
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     * @throws BusinessException TenantWorkerMap을 찾을 수 없는 경우
     */
    @Transactional
    public void removeWorkerFromTenant(Long tenantId, Long workerId) {
        log.info("[TenantWorkerService] removeWorkerFromTenant - tenantId={}, workerId={}", 
                tenantId, workerId);
        
        TenantWorkerMap tenantWorkerMap = tenantWorkerMapRepository
                .findByTenantIdAndWorkerId(tenantId, workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.TENANT_WORKER_MAP_NOT_FOUND));
        
        tenantWorkerMapRepository.delete(tenantWorkerMap);
        
        log.info("[TenantWorkerService] removeWorkerFromTenant - success tenantId={}, workerId={}", 
                tenantId, workerId);
    }
    
    /**
     * 테넌트의 Worker 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return TenantWorkerMap 목록
     */
    public List<TenantWorkerMap> findByTenantId(Long tenantId) {
        log.info("[TenantWorkerService] findByTenantId - tenantId={}", tenantId);
        return tenantWorkerMapRepository.findByTenantId(tenantId);
    }
    
    /**
     * Worker가 속한 테넌트 목록 조회
     * 
     * @param workerId Worker ID
     * @return TenantWorkerMap 목록
     */
    public List<TenantWorkerMap> findByWorkerId(Long workerId) {
        log.info("[TenantWorkerService] findByWorkerId - workerId={}", workerId);
        return tenantWorkerMapRepository.findByWorkerId(workerId);
    }
    
    /**
     * Shared Tenant 접근 권한 검증
     * Worker 멤버십과 역할을 모두 확인합니다.
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @param roleKey 필요한 역할 키
     * @return 접근 권한 여부
     */
    public boolean hasAccessToTenant(Long userId, Long tenantId, String roleKey) {
        log.info("[TenantWorkerService] hasAccessToTenant - userId={}, tenantId={}, roleKey={}", 
                userId, tenantId, roleKey);
        
        // 1. Worker 존재 확인
        Worker worker = workerRepository.findByUserIdAndTenantId(userId, tenantId)
                .orElse(null);
        
        if (worker == null) {
            log.debug("[TenantWorkerService] hasAccessToTenant - Worker not found");
            return false;
        }
        
        // 2. Shared Tenant인 경우 TenantWorkerMap 확인
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElse(null);
        
        if (tenant == null) {
            return false;
        }
        
        if (tenant.getTenantType() == Tenant.TenantType.SHARED) {
            boolean hasMembership = tenantWorkerMapRepository
                    .existsByTenantIdAndWorkerId(tenantId, worker.getId());
            
            if (!hasMembership) {
                log.debug("[TenantWorkerService] hasAccessToTenant - No TenantWorkerMap membership");
                return false;
            }
        }
        
        // 3. 역할 확인
        List<com.agenticcp.core.domain.organization.entity.WorkerRole> workerRoles = 
                workerRoleService.findByWorkerIdAndTenantId(worker.getId(), tenantId);
        
        if (workerRoles.isEmpty()) {
            log.debug("[TenantWorkerService] hasAccessToTenant - No roles assigned");
            return false;
        }
        
        // 4. 요구된 역할 확인 (테넌트별로 조회)
        Role requiredRole = roleRepository.findByRoleKeyAndTenantWithPermissions(roleKey, tenant)
                .orElse(null);
        
        if (requiredRole == null) {
            log.debug("[TenantWorkerService] hasAccessToTenant - Required role not found for tenantId={}, roleKey={}", tenantId, roleKey);
            return false;
        }
        
        boolean hasRole = workerRoles.stream()
                .anyMatch(wr -> wr.getRole().getId().equals(requiredRole.getId()));
        
        log.info("[TenantWorkerService] hasAccessToTenant - result={}, userId={}, tenantId={}", 
                hasRole, userId, tenantId);
        
        return hasRole;
    }
}

