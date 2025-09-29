package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.util.LogMaskingUtils;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.user.enums.PermissionErrorCode;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.dto.CreatePermissionRequest;
import com.agenticcp.core.domain.user.dto.PermissionResponse;
import com.agenticcp.core.domain.user.dto.UpdatePermissionRequest;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 권한 관리 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {
    
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    
    /**
     * 모든 권한 조회 (현재 테넌트)
     * 
     * @return 권한 목록
     */
    public List<Permission> getAllPermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getAllPermissions - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenant(currentTenant);
        log.info("[PermissionService] getAllPermissions - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 테넌트별 권한 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByTenant(String tenantKey) {
        log.info("[PermissionService] getPermissionsByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        List<Permission> result = permissionRepository.findByTenantKey(tenantKey);
        log.info("[PermissionService] getPermissionsByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenantKey));
        return result;
    }
    
    /**
     * 권한 키로 권한 조회
     * 
     * @param permissionKey 권한 키
     * @return 권한 정보
     */
    public Optional<Permission> getPermissionByKey(String permissionKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getPermissionByKey - permissionKey={} tenantKey={}",
                LogMaskingUtils.maskPermissionKey(permissionKey),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        Optional<Permission> result = permissionRepository.findByPermissionKeyAndTenant(permissionKey, currentTenant);
        log.info("[PermissionService] getPermissionByKey - found={} permissionKey={} tenantKey={}",
                result.isPresent(),
                LogMaskingUtils.maskPermissionKey(permissionKey),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 권한 키로 권한 조회 (예외 발생)
     * 
     * @param permissionKey 권한 키
     * @return 권한 정보
     * @throws ResourceNotFoundException 권한을 찾을 수 없는 경우
     */
    public Permission getPermissionByKeyOrThrow(String permissionKey) {
        log.info("[PermissionService] getPermissionByKeyOrThrow - permissionKey={}", LogMaskingUtils.maskPermissionKey(permissionKey));
        Permission permission = getPermissionByKey(permissionKey)
                .orElseThrow(() -> new ResourceNotFoundException(PermissionErrorCode.PERMISSION_NOT_FOUND));
        log.info("[PermissionService] getPermissionByKeyOrThrow - success permissionKey={}", LogMaskingUtils.maskPermissionKey(permissionKey));
        return permission;
    }
    
    /**
     * 활성 권한 조회
     * 
     * @return 활성 권한 목록
     */
    public List<Permission> getActivePermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getActivePermissions - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findActivePermissionsByTenant(currentTenant, Status.ACTIVE);
        log.info("[PermissionService] getActivePermissions - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 시스템 권한 조회
     * 
     * @return 시스템 권한 목록
     */
    public List<Permission> getSystemPermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getSystemPermissions - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getIsSystem()))
                .collect(Collectors.toList());
        log.info("[PermissionService] getSystemPermissions - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 카테고리별 권한 조회
     * 
     * @param category 카테고리
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByCategory(String category) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getPermissionsByCategory - category={} tenantKey={}", category, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenantAndCategory(currentTenant, category);
        log.info("[PermissionService] getPermissionsByCategory - success count={} category={} tenantKey={}", result.size(), category, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 리소스별 권한 조회
     * 
     * @param resource 리소스
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByResource(String resource) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getPermissionsByResource - resource={} tenantKey={}", resource, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> resource.equals(permission.getResource()))
                .collect(Collectors.toList());
        log.info("[PermissionService] getPermissionsByResource - success count={} resource={} tenantKey={}", result.size(), resource, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 액션별 권한 조회
     * 
     * @param action 액션
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByAction(String action) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getPermissionsByAction - action={} tenantKey={}", action, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> action.equals(permission.getAction()))
                .collect(Collectors.toList());
        log.info("[PermissionService] getPermissionsByAction - success count={} action={} tenantKey={}", result.size(), action, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 리소스와 액션으로 권한 조회
     * 
     * @param resource 리소스
     * @param action 액션
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByResourceAndAction(String resource, String action) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] getPermissionsByResourceAndAction - resource={} action={} tenantKey={}", resource, action, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.findByTenantAndResourceAndAction(currentTenant, resource, action);
        log.info("[PermissionService] getPermissionsByResourceAndAction - success count={} resource={} action={} tenantKey={}", result.size(), resource, action, LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 권한 검색
     * 
     * @param keyword 검색 키워드
     * @return 검색된 권한 목록
     */
    public List<Permission> searchPermissions(String keyword) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] searchPermissions - keyword={} tenantKey={}", LogMaskingUtils.mask(keyword, 2, 1), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Permission> result = permissionRepository.searchPermissionsByTenant(keyword, currentTenant);
        log.info("[PermissionService] searchPermissions - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 권한 생성
     * 
     * @param request 권한 생성 요청
     * @return 생성된 권한
     */
    @Transactional
    public Permission createPermission(CreatePermissionRequest request) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[PermissionService] createPermission - tenantKey={} permissionKey={} resource={} action={} category={} priority={}",
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()),
                LogMaskingUtils.maskPermissionKey(request.getPermissionKey()),
                request.getResource(),
                request.getAction(),
                request.getCategory(),
                request.getPriority());
        
        // 권한 키 중복 확인
        if (permissionRepository.existsByPermissionKeyAndTenant(request.getPermissionKey(), currentTenant)) {
            throw new BusinessException(PermissionErrorCode.DUPLICATE_PERMISSION_KEY, 
                    "이미 존재하는 권한 키입니다: " + request.getPermissionKey());
        }
        
        // 권한 생성
        Permission permission = Permission.builder()
                .permissionKey(request.getPermissionKey())
                .permissionName(request.getPermissionName())
                .description(request.getDescription())
                .tenant(currentTenant)
                .resource(request.getResource())
                .action(request.getAction())
                .isSystem(false)
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(Status.ACTIVE)
                .build();
        
        Permission savedPermission = permissionRepository.save(permission);
        
        
        log.info("[PermissionService] createPermission - success permissionKey={} tenantKey={}",
                LogMaskingUtils.maskPermissionKey(savedPermission.getPermissionKey()),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return savedPermission;
    }
    
    /**
     * 권한 수정
     * 
     * @param permissionKey 권한 키
     * @param request 권한 수정 요청
     * @return 수정된 권한
     */
    @Transactional
    public Permission updatePermission(String permissionKey, UpdatePermissionRequest request) {
        log.info("[PermissionService] updatePermission - permissionKey={} fields=[name:{}, desc:set?{}, resource:{}, action:{}, category:{}, priority:{}]",
                LogMaskingUtils.maskPermissionKey(permissionKey),
                request.getPermissionName(),
                request.getDescription() != null,
                request.getResource(),
                request.getAction(),
                request.getCategory(),
                request.getPriority());
        Permission permission = getPermissionByKeyOrThrow(permissionKey);
        
        // 시스템 권한 수정 제한
        if (permission.getIsSystem() && request.getIsSystem() != null && !request.getIsSystem()) {
            throw new BusinessException(PermissionErrorCode.SYSTEM_PERMISSION_NOT_MODIFIABLE);
        }
        
        // 권한 정보 업데이트
        if (request.getPermissionName() != null) {
            permission.setPermissionName(request.getPermissionName());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        if (request.getResource() != null) {
            permission.setResource(request.getResource());
        }
        if (request.getAction() != null) {
            permission.setAction(request.getAction());
        }
        if (request.getCategory() != null) {
            permission.setCategory(request.getCategory());
        }
        if (request.getPriority() != null) {
            permission.setPriority(request.getPriority());
        }
        
        Permission updatedPermission = permissionRepository.save(permission);
        
        
        log.info("[PermissionService] updatePermission - success permissionKey={} tenantKey={}",
                LogMaskingUtils.maskPermissionKey(permissionKey),
                LogMaskingUtils.maskTenantKey(permission.getTenant().getTenantKey()));
        return updatedPermission;
    }
    
    /**
     * 권한 삭제
     * 
     * @param permissionKey 권한 키
     */
    @Transactional
    public void deletePermission(String permissionKey) {
        log.info("[PermissionService] deletePermission - permissionKey={}", LogMaskingUtils.maskPermissionKey(permissionKey));
        Permission permission = getPermissionByKeyOrThrow(permissionKey);
        
        // 시스템 권한 삭제 방지
        if (permission.getIsSystem()) {
            throw new BusinessException(PermissionErrorCode.SYSTEM_PERMISSION_NOT_DELETABLE);
        }
        
        // 권한을 사용하는 역할 확인
        Long roleCount = permissionRepository.countRolesByPermission(permission);
        if (roleCount > 0) {
            throw new BusinessException(PermissionErrorCode.PERMISSION_IN_USE);
        }
        
        // 소프트 삭제
        permission.setIsDeleted(true);
        permissionRepository.save(permission);
        
        
        log.info("[PermissionService] deletePermission - success permissionKey={} tenantKey={}",
                LogMaskingUtils.maskPermissionKey(permissionKey),
                LogMaskingUtils.maskTenantKey(permission.getTenant().getTenantKey()));
    }
    
    /**
     * 권한 응답 DTO 변환
     * 
     * @param permission 권한 엔티티
     * @return 권한 응답 DTO
     */
    public PermissionResponse toPermissionResponse(Permission permission) {
        // LazyInitializationException 방지를 위해 컨텍스트의 테넌트 정보를 사용
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return PermissionResponse.builder()
                .id(permission.getId())
                .permissionKey(permission.getPermissionKey())
                .permissionName(permission.getPermissionName())
                .description(permission.getDescription())
                .tenantKey(currentTenant.getTenantKey())
                .tenantName(currentTenant.getTenantName())
                .status(permission.getStatus())
                .resource(permission.getResource())
                .action(permission.getAction())
                .isSystem(permission.getIsSystem())
                .category(permission.getCategory())
                .priority(permission.getPriority())
                .createdAt(permission.getCreatedAt())
                .updatedAt(permission.getUpdatedAt())
                .createdBy(permission.getCreatedBy())
                .updatedBy(permission.getUpdatedBy())
                .build();
    }
    
}
