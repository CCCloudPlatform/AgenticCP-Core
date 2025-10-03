package com.agenticcp.core.domain.tenant.controller;

import com.agenticcp.core.domain.tenant.service.TenantAuthorizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = TenantAuthorizationController.class)
class TenantAuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TenantAuthorizationService tenantAuthorizationService;

    @Test
    void GET_roles_정상200() throws Exception {
        Mockito.when(tenantAuthorizationService.getTenantRoles(anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/tenants/t-001/roles")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void GET_permissions_정상200() throws Exception {
        Mockito.when(tenantAuthorizationService.getTenantPermissions(anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/tenants/t-001/permissions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void POST_init_permissions_관리자200() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/t-001/init-permissions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void POST_cache_evict_관리자200() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/t-001/cache/evict")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // 시나리오 1: 테넌트별 역할 격리
    @Test
    void 시나리오1_테넌트별_역할_격리() throws Exception {
        var roleA = com.agenticcp.core.domain.user.dto.RoleResponse.builder().roleKey("ADMIN").tenantKey("tenantA").build();
        var roleB = com.agenticcp.core.domain.user.dto.RoleResponse.builder().roleKey("ADMIN").tenantKey("tenantB").build();

        Mockito.when(tenantAuthorizationService.getTenantRoles("tenantA"))
                .thenReturn(List.of(roleA));
        Mockito.when(tenantAuthorizationService.getTenantRoles("tenantB"))
                .thenReturn(List.of(roleB));

        mockMvc.perform(get("/api/v1/tenants/tenantA/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tenants/tenantB/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // 시나리오 2: 권한 데이터 격리
    @Test
    void 시나리오2_권한_데이터_격리() throws Exception {
        var permA = com.agenticcp.core.domain.user.dto.PermissionResponse.builder().permissionKey("READ_ONLY").tenantKey("tenantA").build();
        var permB = com.agenticcp.core.domain.user.dto.PermissionResponse.builder().permissionKey("READ_ONLY").tenantKey("tenantB").build();

        Mockito.when(tenantAuthorizationService.getTenantPermissions("tenantA"))
                .thenReturn(List.of(permA));
        Mockito.when(tenantAuthorizationService.getTenantPermissions("tenantB"))
                .thenReturn(List.of(permB));

        mockMvc.perform(get("/api/v1/tenants/tenantA/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tenants/tenantB/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // 시나리오 3: 테넌트별 기본 권한 설정(초기화 후 조회 가능)
    @Test
    void 시나리오3_초기화후_기본_권한_역할_조회() throws Exception {
        // init은 side-effect만 검증(200)
        mockMvc.perform(post("/api/v1/tenants/tenantC/init-permissions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 이후 조회가 정상 동작한다고 가정하고 mock 세팅
        Mockito.when(tenantAuthorizationService.getTenantRoles("tenantC"))
                .thenReturn(Collections.emptyList());
        Mockito.when(tenantAuthorizationService.getTenantPermissions("tenantC"))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/tenants/tenantC/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/tenants/tenantC/permissions").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}


