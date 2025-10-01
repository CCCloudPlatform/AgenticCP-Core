package com.agenticcp.core.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LogMaskingUtilsTest {

    @Test
    @DisplayName("IPv4 주소를 마스킹한다")
    void maskIpAddress_ipv4() {
        assertThat(LogMaskingUtils.maskIpAddress("192.168.1.100"))
                .isEqualTo("192.168.1.***");
        assertThat(LogMaskingUtils.maskIpAddress("10.0.0.1"))
                .isEqualTo("10.0.0.***");
        assertThat(LogMaskingUtils.maskIpAddress("127.0.0.1"))
                .isEqualTo("127.0.0.***");
    }

    @Test
    @DisplayName("IPv6 주소를 마스킹한다")
    void maskIpAddress_ipv6() {
        assertThat(LogMaskingUtils.maskIpAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334"))
                .isEqualTo("2001:0db8:85a3:0000:****");
        assertThat(LogMaskingUtils.maskIpAddress("::1"))
                .isEqualTo("::1"); // 4섹션 미만이므로 원문 반환
    }

    @Test
    @DisplayName("잘못된 IP 형식은 원문을 반환한다")
    void maskIpAddress_invalid() {
        assertThat(LogMaskingUtils.maskIpAddress("invalid-ip"))
                .isEqualTo("invalid-ip");
        assertThat(LogMaskingUtils.maskIpAddress("192.168.1"))
                .isEqualTo("192.168.1"); // 4섹션 미만
        assertThat(LogMaskingUtils.maskIpAddress("192.168.1.100.200"))
                .isEqualTo("192.168.1.100.200"); // 4섹션 초과
    }

    @Test
    @DisplayName("null이나 빈 문자열은 그대로 반환한다")
    void maskIpAddress_nullOrEmpty() {
        assertThat(LogMaskingUtils.maskIpAddress(null)).isNull();
        assertThat(LogMaskingUtils.maskIpAddress("")).isEmpty();
    }

    @Test
    @DisplayName("User-Agent가 null이면 null을 반환한다")
    void previewUserAgent_null() {
        assertThat(LogMaskingUtils.previewUserAgent(null, 20)).isNull();
    }

    @Test
    @DisplayName("User-Agent 길이가 정확히 maxPrefixLen이면 원문을 반환한다")
    void previewUserAgent_exactLength() {
        String exactUA = "1234567890"; // 10자
        assertThat(LogMaskingUtils.previewUserAgent(exactUA, 10))
                .isEqualTo("1234567890");
    }
}
