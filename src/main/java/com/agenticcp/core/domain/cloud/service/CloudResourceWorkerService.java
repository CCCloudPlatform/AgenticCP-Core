package com.agenticcp.core.domain.cloud.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.entity.CloudResourceWorkerMap;
import com.agenticcp.core.domain.cloud.repository.CloudResourceRepository;
import com.agenticcp.core.domain.cloud.repository.CloudResourceWorkerMapRepository;
import com.agenticcp.core.domain.organization.entity.Worker;
import com.agenticcp.core.domain.organization.enums.WorkerErrorCode;
import com.agenticcp.core.domain.organization.repository.WorkerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CloudResourceWorkerService
 * 
 * <p>클라우드 리소스와 Worker 간의 관계를 관리하는 서비스입니다.
 * 설계 C 기준: 리소스 단위로 Worker 접근 권한을 관리합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-12-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CloudResourceWorkerService {
    
    private final CloudResourceWorkerMapRepository cloudResourceWorkerMapRepository;
    private final WorkerRepository workerRepository;
    private final CloudResourceRepository cloudResourceRepository;
    
    /**
     * 클라우드 리소스에 Worker 할당
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     * @return 생성된 CloudResourceWorkerMap
     * @throws BusinessException 리소스, Worker를 찾을 수 없거나 이미 할당된 경우
     */
    @Transactional
    public CloudResourceWorkerMap assignWorkerToResource(Long resourceId, Long workerId) {
        log.info("[CloudResourceWorkerService] assignWorkerToResource - resourceId={}, workerId={}", 
                resourceId, workerId);
        
        // 클라우드 리소스 존재 확인
        CloudResource resource = cloudResourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        com.agenticcp.core.domain.cloud.exception.CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        
        // Worker 존재 확인
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
        
        // 이미 할당되어 있는지 확인
        if (cloudResourceWorkerMapRepository.existsByResourceIdAndWorkerId(resourceId, workerId)) {
            throw new BusinessException(WorkerErrorCode.CLOUD_RESOURCE_WORKER_MAP_ALREADY_EXISTS, 
                    "이미 할당된 Worker입니다.");
        }
        
        // CloudResourceWorkerMap 생성
        CloudResourceWorkerMap map = CloudResourceWorkerMap.builder()
                .cloudResource(resource)
                .worker(worker)
                .isDeleted(false)
                .build();
        
        CloudResourceWorkerMap savedMap = cloudResourceWorkerMapRepository.save(map);
        
        log.info("[CloudResourceWorkerService] assignWorkerToResource - success resourceId={}, workerId={}", 
                resourceId, workerId);
        
        return savedMap;
    }
    
    /**
     * 클라우드 리소스에서 Worker 제거
     * 
     * @param resourceId 클라우드 리소스 ID
     * @param workerId Worker ID
     * @throws BusinessException CloudResourceWorkerMap을 찾을 수 없는 경우
     */
    @Transactional
    public void removeWorkerFromResource(Long resourceId, Long workerId) {
        log.info("[CloudResourceWorkerService] removeWorkerFromResource - resourceId={}, workerId={}", 
                resourceId, workerId);
        
        CloudResourceWorkerMap map = cloudResourceWorkerMapRepository
                .findByResourceIdAndWorkerId(resourceId, workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.CLOUD_RESOURCE_WORKER_MAP_NOT_FOUND));
        
        cloudResourceWorkerMapRepository.deleteByResourceIdAndWorkerId(resourceId, workerId);
        
        log.info("[CloudResourceWorkerService] removeWorkerFromResource - success resourceId={}, workerId={}", 
                resourceId, workerId);
    }
    
    /**
     * 클라우드 리소스의 Worker 목록 조회
     * 
     * @param resourceId 클라우드 리소스 ID
     * @return CloudResourceWorkerMap 목록
     */
    public List<CloudResourceWorkerMap> findByResourceId(Long resourceId) {
        log.info("[CloudResourceWorkerService] findByResourceId - resourceId={}", resourceId);
        return cloudResourceWorkerMapRepository.findByResourceId(resourceId);
    }
    
    /**
     * Worker가 접근 가능한 리소스 목록 조회
     * 
     * @param workerId Worker ID
     * @return CloudResourceWorkerMap 목록
     */
    public List<CloudResourceWorkerMap> findByWorkerId(Long workerId) {
        log.info("[CloudResourceWorkerService] findByWorkerId - workerId={}", workerId);
        return cloudResourceWorkerMapRepository.findByWorkerId(workerId);
    }
    
    /**
     * Worker가 특정 리소스에 접근 가능한지 확인
     * 
     * @param workerId Worker ID
     * @param resourceId 클라우드 리소스 ID
     * @return 접근 가능 여부
     */
    public boolean hasAccessToResource(Long workerId, Long resourceId) {
        log.info("[CloudResourceWorkerService] hasAccessToResource - workerId={}, resourceId={}", 
                workerId, resourceId);
        
        boolean hasAccess = cloudResourceWorkerMapRepository.existsByResourceIdAndWorkerId(resourceId, workerId);
        
        log.info("[CloudResourceWorkerService] hasAccessToResource - result={}, workerId={}, resourceId={}", 
                hasAccess, workerId, resourceId);
        
        return hasAccess;
    }
    
    /**
     * 테넌트의 모든 리소스에 Worker 할당
     * (테넌트의 모든 리소스에 접근 권한 부여)
     * 
     * @param tenantId 테넌트 ID
     * @param workerId Worker ID
     * @return 생성된 CloudResourceWorkerMap 목록
     */
    @Transactional
    public List<CloudResourceWorkerMap> assignWorkerToTenantResources(Long tenantId, Long workerId) {
        log.info("[CloudResourceWorkerService] assignWorkerToTenantResources - tenantId={}, workerId={}", 
                tenantId, workerId);
        
        // Worker 존재 확인
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new BusinessException(WorkerErrorCode.WORKER_NOT_FOUND));
        
        // 테넌트의 모든 리소스 조회
        List<CloudResource> resources = cloudResourceRepository.findAll().stream()
                .filter(resource -> resource.getTenant() != null && resource.getTenant().getId().equals(tenantId))
                .filter(resource -> !resource.getIsDeleted())
                .toList();
        
        // 각 리소스에 Worker 할당
        return resources.stream()
                .filter(resource -> !cloudResourceWorkerMapRepository.existsByResourceIdAndWorkerId(resource.getId(), workerId))
                .map(resource -> {
                    CloudResourceWorkerMap map = CloudResourceWorkerMap.builder()
                            .cloudResource(resource)
                            .worker(worker)
                            .isDeleted(false)
                            .build();
                    return cloudResourceWorkerMapRepository.save(map);
                })
                .toList();
    }
}

