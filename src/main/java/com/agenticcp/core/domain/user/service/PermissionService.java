package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
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
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 모든 권한 조회 (현재 테넌트)
     * 
     * @return 권한 목록
     */
    public List<Permission> getAllPermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByTenant(currentTenant);
    }
    
    /**
     * 테넌트별 권한 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByTenant(String tenantKey) {
        return permissionRepository.findByTenantKey(tenantKey);
    }
    
    /**
     * 권한 키로 권한 조회
     * 
     * @param permissionKey 권한 키
     * @return 권한 정보
     */
    public Optional<Permission> getPermissionByKey(String permissionKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByPermissionKeyAndTenant(permissionKey, currentTenant);
    }
    
    /**
     * 권한 키로 권한 조회 (예외 발생)
     * 
     * @param permissionKey 권한 키
     * @return 권한 정보
     * @throws ResourceNotFoundException 권한을 찾을 수 없는 경우
     */
    public Permission getPermissionByKeyOrThrow(String permissionKey) {
        return getPermissionByKey(permissionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "permissionKey", permissionKey));
    }
    
    /**
     * 활성 권한 조회
     * 
     * @return 활성 권한 목록
     */
    public List<Permission> getActivePermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findActivePermissionsByTenant(currentTenant, Status.ACTIVE);
    }
    
    /**
     * 시스템 권한 조회
     * 
     * @return 시스템 권한 목록
     */
    public List<Permission> getSystemPermissions() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> Boolean.TRUE.equals(permission.getIsSystem()))
                .collect(Collectors.toList());
    }
    
    /**
     * 카테고리별 권한 조회
     * 
     * @param category 카테고리
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByCategory(String category) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByTenantAndCategory(currentTenant, category);
    }
    
    /**
     * 리소스별 권한 조회
     * 
     * @param resource 리소스
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByResource(String resource) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> resource.equals(permission.getResource()))
                .collect(Collectors.toList());
    }
    
    /**
     * 액션별 권한 조회
     * 
     * @param action 액션
     * @return 권한 목록
     */
    public List<Permission> getPermissionsByAction(String action) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.findByTenant(currentTenant).stream()
                .filter(permission -> action.equals(permission.getAction()))
                .collect(Collectors.toList());
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
        return permissionRepository.findByTenantAndResourceAndAction(currentTenant, resource, action);
    }
    
    /**
     * 권한 검색
     * 
     * @param keyword 검색 키워드
     * @return 검색된 권한 목록
     */
    public List<Permission> searchPermissions(String keyword) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return permissionRepository.searchPermissionsByTenant(keyword, currentTenant);
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
        
        // 권한 키 중복 확인
        if (permissionRepository.existsByPermissionKeyAndTenant(request.getPermissionKey(), currentTenant)) {
            throw new BusinessException("이미 존재하는 권한 키입니다: " + request.getPermissionKey());
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
        
        // 캐시 무효화
        evictPermissionCache();
        
        log.info("Permission created: {} in tenant: {}", savedPermission.getPermissionKey(), currentTenant.getTenantKey());
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
        Permission permission = getPermissionByKeyOrThrow(permissionKey);
        
        // 시스템 권한 수정 제한
        if (permission.getIsSystem() && request.getIsSystem() != null && !request.getIsSystem()) {
            throw new BusinessException("시스템 권한은 수정할 수 없습니다");
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
        
        // 캐시 무효화
        evictPermissionCache();
        evictUserPermissionCache();
        
        log.info("Permission updated: {} in tenant: {}", permissionKey, permission.getTenant().getTenantKey());
        return updatedPermission;
    }
    
    /**
     * 권한 삭제
     * 
     * @param permissionKey 권한 키
     */
    @Transactional
    public void deletePermission(String permissionKey) {
        Permission permission = getPermissionByKeyOrThrow(permissionKey);
        
        // 시스템 권한 삭제 방지
        if (permission.getIsSystem()) {
            throw new BusinessException("시스템 권한은 삭제할 수 없습니다");
        }
        
        // 권한을 사용하는 역할 확인
        Long roleCount = permissionRepository.countRolesByPermission(permission);
        if (roleCount > 0) {
            throw new BusinessException("이 권한을 사용하는 역할이 있어 삭제할 수 없습니다");
        }
        
        // 소프트 삭제
        permission.setIsDeleted(true);
        permissionRepository.save(permission);
        
        // 캐시 무효화
        evictPermissionCache();
        evictUserPermissionCache();
        
        log.info("Permission deleted: {} in tenant: {}", permissionKey, permission.getTenant().getTenantKey());
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
    
    /**
     * 권한 캐시 무효화
     */
    private void evictPermissionCache() {
        try {
            redisTemplate.delete("permissions:*");
            log.debug("Permission cache evicted");
        } catch (Exception e) {
            log.warn("Failed to evict permission cache: {}", e.getMessage());
        }
    }
    
    /**
     * 사용자 권한 캐시 무효화
     */
    private void evictUserPermissionCache() {
        try {
            redisTemplate.delete("user_permissions:*");
            log.debug("User permission cache evicted");
        } catch (Exception e) {
            log.warn("Failed to evict user permission cache: {}", e.getMessage());
        }
    }
}
