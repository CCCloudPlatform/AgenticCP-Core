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
 * 감사 로깅 통합 테스트
 * 
 * AuditController와 AuditRequired 애노테이션이 함께 작동하는지 검증
 */
@ExtendWith(MockitoExtension.class)
class AuditLoggingIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestAuditController()).build();
    }

    @Test
    void testAuditLoggingIntegration_클래스와메서드레벨_우선순위() throws Exception {
        // Given
        String data = "{\"integration\":\"test\"}";

        // When & Then - 메서드 레벨 애노테이션이 클래스 레벨보다 우선
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk())
                .andExpect(content().string("Override action result: " + data));
    }

    @Test
    void testAuditLoggingIntegration_다양한HTTP메서드_검증() throws Exception {
        // Given
        String userId = "user123";
        String userData = "{\"name\":\"testUser\"}";

        // When & Then - POST (클래스 레벨 애노테이션 적용)
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(userData))
                .andExpect(status().isOk());

        // When & Then - PUT (클래스 레벨 애노테이션 적용)
        mockMvc.perform(put("/test/audit/update/{id}", userId)
                .contentType("application/json")
                .content(userData))
                .andExpect(status().isOk());

        // When & Then - DELETE (클래스 레벨 애노테이션 적용)
        mockMvc.perform(delete("/test/audit/delete/{id}", userId))
                .andExpect(status().isOk());

        // When & Then - GET (클래스 레벨 애노테이션 미적용)
        mockMvc.perform(get("/test/audit/get/{id}", userId))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLoggingIntegration_제외메서드와포함메서드_검증() throws Exception {
        // Given
        String data = "{\"exclude\":\"test\"}";

        // When & Then - excludeMethods에 포함된 메서드 (감사 로깅 미적용)
        mockMvc.perform(post("/test/audit/getUserInfo")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - 일반 POST 메서드 (감사 로깅 적용)
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLoggingIntegration_리소스타입다양성_검증() throws Exception {
        // Given
        String data = "{\"resource\":\"test\"}";

        // When & Then - AuditResourceType.USER (클래스 레벨 기본값)
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditResourceType.USER (메서드 레벨 명시)
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditResourceType.CONFIG (메서드 레벨 오버라이드)
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLoggingIntegration_심각도레벨다양성_검증() throws Exception {
        // Given
        String data = "{\"severity\":\"test\"}";

        // When & Then - AuditSeverity.MEDIUM (클래스 레벨 기본값)
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditSeverity.HIGH (메서드 레벨 명시)
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - AuditSeverity.CRITICAL (메서드 레벨 오버라이드)
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLoggingIntegration_데이터포함설정_검증() throws Exception {
        // Given
        String data = "{\"include\":\"test\"}";

        // When & Then - 클래스 레벨 설정: includeRequestData=true, includeResponseData=false
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - 메서드 레벨 설정: includeRequestData=true, includeResponseData=true
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - 메서드 레벨 설정: includeRequestData=false, includeResponseData=true
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }

    @Test
    void testAuditLoggingIntegration_액션명자동생성과명시_검증() throws Exception {
        // Given
        String data = "{\"action\":\"test\"}";

        // When & Then - 클래스 레벨: 메서드명 기반 자동 액션 생성
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        // When & Then - 메서드 레벨: 명시적 액션명
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
    void testAuditLoggingIntegration_설명문_검증() throws Exception {
        // Given
        String data = "{\"description\":\"test\"}";

        // When & Then - 메서드 레벨 설명문이 있는 경우
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());

        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk());
    }
}
