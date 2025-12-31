package com.agenticcp.core.domain.organization.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.organization.entity.Organization;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.OrganizationRepository;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
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
 * 설계 C 기준: User와 Organization 모두 Worker로 변환 가능하며, Worker는 테넌트 독립적입니다.</p>
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
    private final OrganizationRepository organizationRepository;
    
    /**
     * Worker 생성 (User 기반)
     * 
     * @param userId 사용자 ID
     * @return 생성된 Worker
     * @throws BusinessException 사용자를 찾을 수 없거나 이미 존재하는 Worker인 경우
     */
    @Transactional
    public Worker createWorkerFromUser(Long userId) {
        log.info("[WorkerService] createWorkerFromUser - userId={}", userId);
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        
        // 이미 존재하는 Worker인지 확인
        if (workerRepository.existsByUserId(userId)) {
            throw new BusinessException(WorkerErrorCode.WORKER_DUPLICATE_USER);
        }
        
        // Worker 생성 (User 기반)
        Worker worker = Worker.builder()
                .user(user)
                .organization(null)
                .build();
        
        Worker savedWorker = workerRepository.save(worker);
        
        log.info("[WorkerService] createWorkerFromUser - success id={}, userId={}", 
                savedWorker.getId(), userId);
        
        return savedWorker;
    }
    
    /**
     * Worker 생성 (Organization 기반)
     * 
     * @param organizationId 조직 ID
     * @return 생성된 Worker
     * @throws BusinessException 조직을 찾을 수 없거나 이미 존재하는 Worker인 경우
     */
    @Transactional
    public Worker createWorkerFromOrganization(Long organizationId) {
        log.info("[WorkerService] createWorkerFromOrganization - organizationId={}", organizationId);
        
        // 조직 존재 확인
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.ORGANIZATION_NOT_FOUND));
        
        // 이미 존재하는 Worker인지 확인
        if (workerRepository.existsByOrganizationId(organizationId)) {
            throw new BusinessException(WorkerErrorCode.WORKER_DUPLICATE_ORGANIZATION);
        }
        
        // Worker 생성 (Organization 기반)
        Worker worker = Worker.builder()
                .user(null)
                .organization(organization)
                .build();
        
        Worker savedWorker = workerRepository.save(worker);
        
        log.info("[WorkerService] createWorkerFromOrganization - success id={}, organizationId={}", 
                savedWorker.getId(), organizationId);
        
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
     * 조직 ID로 Worker 목록 조회
     * 
     * @param organizationId 조직 ID
     * @return Worker 목록
     */
    public List<Worker> findByOrganizationId(Long organizationId) {
        log.info("[WorkerService] findByOrganizationId - organizationId={}", organizationId);
        return workerRepository.findByOrganizationId(organizationId);
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

