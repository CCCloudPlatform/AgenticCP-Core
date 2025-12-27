package com.agenticcp.core.common.context;

import com.agenticcp.core.common.enums.CommonErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Worker;
import com.agenticcp.core.domain.user.repository.WorkerRepository;
import com.agenticcp.core.domain.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Tenant Context Service
 * 
 * <p>User가 속한 Tenant 목록 조회 및 Tenant 선택 검증을 담당합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-01-XX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantContextService {

    private final WorkerRepository workerRepository;
    private final TenantRepository tenantRepository;

    /**
     * User가 속한 모든 Tenant 목록 조회
     * 
     * @param userId User ID
     * @return Tenant ID 목록
     */
    @Transactional(readOnly = true)
    public List<Long> getAvailableTenantIds(Long userId) {
        log.debug("Getting available tenant IDs for user: {}", userId);
        List<Long> tenantIds = workerRepository.findTenantIdsByUserId(userId);
        log.debug("Found {} tenants for user: {}", tenantIds.size(), userId);
        return tenantIds;
    }

    /**
     * User가 특정 Tenant에 속하는지 검증하고 Worker를 반환
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Worker (Optional)
     */
    @Transactional(readOnly = true)
    public Optional<Worker> validateTenantAccess(Long userId, Long tenantId) {
        log.debug("Validating tenant access: userId={}, tenantId={}", userId, tenantId);
        
        // Tenant 존재 확인
        Optional<Tenant> tenantOpt = tenantRepository.findById(tenantId);
        if (tenantOpt.isEmpty()) {
            log.warn("Tenant not found: {}", tenantId);
            return Optional.empty();
        }

        Tenant tenant = tenantOpt.get();
        if (tenant.getIsDeleted()) {
            log.warn("Tenant is deleted: {}", tenantId);
            return Optional.empty();
        }

        // User가 이 Tenant에 속하는지 확인 (Worker 존재 여부로 확인)
        Optional<Worker> workerOpt = workerRepository.findByUserIdAndTenantIdAndIsDeletedFalse(userId, tenantId);
        if (workerOpt.isEmpty()) {
            log.warn("User {} is not a member of tenant {}", userId, tenantId);
            return Optional.empty();
        }

        log.debug("Tenant access validated: userId={}, tenantId={}, workerId={}", 
            userId, tenantId, workerOpt.get().getId());
        return workerOpt;
    }

    /**
     * User가 특정 Tenant에 속하는지 검증하고 Worker를 반환 (예외 발생)
     * 
     * @param userId User ID
     * @param tenantId Tenant ID
     * @return Worker
     * @throws BusinessException User가 해당 Tenant에 속하지 않는 경우
     */
    @Transactional(readOnly = true)
    public Worker validateTenantAccessOrThrow(Long userId, Long tenantId) {
        return validateTenantAccess(userId, tenantId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN, 
                "User is not a member of this tenant"));
    }
}

