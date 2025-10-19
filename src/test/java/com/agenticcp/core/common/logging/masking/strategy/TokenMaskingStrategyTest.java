package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 토큰 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("토큰 마스킹 전략 테스트")
class TokenMaskingStrategyTest {

    private TokenMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new TokenMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 JWT 토큰 마스킹")
    void maskNormalJwtToken() {
        // given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).contains("eyJh-****-****-VCJ9.eyJz-****-****-IyfQ.SflK-****-****-sw5c");
        assertThat(result).contains(".");
        assertThat(result.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("짧은 JWT 토큰 마스킹")
    void maskShortJwtToken() {
        // given
        String token = "eyJ.eyJ.eyJ";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("****.****.****");
    }

    @Test
    @DisplayName("일반 토큰 마스킹 (JWT 형식이 아닌 경우)")
    void maskNormalToken() {
        // given
        String token = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("abc1-****-****-34yz");
    }

    @Test
    @DisplayName("짧은 일반 토큰 마스킹")
    void maskShortToken() {
        // given
        String token = "abc123def";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("abc1-****-****-3def");
    }

    @Test
    @DisplayName("매우 짧은 토큰 마스킹")
    void maskVeryShortToken() {
        // given
        String token = "abc123";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("****-****-****-****");
    }

    @Test
    @DisplayName("정확히 8자리 토큰 마스킹")
    void maskEightCharToken() {
        // given
        String token = "abc12345";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("abc1-****-****-2345");
    }

    @Test
    @DisplayName("잘못된 JWT 형식 토큰 마스킹 (점이 2개)")
    void maskInvalidJwtToken() {
        // given
        String token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("eyJh-****-****-IyfQ");
    }

    @Test
    @DisplayName("특수문자가 포함된 토큰 마스킹")
    void maskTokenWithSpecialCharacters() {
        // given
        String token = "abc123def456ghi789jkl!@#$%^&*()";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("abc1-****-****-&*()");
    }

    @Test
    @DisplayName("숫자만 포함된 토큰 마스킹")
    void maskNumericToken() {
        // given
        String token = "12345678901234567890";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("1234-****-****-7890");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String token = "";

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String token = null;

        // when
        String result = strategy.mask(token);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.TOKEN);
    }
}
