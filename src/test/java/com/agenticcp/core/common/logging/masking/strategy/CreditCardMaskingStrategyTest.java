package com.agenticcp.core.common.logging.masking.strategy;

import com.agenticcp.core.common.logging.masking.MaskingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 신용카드 마스킹 전략 테스트 (PCI DSS 표준 준수)
 * 
 * PCI DSS 표준에 따라 앞 6자리와 뒤 4자리만 표시하고 중간은 XX로 마스킹 처리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("신용카드 마스킹 전략 테스트")
class CreditCardMaskingStrategyTest {

    private CreditCardMaskingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new CreditCardMaskingStrategy();
    }

    @Test
    @DisplayName("정상적인 신용카드 번호 마스킹 (하이픈 포함)")
    void maskNormalCreditCardWithHyphens() {
        // given
        String creditCard = "1234-5678-9012-3456";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("123456-XX-XXXX-3456");
    }

    @Test
    @DisplayName("정상적인 신용카드 번호 마스킹 (하이픈 없음)")
    void maskNormalCreditCardWithoutHyphens() {
        // given
        String creditCard = "1234567890123456";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("123456XXXXXX3456");
    }

    @Test
    @DisplayName("공백이 포함된 신용카드 번호 마스킹")
    void maskCreditCardWithSpaces() {
        // given
        String creditCard = "1234 5678 9012 3456";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("123456 XX XXXX 3456");
    }

    @Test
    @DisplayName("짧은 신용카드 번호 마스킹")
    void maskShortCreditCard() {
        // given
        String creditCard = "1234567";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("XXXX-XXXX-XXXX-XXXX");
    }

    @Test
    @DisplayName("긴 신용카드 번호 마스킹")
    void maskLongCreditCard() {
        // given
        String creditCard = "12345678901234567890";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("123456XXXXXX7890");
    }

    @Test
    @DisplayName("빈 문자열 마스킹")
    void maskEmptyString() {
        // given
        String creditCard = "";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("");
    }

    @Test
    @DisplayName("null 값 마스킹")
    void maskNullValue() {
        // given
        String creditCard = null;

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("마스킹 타입 확인")
    void getType() {
        // when
        MaskingType type = strategy.getType();

        // then
        assertThat(type).isEqualTo(MaskingType.CREDIT_CARD);
    }

    @Test
    @DisplayName("PCI DSS 표준 - 하이픈 형식 테스트")
    void maskPciDssStandardHyphenFormat() {
        // given
        String creditCard = "4111-1111-1111-1111";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("411111-XX-XXXX-1111");
    }

    @Test
    @DisplayName("PCI DSS 표준 - 공백 형식 테스트")
    void maskPciDssStandardSpaceFormat() {
        // given
        String creditCard = "5522 2222 2222 2222";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("552222 XX XXXX 2222");
    }

    @Test
    @DisplayName("PCI DSS 표준 - 공백 없는 형식 테스트")
    void maskPciDssStandardNoSpaceFormat() {
        // given
        String creditCard = "3782567890128256";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("378256XXXXXX8256");
    }

    @Test
    @DisplayName("8자리 미만 카드 번호는 전체 마스킹")
    void maskCardNumberLessThanMinimum() {
        // given
        String creditCard = "1234567";

        // when
        String result = strategy.mask(creditCard);

        // then
        assertThat(result).isEqualTo("XXXX-XXXX-XXXX-XXXX");
    }

}
