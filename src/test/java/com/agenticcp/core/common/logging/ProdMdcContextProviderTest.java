package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProdMdcContextProviderTest {

    private ProdMdcContextProvider provider;
    private MdcProperties properties;

    @BeforeEach
    void setUp() {
        MDC.clear();
        properties = new MdcProperties(
                List.of("userId", "sessionId", "clientIp", "userAgent"),
                "uuid", "req_", 8, true, true, 20);
        provider = new ProdMdcContextProvider(properties);
    }

    @Test
    @DisplayName("세션/헤더가 존재하면 모든 컨텍스트를 설정한다")
    void setContext_setsAllContext_whenSessionExists() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);

        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("session123");
        when(request.getHeader("Authorization")).thenReturn("Bearer token123");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.SESSION_ID)).isEqualTo("session123");
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull(); // JWT 파싱 미구현으로 null
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("192.168.1.***");
        assertThat(MDC.get(MdcKeys.USER_AGENT)).startsWith("Mozilla/5.0 (Window").endsWith("...");
    }

    @Test
    @DisplayName("세션이 없을 때 안전하게 처리한다")
    void setContext_handlesNoSession() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Short UA");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.SESSION_ID)).isNull();
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull();
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("192.168.1.***");
        assertThat(MDC.get(MdcKeys.USER_AGENT)).isEqualTo("Short UA"); // 짧아서 마스킹 안됨
    }

    @Test
    @DisplayName("잘못된 Authorization 헤더를 무시한다")
    void setContext_handlesInvalidAuthHeader() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Invalid token");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull(); // Bearer로 시작하지 않아서 null
        assertThat(MDC.get(MdcKeys.USER_AGENT)).startsWith("Mozilla/5.0 (Window").endsWith("...");
    }

    @Test
    @DisplayName("헤더가 모두 null일 때 null을 반환한다")
    void setContext_handlesNullHeaders() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession()).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(null);

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.SESSION_ID)).isNull();
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull();
        // getClientIpAddress는 remoteAddr가 null이면 null 반환
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isNull();
        assertThat(MDC.get(MdcKeys.USER_AGENT)).isNull();
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더를 우선 사용한다")
    void setContext_usesXForwardedFor_whenPresent() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.100");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test Agent");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("10.0.0.***");
    }
}
