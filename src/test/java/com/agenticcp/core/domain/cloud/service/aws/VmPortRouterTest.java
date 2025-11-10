package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * VmPortRouter 간단 테스트
 */
@ExtendWith(MockitoExtension.class)
class VmPortRouterTest {

    @Test
    void 라우터_인스턴스_생성_테스트() {
        // Given & When
        VmPortRouter router = new VmPortRouter(List.of());

        // Then
        assertThat(router).isNotNull();
    }

    @Test
    void 지원되지_않는_제공업체_예외_발생() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of());

        // When & Then
        assertThatThrownBy(() -> router.vm(ProviderType.AWS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported provider: AWS");
    }

    @Test
    void 지원되는_제공업체_목록_조회() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of());

        // When
        var supportedProviders = router.getSupportedProviders();

        // Then
        assertThat(supportedProviders).isEmpty();
    }

    @Test
    void 제공업체_지원_여부_확인() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of());

        // When & Then
        assertThat(router.isProviderSupported(ProviderType.AWS)).isFalse();
        assertThat(router.isProviderSupported(ProviderType.AZURE)).isFalse();
    }

    @Test
    void 등록된_포트_개수_확인() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of());

        // When
        int portCount = router.getPortCount();

        // Then
        assertThat(portCount).isEqualTo(0);
    }
}
