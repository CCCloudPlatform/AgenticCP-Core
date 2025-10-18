package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비밀번호 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("비밀번호 마스킹 전략 테스트")
class PasswordMaskingStrategyTest {

    private PasswordMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PasswordMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 비밀번호 마스킹")
    void maskNormalPassword() {
        // given
        String password = "mySecretPassword123";

        // when
        String result = strategy.mask(password);

        // then
        assertThat(result).isEqualTo("********");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String password = "";

        // when
        String result = strategy.mask(password);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String password = null;

        // when
        String result = strategy.mask(password);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.PASSWORD);
    }
}
