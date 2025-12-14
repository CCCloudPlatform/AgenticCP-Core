package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.entity.WorkerRole;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
import com.agenticcp.core.domain.organization.repository.WorkerRoleRepository;
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
 * WorkerRoleService
 * 
 * <p>Worker에게 역할을 부여하고 관리하는 서비스입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkerRoleService {
    
    private final WorkerRoleRepository workerRoleRepository;
    private final WorkerRepository workerRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    
    /**
     * Worker에게 역할 부여
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     * @param tenantId 테넌트 ID
     * @return 생성된 WorkerRole
     * @throws BusinessException Worker, Role, Tenant를 찾을 수 없거나 이미 부여된 역할인 경우
     */
    @Transactional
    public WorkerRole assignRole(Long workerId, Long roleId, Long tenantId) {
        log.info("[WorkerRoleService] assignRole - workerId={}, roleId={}, tenantId={}", 
                workerId, roleId, tenantId);
        
        // Worker 존재 확인
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
        
        // Role 존재 확인
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(
                        com.agenticcp.core.domain.user.enums.RoleErrorCode.ROLE_NOT_FOUND));
        
        // Tenant 존재 확인
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.TENANT_NOT_FOUND));
        
        // 이미 부여된 역할인지 확인
        if (workerRoleRepository.existsByWorkerIdAndTenantIdAndRoleId(workerId, tenantId, roleId)) {
            throw new BusinessException(WorkerErrorCode.WORKER_ROLE_ALREADY_EXISTS, 
                    "이미 부여된 역할입니다.");
        }
        
        // WorkerRole 생성
        WorkerRole workerRole = WorkerRole.builder()
                .worker(worker)
                .role(role)
                .tenant(tenant)
                .build();
        
        WorkerRole savedWorkerRole = workerRoleRepository.save(workerRole);
        
        log.info("[WorkerRoleService] assignRole - success workerId={}, roleId={}, tenantId={}", 
                workerId, roleId, tenantId);
        
        return savedWorkerRole;
    }
    
    /**
     * Worker에서 역할 제거
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     * @param tenantId 테넌트 ID
     * @throws BusinessException WorkerRole을 찾을 수 없는 경우
     */
    @Transactional
    public void removeRole(Long workerId, Long roleId, Long tenantId) {
        log.info("[WorkerRoleService] removeRole - workerId={}, roleId={}, tenantId={}", 
                workerId, roleId, tenantId);
        
        WorkerRole workerRole = workerRoleRepository
                .findByWorkerIdAndTenantIdAndRoleId(workerId, tenantId, roleId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_ROLE_NOT_FOUND));
        
        workerRoleRepository.delete(workerRole);
        
        log.info("[WorkerRoleService] removeRole - success workerId={}, roleId={}, tenantId={}", 
                workerId, roleId, tenantId);
    }
    
    /**
     * Worker ID로 WorkerRole 목록 조회
     * 
     * @param workerId Worker ID
     * @return WorkerRole 목록
     */
    public List<WorkerRole> findByWorkerId(Long workerId) {
        log.info("[WorkerRoleService] findByWorkerId - workerId={}", workerId);
        return workerRoleRepository.findByWorkerId(workerId);
    }
    
    /**
     * Worker ID와 테넌트 ID로 WorkerRole 목록 조회
     * 
     * @param workerId Worker ID
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    public List<WorkerRole> findByWorkerIdAndTenantId(Long workerId, Long tenantId) {
        log.info("[WorkerRoleService] findByWorkerIdAndTenantId - workerId={}, tenantId={}", 
                workerId, tenantId);
        return workerRoleRepository.findByWorkerIdAndTenantId(workerId, tenantId);
    }
    
    /**
     * 테넌트 ID로 WorkerRole 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return WorkerRole 목록
     */
    public List<WorkerRole> findByTenantId(Long tenantId) {
        log.info("[WorkerRoleService] findByTenantId - tenantId={}", tenantId);
        return workerRoleRepository.findByTenantId(tenantId);
    }
}

