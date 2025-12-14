package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.enums.UserErrorCode;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Worker 서비스
 * 
 * <p>Worker의 생성, 조회를 제공합니다.
 * 설계 B 기준: Worker는 오직 User 기반으로만 생성됩니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorkerService {
    
    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    
    /**
     * Worker 생성 (User 기반)
     * 
     * @param userId 사용자 ID
     * @param tenantId 테넌트 ID
     * @return 생성된 Worker
     * @throws BusinessException 사용자 또는 테넌트를 찾을 수 없거나 이미 존재하는 Worker인 경우
     */
    @Transactional
    public Worker createWorker(Long userId, Long tenantId) {
        log.info("[WorkerService] createWorker - userId={}, tenantId={}", userId, tenantId);
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 테넌트 존재 확인
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.TENANT_NOT_FOUND));
        
        // 이미 존재하는 Worker인지 확인
        if (workerRepository.existsByUserIdAndTenantId(userId, tenantId)) {
            throw new BusinessException(WorkerErrorCode.WORKER_DUPLICATE_USER_TENANT);
        }
        
        // Worker 생성
        Worker worker = Worker.builder()
                .user(user)
                .tenant(tenant)
                .build();
        
        Worker savedWorker = workerRepository.save(worker);
        
        log.info("[WorkerService] createWorker - success id={}, userId={}, tenantId={}", 
                savedWorker.getId(), userId, tenantId);
        
        return savedWorker;
    }
    
    /**
     * 사용자 ID로 Worker 목록 조회
     * 
     * @param userId 사용자 ID
     * @return Worker 목록
     */
    public List<Worker> findByUserId(Long userId) {
        log.info("[WorkerService] findByUserId - userId={}", userId);
        return workerRepository.findByUserId(userId);
    }
    
    /**
     * 테넌트 ID로 Worker 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return Worker 목록
     */
    public List<Worker> findByTenantId(Long tenantId) {
        log.info("[WorkerService] findByTenantId - tenantId={}", tenantId);
        return workerRepository.findByTenantId(tenantId);
    }
    
    /**
     * Worker 조회
     * 
     * @param workerId Worker ID
     * @return Worker
     * @throws BusinessException Worker를 찾을 수 없는 경우
     */
    public Worker findById(Long workerId) {
        log.info("[WorkerService] findById - workerId={}", workerId);
        return workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
    }
}

