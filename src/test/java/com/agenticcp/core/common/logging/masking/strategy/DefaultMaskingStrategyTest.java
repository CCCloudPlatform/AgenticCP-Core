package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 기본 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("기본 마스킹 전략 테스트")
class DefaultMaskingStrategyTest {

    private DefaultMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 문자열 마스킹")
    void maskNormalString() {
        // given
        String value = "sensitiveData";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("긴 문자열 마스킹")
    void maskLongString() {
        // given
        String value = "veryLongSensitiveDataThatShouldBeMasked";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("짧은 문자열 마스킹")
    void maskShortString() {
        // given
        String value = "abc";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("숫자 문자열 마스킹")
    void maskNumericString() {
        // given
        String value = "123456789";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("특수문자가 포함된 문자열 마스킹")
    void maskStringWithSpecialCharacters() {
        // given
        String value = "test@#$%^&*()";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String value = "";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String value = null;

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("공백 문자열 마스킹")
    void maskWhitespaceString() {
        // given
        String value = "   ";

        // when
        String result = strategy.mask(value);

        // then
        assertThat(result).isEqualTo("******");
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.DEFAULT);
    }
}
