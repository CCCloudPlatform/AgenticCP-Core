package com.agenticcp.core.common.logging.masking;

import com.agenticcp.core.common.logging.masking.strategy.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * MaskingStrategyProvider 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("MaskingStrategyProvider 테스트")
class MaskingStrategyProviderTest {

    private MaskingStrategyProvider provider;
    private List<MaskingStrategy> strategies;

    @BeforeEach
    void setUp() {
        strategies = Arrays.asList(
                new PasswordMaskingStrategy(),
                new EmailMaskingStrategy(),
                new CreditCardMaskingStrategy(),
                new IpAddressMaskingStrategy(),
                new PhoneNumberMaskingStrategy(),
                new SsnMaskingStrategy(),
                new SecretKeyMaskingStrategy(),
                new TokenMaskingStrategy(),
                new DefaultMaskingStrategy()
        );
        
        provider = new MaskingStrategyProvider(strategies);
    }

    @Test
    @DisplayName("모든 마스킹 전략이 올바르게 등록됨")
    void allStrategiesRegistered() {
        // when & then
        assertThat(provider.getStrategy(MaskingType.PASSWORD)).isInstanceOf(PasswordMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.EMAIL)).isInstanceOf(EmailMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.CREDIT_CARD)).isInstanceOf(CreditCardMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.IP_ADDRESS)).isInstanceOf(IpAddressMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.PHONE_NUMBER)).isInstanceOf(PhoneNumberMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.SSN)).isInstanceOf(SsnMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.SECRET_KEY)).isInstanceOf(SecretKeyMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.TOKEN)).isInstanceOf(TokenMaskingStrategy.class);
        assertThat(provider.getStrategy(MaskingType.DEFAULT)).isInstanceOf(DefaultMaskingStrategy.class);
    }

    @Test
    @DisplayName("기본 전략이 올바르게 설정됨")
    void defaultStrategySet() {
        // when
        MaskingStrategy defaultStrategy = provider.getStrategy(MaskingType.DEFAULT);

        // then
        assertThat(defaultStrategy).isInstanceOf(DefaultMaskingStrategy.class);
        assertThat(defaultStrategy.getType()).isEqualTo(MaskingType.DEFAULT);
    }

    @Test
    @DisplayName("null 타입에 대해 기본 전략 반환")
    void getStrategyWithNullType() {
        // when
        MaskingStrategy strategy = provider.getStrategy(null);

        // then
        assertThat(strategy).isInstanceOf(DefaultMaskingStrategy.class);
    }

    @Test
    @DisplayName("존재하지 않는 타입에 대해 기본 전략 반환")
    void getStrategyWithNonExistentType() {
        // when
        MaskingStrategy strategy = provider.getStrategy(MaskingType.PASSWORD);

        // then
        assertThat(strategy).isInstanceOf(PasswordMaskingStrategy.class);
    }

    @Test
    @DisplayName("기본 전략이 없는 경우 예외 발생")
    void providerWithoutDefaultStrategy() {
        // given
        List<MaskingStrategy> strategiesWithoutDefault = Arrays.asList(
                new PasswordMaskingStrategy(),
                new EmailMaskingStrategy()
        );

        // when & then
        assertThatThrownBy(() -> new MaskingStrategyProvider(strategiesWithoutDefault))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기본 마스킹 전략이 등록되지 않았습니다");
    }

    @Test
    @DisplayName("빈 전략 리스트로 Provider 생성 시 예외 발생")
    void providerWithEmptyStrategyList() {
        // given
        List<MaskingStrategy> emptyStrategies = Arrays.asList();

        // when & then
        assertThatThrownBy(() -> new MaskingStrategyProvider(emptyStrategies))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("기본 마스킹 전략이 등록되지 않았습니다");
    }

    @Test
    @DisplayName("중복된 전략이 있는 경우 첫 번째 전략 유지")
    void providerWithDuplicateStrategies() {
        // given
        List<MaskingStrategy> strategiesWithDuplicate = Arrays.asList(
                new DefaultMaskingStrategy(),
                new DefaultMaskingStrategy() // 중복
        );

        // when
        MaskingStrategyProvider providerWithDuplicate = new MaskingStrategyProvider(strategiesWithDuplicate);

        // then
        MaskingStrategy strategy = providerWithDuplicate.getStrategy(MaskingType.DEFAULT);
        assertThat(strategy).isInstanceOf(DefaultMaskingStrategy.class);
    }

    @Test
    @DisplayName("각 전략의 타입이 올바르게 설정됨")
    void strategyTypesAreCorrect() {
        // when & then
        assertThat(provider.getStrategy(MaskingType.PASSWORD).getType()).isEqualTo(MaskingType.PASSWORD);
        assertThat(provider.getStrategy(MaskingType.EMAIL).getType()).isEqualTo(MaskingType.EMAIL);
        assertThat(provider.getStrategy(MaskingType.CREDIT_CARD).getType()).isEqualTo(MaskingType.CREDIT_CARD);
        assertThat(provider.getStrategy(MaskingType.IP_ADDRESS).getType()).isEqualTo(MaskingType.IP_ADDRESS);
        assertThat(provider.getStrategy(MaskingType.PHONE_NUMBER).getType()).isEqualTo(MaskingType.PHONE_NUMBER);
        assertThat(provider.getStrategy(MaskingType.SSN).getType()).isEqualTo(MaskingType.SSN);
        assertThat(provider.getStrategy(MaskingType.SECRET_KEY).getType()).isEqualTo(MaskingType.SECRET_KEY);
        assertThat(provider.getStrategy(MaskingType.TOKEN).getType()).isEqualTo(MaskingType.TOKEN);
        assertThat(provider.getStrategy(MaskingType.DEFAULT).getType()).isEqualTo(MaskingType.DEFAULT);
    }
}
