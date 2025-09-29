package com.agenticcp.core.domain.security.controller;

import com.agenticcp.core.domain.security.service.AuthorizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityScenarioIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthorizationService authorizationService;

    @Nested
    @DisplayName("시나리오 1: 권한 기반 접근 제어")
    class Scenario1 {
        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        @DisplayName("@PreAuthorize(hasAnyRole('ADMIN','AUDITOR')) → 200")
        void preAuthorize_Allows_Admin() throws Exception {
            mockMvc.perform(get("/api/v1/security/_test/protected/preauthorize"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        @DisplayName("@PreAuthorize(hasAnyRole('ADMIN','AUDITOR')) → 403")
        void preAuthorize_Denies_User() throws Exception {
            mockMvc.perform(get("/api/v1/security/_test/protected/preauthorize"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(username = "alice")
        @DisplayName("@RequirePermission('sample.permission') 허용 → 200")
        void requirePermission_Allows() throws Exception {
            doNothing().when(authorizationService).warmUserPermissionCache(anyString());
            org.mockito.Mockito.when(authorizationService.hasPermission("alice", "sample.permission"))
                    .thenReturn(true);

            mockMvc.perform(get("/api/v1/security/_test/protected/permission"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "bob")
        @DisplayName("@RequirePermission('sample.permission') 거부 → 403")
        void requirePermission_Denies() throws Exception {
            org.mockito.Mockito.when(authorizationService.hasPermission("bob", "sample.permission"))
                    .thenReturn(false);

            mockMvc.perform(get("/api/v1/security/_test/protected/permission"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("시나리오 2: 테넌트별 권한 격리")
    class Scenario2 {
        @Test
        @WithMockUser(username = "alice")
        @DisplayName("다른 테넌트 접근 시 403")
        void tenant_Isolation_Forbidden() throws Exception {
            doThrow(new AccessDeniedException("해당 테넌트에 대한 접근 권한이 없습니다"))
                    .when(authorizationService).validateTenantAccess("alice", "tnt-b");

            mockMvc.perform(post("/api/v1/security/tenant/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Tenant-Key", "tnt-b"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("시나리오 3: 권한 캐싱(간접 검증)")
    class Scenario3 {
        @Test
        @WithMockUser(username = "alice")
        @DisplayName("내 권한 조회 2회 연속 호출 200")
        void permissions_Called_Twice_ShouldOk() throws Exception {
            org.mockito.Mockito.when(authorizationService.getUserPermissions("alice"))
                    .thenReturn(java.util.Set.of("user.read"));

            mockMvc.perform(get("/api/v1/security/me/permissions"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/security/me/permissions"))
                    .andExpect(status().isOk());
        }
    }
}


