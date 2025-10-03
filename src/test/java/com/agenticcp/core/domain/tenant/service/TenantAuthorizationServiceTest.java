package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.Permission;
import com.agenticcp.core.domain.user.entity.Role;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import com.agenticcp.core.domain.user.service.PermissionService;
import com.agenticcp.core.domain.user.service.RoleService;
import com.agenticcp.core.domain.user.service.SystemRolePermissionInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAuthorizationServiceTest {

    @Mock private TenantService tenantService;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleService roleService;
    @Mock private PermissionService permissionService;
    @Mock private SystemRolePermissionInitializer systemRolePermissionInitializer;
    @Mock private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks private TenantAuthorizationService sut;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantKey("t-001");
        tenant.setTenantName("Tenant 001");
    }

    @Test
    void getTenantPermissions_캐시미스_DB조회후캐시저장() {
        String tenantKey = "t-001";
        given(tenantService.getTenantByKey(tenantKey)).willReturn(Optional.of(tenant));
        given(permissionRepository.findByTenantKey(tenantKey)).willReturn(List.of(new Permission()));

        var ops = mock(org.springframework.data.redis.core.ValueOperations.class);
        given(redisTemplate.opsForValue()).willReturn(ops);
        given(ops.get("tenant_permissions:" + tenantKey)).willReturn(null);

        var result = sut.getTenantPermissions(tenantKey);

        assertThat(result).hasSize(1);
        verify(ops).set(startsWith("tenant_permissions:"), any(), any());
    }

    @Test
    void initializeTenantPermissions_초기화후캐시무효화() {
        String tenantKey = "t-001";
        given(tenantService.getTenantByKey(tenantKey)).willReturn(Optional.of(tenant));

        sut.initializeTenantPermissions(tenantKey);

        verify(systemRolePermissionInitializer).initializeForTenant(tenant);
        verify(redisTemplate).delete("tenant_roles:" + tenantKey);
        verify(redisTemplate).delete("tenant_permissions:" + tenantKey);
    }
}


