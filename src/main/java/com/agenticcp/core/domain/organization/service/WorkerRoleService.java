package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.entity.WorkerRole;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
import com.agenticcp.core.domain.organization.repository.WorkerRoleRepository;
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
    
    /**
     * Worker에게 역할 부여
     * 
     * @param workerId Worker ID
     * @param roleId Role ID (Role이 이미 tenant_id를 가짐)
     * @return 생성된 WorkerRole
     * @throws BusinessException Worker, Role을 찾을 수 없거나 이미 부여된 역할인 경우
     */
    @Transactional
    public WorkerRole assignRole(Long workerId, Long roleId) {
        log.info("[WorkerRoleService] assignRole - workerId={}, roleId={}", 
                workerId, roleId);
        
        // Worker 존재 확인
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
        
        // Role 존재 확인
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(
                        com.agenticcp.core.domain.user.enums.RoleErrorCode.ROLE_NOT_FOUND));
        
        // 이미 부여된 역할인지 확인
        if (workerRoleRepository.existsByWorkerIdAndRoleId(workerId, roleId)) {
            throw new BusinessException(WorkerErrorCode.WORKER_ROLE_ALREADY_EXISTS, 
                    "이미 부여된 역할입니다.");
        }
        
        // WorkerRole 생성 (tenant는 Role에서 가져옴)
        WorkerRole workerRole = WorkerRole.builder()
                .worker(worker)
                .role(role)
                .build();
        
        WorkerRole savedWorkerRole = workerRoleRepository.save(workerRole);
        
        log.info("[WorkerRoleService] assignRole - success workerId={}, roleId={}", 
                workerId, roleId);
        
        return savedWorkerRole;
    }
    
    /**
     * Worker에서 역할 제거
     * 
     * @param workerId Worker ID
     * @param roleId Role ID
     * @throws BusinessException WorkerRole을 찾을 수 없는 경우
     */
    @Transactional
    public void removeRole(Long workerId, Long roleId) {
        log.info("[WorkerRoleService] removeRole - workerId={}, roleId={}", 
                workerId, roleId);
        
        WorkerRole workerRole = workerRoleRepository
                .findByWorkerIdAndRoleId(workerId, roleId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_ROLE_NOT_FOUND));
        
        workerRoleRepository.deleteByWorkerIdAndRoleId(workerId, roleId);
        
        log.info("[WorkerRoleService] removeRole - success workerId={}, roleId={}", 
                workerId, roleId);
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
    
}

