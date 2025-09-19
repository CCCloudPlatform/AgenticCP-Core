package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.dto.CreateRoleRequest;
import com.agenticcp.core.domain.user.dto.RoleResponse;
import com.agenticcp.core.domain.user.dto.UpdateRoleRequest;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 역할 관리 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleService {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 모든 역할 조회 (현재 테넌트)
     * 
     * @return 역할 목록
     */
    public List<Role> getAllRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.findByTenantWithPermissions(currentTenant);
    }
    
    /**
     * 테넌트별 역할 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 역할 목록
     */
    public List<Role> getRolesByTenant(String tenantKey) {
        return roleRepository.findByTenantKey(tenantKey);
    }
    
    /**
     * 역할 키로 역할 조회
     * 
     * @param roleKey 역할 키
     * @return 역할 정보
     */
    public Optional<Role> getRoleByKey(String roleKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.findByRoleKeyAndTenantWithPermissions(roleKey, currentTenant);
    }
    
    /**
     * 역할 키로 역할 조회 (예외 발생)
     * 
     * @param roleKey 역할 키
     * @return 역할 정보
     * @throws ResourceNotFoundException 역할을 찾을 수 없는 경우
     */
    public Role getRoleByKeyOrThrow(String roleKey) {
        return getRoleByKey(roleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "roleKey", roleKey));
    }
    
    /**
     * 활성 역할 조회
     * 
     * @return 활성 역할 목록
     */
    public List<Role> getActiveRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.findActiveRolesByTenant(currentTenant, Status.ACTIVE);
    }
    
    /**
     * 시스템 역할 조회
     * 
     * @return 시스템 역할 목록
     */
    public List<Role> getSystemRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.findSystemRolesByTenant(currentTenant, true);
    }
    
    /**
     * 기본 역할 조회
     * 
     * @return 기본 역할 목록
     */
    public List<Role> getDefaultRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.findDefaultRolesByTenant(currentTenant, true);
    }
    
    /**
     * 역할 검색
     * 
     * @param keyword 검색 키워드
     * @return 검색된 역할 목록
     */
    public List<Role> searchRoles(String keyword) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        return roleRepository.searchRolesByTenant(keyword, currentTenant);
    }
    
    /**
     * 역할 생성
     * 
     * @param request 역할 생성 요청
     * @return 생성된 역할
     */
    @Transactional
    public Role createRole(CreateRoleRequest request) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        
        // 역할 키 중복 확인
        if (roleRepository.existsByRoleKeyAndTenant(request.getRoleKey(), currentTenant)) {
            throw new BusinessException("이미 존재하는 역할 키입니다: " + request.getRoleKey());
        }
        
        // 역할 생성
        Role role = Role.builder()
                .roleKey(request.getRoleKey())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .tenant(currentTenant)
                .isSystem(Boolean.TRUE.equals(request.getIsSystem()))
                .isDefault(false)
                .priority(request.getPriority())
                .status(Status.ACTIVE)
                .build();
        
        Role savedRole = roleRepository.save(role);
        
        // 권한 매핑
        if (request.getPermissionKeys() != null && !request.getPermissionKeys().isEmpty()) {
            assignPermissionsToRole(savedRole.getId(), request.getPermissionKeys());
        }
        
        // 캐시 무효화
        evictRoleCache();
        
        log.info("Role created: {} in tenant: {}", savedRole.getRoleKey(), currentTenant.getTenantKey());
        // 권한 포함하여 다시 로드해 반환
        return roleRepository.findByIdAndTenantWithPermissions(savedRole.getId(), currentTenant)
                .orElse(savedRole);
    }
    
    /**
     * 역할 수정
     * 
     * @param roleKey 역할 키
     * @param request 역할 수정 요청
     * @return 수정된 역할
     */
    @Transactional
    public Role updateRole(String roleKey, UpdateRoleRequest request) {
        Role role = getRoleByKeyOrThrow(roleKey);
        
        // 시스템 역할 수정 제한
        if (role.getIsSystem() && request.getIsSystem() != null && !request.getIsSystem()) {
            throw new BusinessException("시스템 역할은 수정할 수 없습니다");
        }
        
        // 역할 정보 업데이트
        if (request.getRoleName() != null) {
            role.setRoleName(request.getRoleName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            role.setPriority(request.getPriority());
        }
        
        Role updatedRole = roleRepository.save(role);
        
        // 권한 업데이트
        if (request.getPermissionKeys() != null) {
            updateRolePermissions(role.getId(), request.getPermissionKeys());
        }
        
        // 캐시 무효화
        evictRoleCache();
        
        log.info("Role updated: {} in tenant: {}", roleKey, role.getTenant().getTenantKey());
        return updatedRole;
    }
    
    /**
     * 역할 삭제
     * 
     * @param roleKey 역할 키
     */
    @Transactional
    public void deleteRole(String roleKey) {
        Role role = getRoleByKeyOrThrow(roleKey);
        
        // 시스템 역할 삭제 방지
        if (role.getIsSystem()) {
            throw new BusinessException("시스템 역할은 삭제할 수 없습니다");
        }
        
        // 기본 역할 삭제 방지
        if (role.getIsDefault()) {
            throw new BusinessException("기본 역할은 삭제할 수 없습니다");
        }
        
        // 역할을 사용하는 사용자 확인
        Long userCount = roleRepository.countUsersByRole(role);
        if (userCount > 0) {
            throw new BusinessException("이 역할을 사용하는 사용자가 있어 삭제할 수 없습니다");
        }
        
        // 소프트 삭제
        role.setIsDeleted(true);
        roleRepository.save(role);
        
        // 캐시 무효화
        evictRoleCache();
        
        log.info("Role deleted: {} in tenant: {}", roleKey, role.getTenant().getTenantKey());
    }
    
    /**
     * 역할에 권한 할당
     * 
     * @param roleId 역할 ID
     * @param permissionKeys 권한 키 목록
     */
    @Transactional
    public void assignPermissionsToRole(Long roleId, List<String> permissionKeys) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        
        // 테넌트 확인
        if (!role.getTenant().equals(currentTenant)) {
            throw new BusinessException("다른 테넌트의 역할에 접근할 수 없습니다");
        }
        
        List<Permission> permissions = permissionRepository.findByPermissionKeyInAndTenant(permissionKeys, currentTenant);
        
        if (permissions.size() != permissionKeys.size()) {
            throw new BusinessException("일부 권한을 찾을 수 없습니다");
        }
        
        role.setPermissions(permissions);
        roleRepository.save(role);
        
        // 사용자 권한 캐시 무효화
        evictUserPermissionCache();
        
        log.info("Permissions assigned to role: {} in tenant: {}", role.getRoleKey(), currentTenant.getTenantKey());
    }
    
    /**
     * 역할 권한 업데이트
     * 
     * @param roleId 역할 ID
     * @param permissionKeys 권한 키 목록
     */
    @Transactional
    public void updateRolePermissions(Long roleId, List<String> permissionKeys) {
        assignPermissionsToRole(roleId, permissionKeys);
    }
    
    /**
     * 역할에서 권한 제거
     * 
     * @param roleId 역할 ID
     * @param permissionKey 권한 키
     */
    @Transactional
    public void removePermissionFromRole(Long roleId, String permissionKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        
        // 테넌트 확인
        if (!role.getTenant().equals(currentTenant)) {
            throw new BusinessException("다른 테넌트의 역할에 접근할 수 없습니다");
        }
        
        Permission permission = permissionRepository.findByPermissionKey(permissionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "permissionKey", permissionKey));
        
        // 테넌트 확인
        if (!permission.getTenant().equals(currentTenant)) {
            throw new BusinessException("다른 테넌트의 권한에 접근할 수 없습니다");
        }
        
        List<Permission> permissions = role.getPermissions();
        if (permissions != null) {
            permissions.removeIf(p -> p.getPermissionKey().equals(permissionKey));
            role.setPermissions(permissions);
            roleRepository.save(role);
        }
        
        // 사용자 권한 캐시 무효화
        evictUserPermissionCache();
        
        log.info("Permission removed from role: {} in tenant: {}", role.getRoleKey(), currentTenant.getTenantKey());
    }
    
    /**
     * 역할 응답 DTO 변환
     * 
     * @param role 역할 엔티티
     * @return 역할 응답 DTO
     */
    public RoleResponse toRoleResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .roleKey(role.getRoleKey())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .tenantKey(role.getTenant().getTenantKey())
                .tenantName(role.getTenant().getTenantName())
                .status(role.getStatus())
                .isSystem(role.getIsSystem())
                .isDefault(role.getIsDefault())
                .priority(role.getPriority())
                .permissions(role.getPermissions() != null ? 
                    role.getPermissions().stream()
                        .map(this::toPermissionResponse)
                        .collect(Collectors.toList()) : null)
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .createdBy(role.getCreatedBy())
                .updatedBy(role.getUpdatedBy())
                .build();
    }
    
    /**
     * 권한 응답 DTO 변환
     * 
     * @param permission 권한 엔티티
     * @return 권한 응답 DTO
     */
    private com.agenticcp.core.domain.user.dto.PermissionResponse toPermissionResponse(Permission permission) {
        return com.agenticcp.core.domain.user.dto.PermissionResponse.builder()
                .id(permission.getId())
                .permissionKey(permission.getPermissionKey())
                .permissionName(permission.getPermissionName())
                .description(permission.getDescription())
                .tenantKey(permission.getTenant().getTenantKey())
                .tenantName(permission.getTenant().getTenantName())
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
     * 역할 캐시 무효화
     */
    private void evictRoleCache() {
        try {
            redisTemplate.delete("roles:*");
            log.debug("Role cache evicted");
        } catch (Exception e) {
            log.warn("Failed to evict role cache: {}", e.getMessage());
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
