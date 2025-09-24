package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 시스템 역할 및 권한 초기화 서비스
 * 애플리케이션 시작 시 기본 시스템 역할과 권한을 생성합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemRolePermissionInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Starting system role and permission initialization...");
        
        // 모든 테넌트에 대해 시스템 역할과 권한 초기화
        initializeSystemRolesAndPermissions();
        
        log.info("System role and permission initialization completed");
    }
    
    /**
     * 시스템 역할과 권한 초기화
     */
    private void initializeSystemRolesAndPermissions() {
        // TODO: 실제 구현에서는 모든 테넌트를 조회하여 각 테넌트별로 초기화
        // 현재는 예시로 하나의 테넌트만 초기화
        log.info("System roles and permissions will be initialized per tenant on first access");
    }
    
    /**
     * 특정 테넌트에 대한 시스템 역할과 권한 초기화
     * 
     * @param tenant 테넌트
     */
    @Transactional
    public void initializeForTenant(Tenant tenant) {
        log.info("Initializing system roles and permissions for tenant: {}", tenant.getTenantKey());
        
        // 시스템 권한 생성
        List<Permission> systemPermissions = createSystemPermissions(tenant);
        permissionRepository.saveAll(systemPermissions);
        
        // 시스템 역할 생성
        List<Role> systemRoles = createSystemRoles(tenant, systemPermissions);
        roleRepository.saveAll(systemRoles);
        
        log.info("System roles and permissions initialized for tenant: {}", tenant.getTenantKey());
    }
    
    /**
     * 시스템 권한 생성
     * 
     * @param tenant 테넌트
     * @return 시스템 권한 목록
     */
    private List<Permission> createSystemPermissions(Tenant tenant) {
        return Arrays.asList(
            // 사용자 관리 권한
            createPermission(tenant, "USER_CREATE", "사용자 생성", "사용자를 생성할 수 있는 권한", "USER", "CREATE", "USER_MANAGEMENT", 100, true),
            createPermission(tenant, "USER_READ", "사용자 조회", "사용자 정보를 조회할 수 있는 권한", "USER", "READ", "USER_MANAGEMENT", 100, true),
            createPermission(tenant, "USER_UPDATE", "사용자 수정", "사용자 정보를 수정할 수 있는 권한", "USER", "UPDATE", "USER_MANAGEMENT", 100, true),
            createPermission(tenant, "USER_DELETE", "사용자 삭제", "사용자를 삭제할 수 있는 권한", "USER", "DELETE", "USER_MANAGEMENT", 100, true),
            
            // 역할 관리 권한
            createPermission(tenant, "ROLE_CREATE", "역할 생성", "역할을 생성할 수 있는 권한", "ROLE", "CREATE", "ROLE_MANAGEMENT", 100, true),
            createPermission(tenant, "ROLE_READ", "역할 조회", "역할 정보를 조회할 수 있는 권한", "ROLE", "READ", "ROLE_MANAGEMENT", 100, true),
            createPermission(tenant, "ROLE_UPDATE", "역할 수정", "역할 정보를 수정할 수 있는 권한", "ROLE", "UPDATE", "ROLE_MANAGEMENT", 100, true),
            createPermission(tenant, "ROLE_DELETE", "역할 삭제", "역할을 삭제할 수 있는 권한", "ROLE", "DELETE", "ROLE_MANAGEMENT", 100, true),
            
            // 권한 관리 권한
            createPermission(tenant, "PERMISSION_CREATE", "권한 생성", "권한을 생성할 수 있는 권한", "PERMISSION", "CREATE", "PERMISSION_MANAGEMENT", 100, true),
            createPermission(tenant, "PERMISSION_READ", "권한 조회", "권한 정보를 조회할 수 있는 권한", "PERMISSION", "READ", "PERMISSION_MANAGEMENT", 100, true),
            createPermission(tenant, "PERMISSION_UPDATE", "권한 수정", "권한 정보를 수정할 수 있는 권한", "PERMISSION", "UPDATE", "PERMISSION_MANAGEMENT", 100, true),
            createPermission(tenant, "PERMISSION_DELETE", "권한 삭제", "권한을 삭제할 수 있는 권한", "PERMISSION", "DELETE", "PERMISSION_MANAGEMENT", 100, true),
            
            // 테넌트 관리 권한
            createPermission(tenant, "TENANT_CREATE", "테넌트 생성", "테넌트를 생성할 수 있는 권한", "TENANT", "CREATE", "TENANT_MANAGEMENT", 100, true),
            createPermission(tenant, "TENANT_READ", "테넌트 조회", "테넌트 정보를 조회할 수 있는 권한", "TENANT", "READ", "TENANT_MANAGEMENT", 100, true),
            createPermission(tenant, "TENANT_UPDATE", "테넌트 수정", "테넌트 정보를 수정할 수 있는 권한", "TENANT", "UPDATE", "TENANT_MANAGEMENT", 100, true),
            createPermission(tenant, "TENANT_DELETE", "테넌트 삭제", "테넌트를 삭제할 수 있는 권한", "TENANT", "DELETE", "TENANT_MANAGEMENT", 100, true),
            
            // 클라우드 관리 권한
            createPermission(tenant, "CLOUD_CREATE", "클라우드 리소스 생성", "클라우드 리소스를 생성할 수 있는 권한", "CLOUD", "CREATE", "CLOUD_MANAGEMENT", 90, true),
            createPermission(tenant, "CLOUD_READ", "클라우드 리소스 조회", "클라우드 리소스를 조회할 수 있는 권한", "CLOUD", "READ", "CLOUD_MANAGEMENT", 90, true),
            createPermission(tenant, "CLOUD_UPDATE", "클라우드 리소스 수정", "클라우드 리소스를 수정할 수 있는 권한", "CLOUD", "UPDATE", "CLOUD_MANAGEMENT", 90, true),
            createPermission(tenant, "CLOUD_DELETE", "클라우드 리소스 삭제", "클라우드 리소스를 삭제할 수 있는 권한", "CLOUD", "DELETE", "CLOUD_MANAGEMENT", 90, true),
            
            // 모니터링 권한
            createPermission(tenant, "MONITORING_READ", "모니터링 조회", "모니터링 정보를 조회할 수 있는 권한", "MONITORING", "READ", "MONITORING", 80, true),
            createPermission(tenant, "MONITORING_UPDATE", "모니터링 설정", "모니터링 설정을 변경할 수 있는 권한", "MONITORING", "UPDATE", "MONITORING", 80, true),
            
            // 비용 관리 권한
            createPermission(tenant, "COST_READ", "비용 조회", "비용 정보를 조회할 수 있는 권한", "COST", "READ", "COST_MANAGEMENT", 70, true),
            createPermission(tenant, "COST_UPDATE", "비용 설정", "비용 설정을 변경할 수 있는 권한", "COST", "UPDATE", "COST_MANAGEMENT", 70, true)
        );
    }
    
    /**
     * 시스템 역할 생성
     * 
     * @param tenant 테넌트
     * @param systemPermissions 시스템 권한 목록
     * @return 시스템 역할 목록
     */
    private List<Role> createSystemRoles(Tenant tenant, List<Permission> systemPermissions) {
        return Arrays.asList(
            // SUPER_ADMIN 역할
            createRole(tenant, "SUPER_ADMIN", "최고 관리자", "모든 권한을 가진 최고 관리자 역할", 
                      systemPermissions, 100, true, false),
            
            // TENANT_ADMIN 역할
            createRole(tenant, "TENANT_ADMIN", "테넌트 관리자", "테넌트 내 모든 권한을 가진 관리자 역할", 
                      systemPermissions.stream()
                          .filter(p -> !p.getPermissionKey().startsWith("TENANT_"))
                          .collect(java.util.stream.Collectors.toList()), 90, true, false),
            
            // CLOUD_ADMIN 역할
            createRole(tenant, "CLOUD_ADMIN", "클라우드 관리자", "클라우드 리소스 관리 권한을 가진 역할", 
                      systemPermissions.stream()
                          .filter(p -> p.getPermissionKey().startsWith("CLOUD_") || 
                                     p.getPermissionKey().startsWith("MONITORING_") ||
                                     p.getPermissionKey().startsWith("COST_"))
                          .collect(java.util.stream.Collectors.toList()), 80, true, false),
            
            // DEVELOPER 역할
            createRole(tenant, "DEVELOPER", "개발자", "개발 관련 권한을 가진 역할", 
                      systemPermissions.stream()
                          .filter(p -> p.getPermissionKey().startsWith("CLOUD_") || 
                                     p.getPermissionKey().startsWith("MONITORING_"))
                          .collect(java.util.stream.Collectors.toList()), 70, true, false),
            
            // VIEWER 역할
            createRole(tenant, "VIEWER", "조회자", "조회 권한만을 가진 역할", 
                      systemPermissions.stream()
                          .filter(p -> p.getAction().equals("READ"))
                          .collect(java.util.stream.Collectors.toList()), 60, true, true),
            
            // AUDITOR 역할
            createRole(tenant, "AUDITOR", "감사자", "감사 및 모니터링 권한을 가진 역할", 
                      systemPermissions.stream()
                          .filter(p -> p.getPermissionKey().startsWith("MONITORING_") || 
                                     p.getPermissionKey().startsWith("COST_") ||
                                     p.getAction().equals("READ"))
                          .collect(java.util.stream.Collectors.toList()), 50, true, false)
        );
    }
    
    /**
     * 권한 생성 헬퍼 메서드
     */
    private Permission createPermission(Tenant tenant, String key, String name, String description, 
                                      String resource, String action, String category, 
                                      Integer priority, Boolean isSystem) {
        return Permission.builder()
                .permissionKey(key)
                .permissionName(name)
                .description(description)
                .tenant(tenant)
                .resource(resource)
                .action(action)
                .isSystem(isSystem)
                .category(category)
                .priority(priority)
                .status(Status.ACTIVE)
                .build();
    }
    
    /**
     * 역할 생성 헬퍼 메서드
     */
    private Role createRole(Tenant tenant, String key, String name, String description, 
                           List<Permission> permissions, Integer priority, Boolean isSystem, Boolean isDefault) {
        return Role.builder()
                .roleKey(key)
                .roleName(name)
                .description(description)
                .tenant(tenant)
                .permissions(permissions)
                .isSystem(isSystem)
                .isDefault(isDefault)
                .priority(priority)
                .status(Status.ACTIVE)
                .build();
    }
}
