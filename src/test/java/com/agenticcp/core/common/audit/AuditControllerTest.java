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
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuditController 애노테이션 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    private MockMvc mockMvc;

    @Mock
    private WebApplicationContext webApplicationContext;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestAuditController()).build();
    }

    @Test
    void testAuditControllerAnnotation_클래스레벨애노테이션_정상작동() throws Exception {
        // Given
        String userData = "{\"name\":\"testUser\",\"email\":\"test@example.com\"}";

        // When & Then - POST 메서드는 targetHttpMethods에 포함되어 감사 로깅 적용
        mockMvc.perform(post("/test/audit/create")
                .contentType("application/json")
                .content(userData))
                .andExpect(status().isOk())
                .andExpect(content().string("User created: " + userData));
    }

    @Test
    void testAuditControllerAnnotation_PUT메서드_감사로깅적용() throws Exception {
        // Given
        String userId = "user123";
        String userData = "{\"name\":\"updatedUser\"}";

        // When & Then - PUT 메서드는 targetHttpMethods에 포함되어 감사 로깅 적용
        mockMvc.perform(put("/test/audit/update/{id}", userId)
                .contentType("application/json")
                .content(userData))
                .andExpect(status().isOk())
                .andExpect(content().string("User updated: " + userId + " with " + userData));
    }

    @Test
    void testAuditControllerAnnotation_DELETE메서드_감사로깅적용() throws Exception {
        // Given
        String userId = "user123";

        // When & Then - DELETE 메서드는 targetHttpMethods에 포함되어 감사 로깅 적용
        mockMvc.perform(delete("/test/audit/delete/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("User deleted: " + userId));
    }

    @Test
    void testAuditControllerAnnotation_GET메서드_감사로깅미적용() throws Exception {
        // Given
        String userId = "user123";

        // When & Then - GET 메서드는 targetHttpMethods에 없어서 감사 로깅 미적용
        mockMvc.perform(get("/test/audit/get/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(content().string("User: " + userId));
    }

    @Test
    void testAuditControllerAnnotation_제외메서드_감사로깅미적용() throws Exception {
        // Given
        String request = "{\"userId\":\"user123\"}";

        // When & Then - excludeMethods에 포함된 메서드는 감사 로깅 미적용
        mockMvc.perform(post("/test/audit/getUserInfo")
                .contentType("application/json")
                .content(request))
                .andExpect(status().isOk())
                .andExpect(content().string("User info: " + request));
    }

    @Test
    void testAuditControllerAnnotation_메서드레벨우선순위() throws Exception {
        // Given
        String data = "{\"action\":\"test\"}";

        // When & Then - 메서드 레벨 애노테이션이 클래스 레벨보다 우선
        mockMvc.perform(post("/test/audit/override")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk())
                .andExpect(content().string("Override action result: " + data));
    }

    @Test
    void testAuditControllerAnnotation_커스텀액션() throws Exception {
        // Given
        String data = "{\"custom\":\"data\"}";

        // When & Then - 메서드 레벨 커스텀 애노테이션 적용
        mockMvc.perform(post("/test/audit/custom")
                .contentType("application/json")
                .content(data))
                .andExpect(status().isOk())
                .andExpect(content().string("Custom action result: " + data));
    }
}

