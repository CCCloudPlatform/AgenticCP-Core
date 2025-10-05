package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.repository.PermissionRepository;
import com.agenticcp.core.domain.user.repository.RoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TenantAwareAuthorizationServiceTest {

    @Mock private TenantService tenantService;
    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;

    @InjectMocks private TenantAwareAuthorizationService sut;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setTenantKey("t-001");
        TenantContextHolder.setTenant(tenant);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void hasRoleInTenant_컨텍스트테넌트와일치하면_true() {
        given(tenantService.getTenantByKey("t-001")).willReturn(Optional.of(tenant));
        given(roleRepository.existsByRoleKeyAndTenant("ROLE_ADMIN", tenant)).willReturn(true);

        boolean result = sut.hasRoleInTenant("t-001", "ROLE_ADMIN");
        assertThat(result).isTrue();
    }

    @Test
    void hasRoleInTenant_컨텍스트불일치면_예외() {
        assertThrows(IllegalStateException.class, () -> sut.hasRoleInTenant("other", "ROLE_ADMIN"));
    }
}


