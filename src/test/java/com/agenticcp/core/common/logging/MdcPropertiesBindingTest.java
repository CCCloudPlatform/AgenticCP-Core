package com.agenticcp.core.common.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(MdcProperties.class)
@TestPropertySource(properties = {
        "logging.mdc.enabled-keys=requestId,tenantId,clientIp",
        "logging.mdc.request-id-type=uuid",
        "logging.mdc.request-id-prefix=req_",
        "logging.mdc.request-id-length=8",
        "logging.mdc.mask-client-ip=true",
        "logging.mdc.mask-user-agent=true"
})
public class MdcPropertiesBindingTest {

    @Autowired
    MdcProperties props;

    @Test
    @DisplayName("기본 프로퍼티 바인딩을 검증한다")
    void bind_basic() {
        assertThat(props.getEnabledKeys()).containsExactly("requestId","tenantId","clientIp");
        assertThat(props.getRequestIdType()).isEqualTo("uuid");
        assertThat(props.getRequestIdPrefix()).isEqualTo("req_");
        assertThat(props.getRequestIdLength()).isEqualTo(8);
        assertThat(props.isMaskClientIp()).isTrue();
        assertThat(props.isMaskUserAgent()).isTrue();
    }

    @Test
    @DisplayName("null 입력 시 기본값을 적용한다")
    void bind_with_defaults() {
        // 기본값 테스트를 위한 새로운 프로퍼티 설정
        var defaultProps = new MdcProperties(null, null, null, 0, false, false, 0);
        
        assertThat(defaultProps.getEnabledKeys()).containsExactly("requestId","tenantId","clientIp");
        assertThat(defaultProps.getRequestIdType()).isEqualTo("uuid");
        assertThat(defaultProps.getRequestIdPrefix()).isEqualTo("req_");
        assertThat(defaultProps.getRequestIdLength()).isEqualTo(8);
        assertThat(defaultProps.isMaskClientIp()).isFalse();
        assertThat(defaultProps.isMaskUserAgent()).isFalse();
        assertThat(defaultProps.getUserAgentPreviewLength()).isEqualTo(20);
    }

    @Test
    @DisplayName("enabledKeys가 비어도 그대로 반영한다")
    void bind_with_empty_enabled_keys() {
        var emptyProps = new MdcProperties(List.of(), "timestamp", "test_", 12, true, true, 30);
        
        assertThat(emptyProps.getEnabledKeys()).isEmpty();
        assertThat(emptyProps.getRequestIdType()).isEqualTo("timestamp");
        assertThat(emptyProps.getRequestIdPrefix()).isEqualTo("test_");
        assertThat(emptyProps.getRequestIdLength()).isEqualTo(12);
    }

    @Test
    @DisplayName("음수 길이는 기본값 8을 사용한다")
    void bind_with_negative_request_id_length() {
        var negativeProps = new MdcProperties(List.of("requestId"), "uuid", "req_", -5, false, false, 0);
        
        // 음수 길이는 기본값 8로 설정되어야 함
        assertThat(negativeProps.getRequestIdLength()).isEqualTo(8);
    }
}
