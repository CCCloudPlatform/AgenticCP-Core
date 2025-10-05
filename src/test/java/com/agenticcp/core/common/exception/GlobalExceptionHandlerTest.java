package com.agenticcp.core.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Logstash 인코더 문제로 인한 ApplicationContext 로딩 실패")
public class GlobalExceptionHandlerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException 처리 테스트")
    void testBusinessException() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("DataAccessException 처리 테스트")
    void testDataAccessException() throws Exception {
        mockMvc.perform(get("/test/data-access-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_2"))
                .andExpect(jsonPath("$.message").value("데이터베이스 오류가 발생했습니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("AuthorizationException 처리 테스트")
    void testAuthorizationException() throws Exception {
        mockMvc.perform(get("/test/authorization-exception"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_403"))
                .andExpect(jsonPath("$.message").value("User(ID: 1): 'testResource' 리소스에 대한 'read' 권한이 없습니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ResourceNotFoundException 처리 테스트")
    void testResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_404"))
                .andExpect(jsonPath("$.message").value("User 리소스를 찾을 수 없습니다. (id: 123)"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 테스트")
    void testMethodArgumentNotValidException() throws Exception {
        String invalidRequest = "{\"name\": null, \"email\": \"\"}";

        mockMvc.perform(post("/test/validation-error")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_422"))
                .andExpect(jsonPath("$.message").value("필드 유효성 검증에 실패했습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].reason").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("HttpRequestMethodNotSupportedException 처리 테스트")
    void testHttpRequestMethodNotSupportedException() throws Exception {
        mockMvc.perform(get("/test/method-not-allowed"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_405"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Unexpected Exception 처리 테스트")
    void testUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_1"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("MissingServletRequestParameterException 처리 테스트")
    void testMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_422"))
                .andExpect(jsonPath("$.message").value("필드 유효성 검증에 실패했습니다."))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("requiredParam"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("필수 요청 파라미터가 누락되었습니다."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException 처리 테스트")
    void testMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("id"))
                .andExpect(jsonPath("$.fieldErrors[0].value").value("abc"))
                .andExpect(jsonPath("$.fieldErrors[0].reason").value("값 타입이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("HttpMessageNotReadableException 처리 테스트")
    void testHttpMessageNotReadableException() throws Exception {
        String malformedJson = "{\"invalid\": json}";

        mockMvc.perform(post("/test/malformed-json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"));
    }
}
