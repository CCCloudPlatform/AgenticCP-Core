package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주민등록번호 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("주민등록번호 마스킹 전략 테스트")
class SsnMaskingStrategyTest {

    private SsnMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SsnMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 주민등록번호 마스킹 (하이픈 포함)")
    void maskNormalSsnWithHyphen() {
        // given
        String ssn = "900101-1234567";

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isEqualTo("900101-1******");
    }

    @Test
    @DisplayName("정상적인 주민등록번호 마스킹 (하이픈 없음)")
    void maskNormalSsnWithoutHyphen() {
        // given
        String ssn = "9001011234567";

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isEqualTo("9001011******");
    }

    @Test
    @DisplayName("공백이 포함된 주민등록번호 마스킹")
    void maskSsnWithSpaces() {
        // given
        String ssn = "900101 1234567";

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isEqualTo("9001011******");
    }

    @Test
    @DisplayName("잘못된 형식의 주민등록번호 마스킹")
    void maskInvalidSsn() {
        // given
        String ssn = "90010-123456";

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isEqualTo("***-*******");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String ssn = "";

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String ssn = null;

        // when
        String result = strategy.mask(ssn);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.SSN);
    }
}
