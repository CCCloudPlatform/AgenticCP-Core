package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전화번호 마스킹 전략 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("전화번호 마스킹 전략 테스트")
class PhoneNumberMaskingStrategyTest {

    private PhoneNumberMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new PhoneNumberMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 휴대폰 번호 마스킹 (하이픈 포함)")
    void maskNormalMobileWithHyphens() {
        // given
        String phone = "010-1234-5678";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("정상적인 휴대폰 번호 마스킹 (하이픈 없음)")
    void maskNormalMobileWithoutHyphens() {
        // given
        String phone = "01012345678";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("공백이 포함된 휴대폰 번호 마스킹")
    void maskMobileWithSpaces() {
        // given
        String phone = "010 1234 5678";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("괄호가 포함된 휴대폰 번호 마스킹")
    void maskMobileWithParentheses() {
        // given
        String phone = "010(1234)5678";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("일반 전화번호 마스킹 (지역번호)")
    void maskLandlineNumber() {
        // given
        String phone = "02-1234-5678";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("021-****-5678");
    }

    @Test
    @DisplayName("짧은 전화번호 마스킹")
    void maskShortPhoneNumber() {
        // given
        String phone = "123456";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("***-****-****");
    }

    @Test
    @DisplayName("긴 전화번호 마스킹")
    void maskLongPhoneNumber() {
        // given
        String phone = "010123456789012";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-9012");
    }

    @Test
    @DisplayName("특수문자가 포함된 전화번호 마스킹")
    void maskPhoneWithSpecialCharacters() {
        // given
        String phone = "010-1234-5678!@#";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String phone = "";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String phone = null;

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("숫자가 아닌 문자만 포함된 문자열 마스킹")
    void maskNonNumericString() {
        // given
        String phone = "abcdefghijkl";

        // when
        String result = strategy.mask(phone);

        // then
        assertThat(result).isEqualTo("***-****-****");
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.PHONE_NUMBER);
    }
}
