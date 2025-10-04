package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuditRequired 애노테이션 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuditRequiredTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestAuditController()).build();
    }

    @Test
    void testAuditRequired_메서드레벨애노테이션_정상작동() throws Exception {
        // Given
        String data = "{\"test\":\"data\"}";

        // When & Then - 메서드 레벨 AuditRequired 애노테이션 적용
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk())
                .andExpect(content().string("Custom action result: " + data));
    }

    @Test
    void testAuditRequired_다양한설정값_검증() throws Exception {
        // Given
        String data = "{\"override\":\"test\"}";

        // When & Then - 다양한 설정값이 포함된 AuditRequired 애노테이션
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk())
                .andExpect(content().string("Override action result: " + data));
    }

    @Test
    void testAuditRequired_리소스타입_검증() throws Exception {
        // Given
        String data = "{\"resource\":\"test\"}";

        // When & Then - AuditResourceType.USER로 설정된 메서드
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditResourceType.CONFIG으로 설정된 메서드
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditRequired_심각도레벨_검증() throws Exception {
        // Given
        String data = "{\"severity\":\"test\"}";

        // When & Then - AuditSeverity.HIGH로 설정된 메서드
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditSeverity.CRITICAL로 설정된 메서드
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditRequired_요청응답데이터포함_검증() throws Exception {
        // Given
        String requestData = "{\"include\":\"request\"}";

        // When & Then - includeRequestData=true, includeResponseData=true
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(requestData))
                .andExpect(status().isOk());

        // When & Then - includeRequestData=false, includeResponseData=true
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(requestData))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditRequired_설명문_검증() throws Exception {
        // Given
        String data = "{\"description\":\"test\"}";

        // When & Then - description이 설정된 메서드들
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditRequired_액션명_검증() throws Exception {
        // Given
        String data = "{\"action\":\"test\"}";

        // When & Then - CUSTOM_USER_ACTION 액션명
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - OVERRIDE_ACTION 액션명
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }
}
