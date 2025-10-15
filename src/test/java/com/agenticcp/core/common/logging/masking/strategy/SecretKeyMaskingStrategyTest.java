package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시크릿 키 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("시크릿 키 마스킹 전략 테스트")
class SecretKeyMaskingStrategyTest {

    private SecretKeyMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SecretKeyMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 시크릿 키 마스킹 (긴 키)")
    void maskNormalSecretKey() {
        // given
        String secretKey = "abc123def456ghi789jkl012mno345pqr678stu901vwx234yz";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("abc1-****-****-****-34yz");
    }

    @Test
    @DisplayName("중간 길이 시크릿 키 마스킹")
    void maskMediumSecretKey() {
        // given
        String secretKey = "abc123def456ghi789jkl";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("abc1-****-****-****-9jkl");
    }

    @Test
    @DisplayName("짧은 시크릿 키 마스킹")
    void maskShortSecretKey() {
        // given
        String secretKey = "abc123def";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("abc1-****-3def");
    }

    @Test
    @DisplayName("매우 짧은 시크릿 키 마스킹")
    void maskVeryShortSecretKey() {
        // given
        String secretKey = "abc123";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("****-****-****-****");
    }

    @Test
    @DisplayName("정확히 8자리 시크릿 키 마스킹")
    void maskEightCharSecretKey() {
        // given
        String secretKey = "abc12345";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("abc1-2345");
    }

    @Test
    @DisplayName("특수문자가 포함된 시크릿 키 마스킹")
    void maskSecretKeyWithSpecialCharacters() {
        // given
        String secretKey = "abc123def456ghi789jkl!@#$%^&*()";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("abc1-****-****-****-&*()");
    }

    @Test
    @DisplayName("숫자만 포함된 시크릿 키 마스킹")
    void maskNumericSecretKey() {
        // given
        String secretKey = "12345678901234567890";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("1234-****-****-****-7890");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String secretKey = "";

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String secretKey = null;

        // when
        String result = strategy.mask(secretKey);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.SECRET_KEY);
    }
}
