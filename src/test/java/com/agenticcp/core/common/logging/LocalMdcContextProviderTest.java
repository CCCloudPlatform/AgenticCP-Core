package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocalMdcContextProviderTest {

    private LocalMdcContextProvider provider;
    private MdcProperties properties;

    @BeforeEach
    void setUp() {
        MDC.clear();
        properties = new MdcProperties(
                List.of("clientIp"), "uuid", "req_", 8, true, false, 20);
        provider = new LocalMdcContextProvider(properties);
    }

    @Test
    @DisplayName("클라이언트 IP를 설정한다")
    void setContext_setsClientIp_whenEnabled() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("192.168.1.***");
    }

    @Test
    @DisplayName("X-Forwarded-For 헤더를 우선 사용한다")
    void setContext_usesXForwardedFor_whenPresent() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.100");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("10.0.0.***");
    }

    @Test
    @DisplayName("X-Real-IP 헤더를 사용한다")
    void setContext_usesXRealIp_whenXForwardedForNotPresent() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("172.16.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isEqualTo("172.16.0.***");
    }

    @Test
    @DisplayName("기타 MDC 키를 설정하지 않는다")
    void setContext_doesNotSetOtherKeys() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.USER_ID)).isNull();
        assertThat(MDC.get(MdcKeys.SESSION_ID)).isNull();
        assertThat(MDC.get(MdcKeys.USER_AGENT)).isNull();
    }

    @Test
    @DisplayName("null IP를 안전하게 처리한다")
    void setContext_handlesNullClientIp() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        // When
        provider.setContext(request);

        // Then
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isNull();
    }
}
