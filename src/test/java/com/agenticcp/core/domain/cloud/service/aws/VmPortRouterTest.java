package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.service.vm.VmPortRouter;
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
        VmPortRouter router = new VmPortRouter(List.of(), List.of(), List.of());

        // Then
        assertThat(router).isNotNull();
    }

    @Test
    void discovery_지원되지_않는_제공업체_예외_발생() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of(), List.of(), List.of());

        // When & Then
        assertThatThrownBy(() -> router.discovery(ProviderType.AWS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("지원하지 않는 프로바이더입니다");
    }

    @Test
    void lifecycle_지원되지_않는_제공업체_예외_발생() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of(), List.of(), List.of());

        // When & Then
        assertThatThrownBy(() -> router.lifecycle(ProviderType.AWS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("지원하지 않는 프로바이더입니다");
    }

    @Test
    void tagging_지원되지_않는_제공업체_예외_발생() {
        // Given
        VmPortRouter router = new VmPortRouter(List.of(), List.of(), List.of());

        // When & Then
        assertThatThrownBy(() -> router.tagging(ProviderType.AWS))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("태그 관리를 지원하지 않는 프로바이더입니다");
    }
}
