package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.common.util.LogMaskingUtils;
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
        log.info("[RoleService] getAllRoles - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Role> result = roleRepository.findByTenantWithPermissions(currentTenant);
        log.info("[RoleService] getAllRoles - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 테넌트별 역할 조회
     * 
     * @param tenantKey 테넌트 키
     * @return 역할 목록
     */
    public List<Role> getRolesByTenant(String tenantKey) {
        log.info("[RoleService] getRolesByTenant - tenantKey={}", LogMaskingUtils.maskTenantKey(tenantKey));
        List<Role> result = roleRepository.findByTenantKey(tenantKey);
        log.info("[RoleService] getRolesByTenant - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(tenantKey));
        return result;
    }
    
    /**
     * 역할 키로 역할 조회
     * 
     * @param roleKey 역할 키
     * @return 역할 정보
     */
    public Optional<Role> getRoleByKey(String roleKey) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[RoleService] getRoleByKey - roleKey={} tenantKey={}", LogMaskingUtils.mask(roleKey, 2, 2), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        Optional<Role> result = roleRepository.findByRoleKeyAndTenantWithPermissions(roleKey, currentTenant);
        log.info("[RoleService] getRoleByKey - found={} roleKey={} tenantKey={}", result.isPresent(), LogMaskingUtils.mask(roleKey, 2, 2), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 역할 키로 역할 조회 (예외 발생)
     * 
     * @param roleKey 역할 키
     * @return 역할 정보
     * @throws ResourceNotFoundException 역할을 찾을 수 없는 경우
     */
    public Role getRoleByKeyOrThrow(String roleKey) {
        log.info("[RoleService] getRoleByKeyOrThrow - roleKey={}", LogMaskingUtils.mask(roleKey, 2, 2));
        Role role = getRoleByKey(roleKey)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "roleKey", roleKey));
        log.info("[RoleService] getRoleByKeyOrThrow - success roleKey={}", LogMaskingUtils.mask(roleKey, 2, 2));
        return role;
    }
    
    /**
     * 활성 역할 조회
     * 
     * @return 활성 역할 목록
     */
    public List<Role> getActiveRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[RoleService] getActiveRoles - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Role> result = roleRepository.findActiveRolesByTenant(currentTenant, Status.ACTIVE);
        log.info("[RoleService] getActiveRoles - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 시스템 역할 조회
     * 
     * @return 시스템 역할 목록
     */
    public List<Role> getSystemRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[RoleService] getSystemRoles - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Role> result = roleRepository.findSystemRolesByTenant(currentTenant, true);
        log.info("[RoleService] getSystemRoles - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 기본 역할 조회
     * 
     * @return 기본 역할 목록
     */
    public List<Role> getDefaultRoles() {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[RoleService] getDefaultRoles - tenantKey={}", LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Role> result = roleRepository.findDefaultRolesByTenant(currentTenant, true);
        log.info("[RoleService] getDefaultRoles - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
    }
    
    /**
     * 역할 검색
     * 
     * @param keyword 검색 키워드
     * @return 검색된 역할 목록
     */
    public List<Role> searchRoles(String keyword) {
        Tenant currentTenant = TenantContextHolder.getCurrentTenantOrThrow();
        log.info("[RoleService] searchRoles - keyword={} tenantKey={}", LogMaskingUtils.mask(keyword, 2, 1), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        List<Role> result = roleRepository.searchRolesByTenant(keyword, currentTenant);
        log.info("[RoleService] searchRoles - success count={} tenantKey={}", result.size(), LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        return result;
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
        log.info("[RoleService] createRole - tenantKey={} roleKey={} isSystem={} priority={}",
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()),
                LogMaskingUtils.mask(request.getRoleKey(), 2, 2),
                request.getIsSystem(),
                request.getPriority());
        
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
        
        log.info("[RoleService] createRole - success roleKey={} tenantKey={}",
                LogMaskingUtils.mask(savedRole.getRoleKey(), 2, 2),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
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
        log.info("[RoleService] updateRole - roleKey={} fields=[name:{}, desc:set?{}, priority:{}]",
                LogMaskingUtils.mask(roleKey, 2, 2),
                request.getRoleName(),
                request.getDescription() != null,
                request.getPriority());
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
        
        log.info("[RoleService] updateRole - success roleKey={} tenantKey={}",
                LogMaskingUtils.mask(roleKey, 2, 2),
                LogMaskingUtils.maskTenantKey(role.getTenant().getTenantKey()));
        return updatedRole;
    }
    
    /**
     * 역할 삭제
     * 
     * @param roleKey 역할 키
     */
    @Transactional
    public void deleteRole(String roleKey) {
        log.info("[RoleService] deleteRole - roleKey={}", LogMaskingUtils.mask(roleKey, 2, 2));
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
        
        log.info("[RoleService] deleteRole - success roleKey={} tenantKey={}",
                LogMaskingUtils.mask(roleKey, 2, 2),
                LogMaskingUtils.maskTenantKey(role.getTenant().getTenantKey()));
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
        log.info("[RoleService] assignPermissionsToRole - roleId={} permissionKeys={} tenantKey={}",
                roleId,
                permissionKeys == null ? 0 : permissionKeys.size(),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
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
        
        log.info("[RoleService] assignPermissionsToRole - success roleKey={} tenantKey={}",
                LogMaskingUtils.mask(role.getRoleKey(), 2, 2),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
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
        log.info("[RoleService] removePermissionFromRole - roleId={} permissionKey={} tenantKey={}",
                roleId,
                LogMaskingUtils.mask(permissionKey, 2, 2),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
        
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
        
        log.info("[RoleService] removePermissionFromRole - success roleKey={} tenantKey={}",
                LogMaskingUtils.mask(role.getRoleKey(), 2, 2),
                LogMaskingUtils.maskTenantKey(currentTenant.getTenantKey()));
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
