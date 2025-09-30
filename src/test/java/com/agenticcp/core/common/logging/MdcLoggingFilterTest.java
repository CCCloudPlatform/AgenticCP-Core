package com.agenticcp.core.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.MDC;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class MdcLoggingFilterTest {

    @Test
    @DisplayName("requestId/tenantId를 설정하고 종료 시 MDC를 정리한다")
    void adds_requestId_and_tenantId_then_clears() throws Exception {
        var props = new MdcProperties(
                List.of("requestId","tenantId","clientIp"),
                "uuid","req_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            assertThat(MDC.get(MdcKeys.REQUEST_ID)).startsWith("req_");
            assertThat(MDC.get(MdcKeys.TENANT_ID)).isNotNull();
        };

        filter.doFilter(req, res, chain);
        assertThat(MDC.getCopyOfContextMap()).isNull();
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("timestamp 타입 requestId를 생성한다")
    void generates_timestamp_requestId_whenConfigured() throws Exception {
        var props = new MdcProperties(
                List.of("requestId"),
                "timestamp","ts_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            String requestId = MDC.get(MdcKeys.REQUEST_ID);
            assertThat(requestId).startsWith("ts_");
            assertThat(requestId.substring(3)).matches("\\d+"); // 숫자만
        };

        filter.doFilter(req, res, chain);
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("sequence 타입 requestId를 생성한다")
    void generates_sequence_requestId_whenConfigured() throws Exception {
        var props = new MdcProperties(
                List.of("requestId"),
                "sequence","seq_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            String requestId = MDC.get(MdcKeys.REQUEST_ID);
            assertThat(requestId).startsWith("seq_");
            assertThat(requestId.substring(4)).matches("\\d+"); // 숫자만
        };

        filter.doFilter(req, res, chain);
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("uuid 타입 requestId를 지정 길이로 생성한다")
    void generates_uuid_requestId_with_custom_length() throws Exception {
        var props = new MdcProperties(
                List.of("requestId"),
                "uuid","test_",12,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            String requestId = MDC.get(MdcKeys.REQUEST_ID);
            assertThat(requestId).startsWith("test_");
            assertThat(requestId).hasSize(17); // "test_" + 12 chars
        };

        filter.doFilter(req, res, chain);
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("비활성화된 키는 설정하지 않는다")
    void skips_disabled_keys() throws Exception {
        var props = new MdcProperties(
                List.of("clientIp"), // requestId, tenantId 비활성화
                "uuid","req_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            assertThat(MDC.get(MdcKeys.REQUEST_ID)).isNull();
            assertThat(MDC.get(MdcKeys.TENANT_ID)).isNull();
        };

        filter.doFilter(req, res, chain);
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("tenantId가 없을 때 system으로 설정한다")
    void sets_tenantId_to_system_when_null() throws Exception {
        var props = new MdcProperties(
                List.of("tenantId"),
                "uuid","req_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            assertThat(MDC.get(MdcKeys.TENANT_ID)).isEqualTo("system");
        };

        filter.doFilter(req, res, chain);
        verify(provider).setContext(any(HttpServletRequest.class));
    }

    @Test
    @DisplayName("필터 처리 중 예외 발생해도 MDC를 정리한다")
    void handles_filter_exception_and_clears_mdc() throws Exception {
        var props = new MdcProperties(
                List.of("requestId"),
                "uuid","req_",8,false,false, 20);

        var provider = mock(MdcContextProvider.class);
        var filter = new MdcLoggingFilter(provider, props);

        var req = mock(HttpServletRequest.class);
        var res = mock(ServletResponse.class);

        FilterChain chain = (request, response) -> {
            throw new RuntimeException("Test exception");
        };

        try {
            filter.doFilter(req, res, chain);
        } catch (RuntimeException e) {
            assertThat(MDC.getCopyOfContextMap()).isNull();
        }
    }
}
