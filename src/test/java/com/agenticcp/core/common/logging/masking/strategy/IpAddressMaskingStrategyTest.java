package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IP 주소 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("IP 주소 마스킹 전략 테스트")
class IpAddressMaskingStrategyTest {

    private IpAddressMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new IpAddressMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 IPv4 주소 마스킹")
    void maskNormalIPv4() {
        // given
        String ip = "192.168.1.100";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("192.168.1.***");
    }

    @Test
    @DisplayName("로컬호스트 IPv4 주소 마스킹")
    void maskLocalhostIPv4() {
        // given
        String ip = "127.0.0.1";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("127.0.0.***");
    }

    @Test
    @DisplayName("공인 IP 주소 마스킹")
    void maskPublicIPv4() {
        // given
        String ip = "8.8.8.8";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("8.8.8.***");
    }

    @Test
    @DisplayName("정상적인 IPv6 주소 마스킹")
    void maskNormalIPv6() {
        // given
        String ip = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("2001:0db8:85a3:0000:****");
    }

    @Test
    @DisplayName("축약된 IPv6 주소 마스킹")
    void maskShortenedIPv6() {
        // given
        String ip = "2001:db8:85a3::8a2e:370:7334";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("2001:db8:85a3::****");
    }

    @Test
    @DisplayName("잘못된 IPv4 형식 마스킹")
    void maskInvalidIPv4() {
        // given
        String ip = "192.168.1";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("192.168.1"); // 원본 반환
    }

    @Test
    @DisplayName("잘못된 IPv6 형식 마스킹")
    void maskInvalidIPv6() {
        // given
        String ip = "2001:db8:85a3";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("2001:db8:85a3"); // 원본 반환
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String ip = "";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String ip = null;

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("IP가 아닌 문자열 마스킹")
    void maskNonIpString() {
        // given
        String ip = "notanipaddress";

        // when
        String result = strategy.mask(ip);

        // then
        assertThat(result).isEqualTo("notanipaddress"); // 원본 반환
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.IP_ADDRESS);
    }
}
