package com.agenticcp.core.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractMdcContextProviderTest {

    static class TestProvider extends AbstractMdcContextProvider {
        TestProvider(MdcProperties p) { super(p); }
        @Override public void setContext(HttpServletRequest req) {}
        String ip(HttpServletRequest r){ return getClientIpAddress(r); }
        String ua(HttpServletRequest r){ return getUserAgent(r); }
    }

    @Test
    @DisplayName("X-Forwarded-For 첫 번째 IP를 마스킹한다")
    void xForwardedFor_firstIpMasked_whenEnabled() {
        var props = new MdcProperties(
                List.of("clientIp"), "uuid","req_",8,true,true, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");
        assertThat(provider.ip(req)).isEqualTo("1.2.3.***");
    }

    @Test
    @DisplayName("X-Real-IP 헤더가 있으면 그것을 사용한다")
    void xRealIp_used_whenPresent() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn("10.0.0.9");
        assertThat(provider.ip(req)).isEqualTo("10.0.0.9");
    }

    @Test
    @DisplayName("User-Agent 마스킹이 활성화되고 길면 마스킹한다")
    void userAgent_mask_whenEnabledAndLong() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,true, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        assertThat(provider.ua(req)).startsWith("Mozilla/5.0 (Macin").endsWith("...");
    }

    @Test
    @DisplayName("User-Agent 마스킹이 비활성화되면 원문을 반환한다")
    void userAgent_noMask_whenDisabled() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("User-Agent"))
                .thenReturn("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        assertThat(provider.ua(req)).isEqualTo("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
    }

    @Test
    @DisplayName("User-Agent가 짧으면 마스킹하지 않는다")
    void userAgent_noMask_whenShort() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,true, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("User-Agent")).thenReturn("Short UA");
        assertThat(provider.ua(req)).isEqualTo("Short UA");
    }

    @Test
    @DisplayName("User-Agent 헤더가 없으면 null을 반환한다")
    void userAgent_null_whenHeaderMissing() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,true, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("User-Agent")).thenReturn(null);
        assertThat(provider.ua(req)).isNull();
    }

    @Test
    @DisplayName("IPv4 주소를 마스킹한다")
    void clientIp_mask_ipv4_whenEnabled() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,true,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.168.1.100");
        
        assertThat(provider.ip(req)).isEqualTo("192.168.1.***");
    }

    @Test
    @DisplayName("IPv6 주소를 마스킹한다")
    void clientIp_mask_ipv6_whenEnabled() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,true,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        
        assertThat(provider.ip(req)).isEqualTo("2001:0db8:85a3:0000:****");
    }

    @Test
    @DisplayName("IP 마스킹 비활성화 시 원문을 사용한다")
    void clientIp_noMask_whenDisabled() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,false,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("192.168.1.100");
        
        assertThat(provider.ip(req)).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("잘못된 IP 형식은 변경하지 않는다")
    void clientIp_handles_malformed_ip() {
        var props = new MdcProperties(List.of(), "uuid","req_",8,true,false, 20);
        var provider = new TestProvider(props);

        var req = mock(HttpServletRequest.class);
        when(req.getHeader("X-Forwarded-For")).thenReturn(null);
        when(req.getHeader("X-Real-IP")).thenReturn(null);
        when(req.getRemoteAddr()).thenReturn("invalid-ip");
        
        assertThat(provider.ip(req)).isEqualTo("invalid-ip");
    }
}

