package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이메일 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("이메일 마스킹 전략 테스트")
class EmailMaskingStrategyTest {

    private EmailMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EmailMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 이메일 주소 마스킹")
    void maskNormalEmail() {
        // given
        String email = "john.doe@example.com";

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isEqualTo("j******e@example.com");
    }

    @Test
    @DisplayName("한 글자 사용자명 이메일 마스킹")
    void maskSingleCharUsernameEmail() {
        // given
        String email = "a@example.com";

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isEqualTo("*@example.com");
    }

    @Test
    @DisplayName("@가 없는 문자열 마스킹")
    void maskStringWithoutAt() {
        // given
        String email = "notanemail";

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isEqualTo("****@****.***");
    }

    @Test
    @DisplayName("여러 개의 @가 있는 문자열 마스킹")
    void maskStringWithMultipleAt() {
        // given
        String email = "test@test@example.com";

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isEqualTo("****@****.***");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String email = "";

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String email = null;

        // when
        String result = strategy.mask(email);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.EMAIL);
    }
}
