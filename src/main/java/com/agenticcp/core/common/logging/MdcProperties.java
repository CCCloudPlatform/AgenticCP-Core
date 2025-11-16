package com.agenticcp.core.common.logging;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * MDC 로깅 설정을 주입받는 설정 프로퍼티 클래스입니다.
 * 사용 여부, 요청 ID 생성 규칙, 마스킹 옵션 등을 제어합니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Getter
@ConfigurationProperties(prefix = "logging.mdc")
public class MdcProperties {

    private final List<String> enabledKeys;
    private final String requestIdType;
    private final String requestIdPrefix;
    private final int requestIdLength;
    private final boolean maskClientIp;
    private final boolean maskUserAgent;
    private final int userAgentPreviewLength;

    public MdcProperties(List<String> enabledKeys, String requestIdType, String requestIdPrefix,
                         int requestIdLength, boolean maskClientIp, boolean maskUserAgent, 
                         int userAgentPreviewLength) {

        this.enabledKeys = enabledKeys != null ? enabledKeys : List.of("requestId","tenantId","clientIp");
        this.requestIdType = requestIdType != null ? requestIdType : "uuid";
        this.requestIdPrefix = requestIdPrefix != null ? requestIdPrefix : "req_";
        this.requestIdLength = requestIdLength > 0 ? requestIdLength : 8;
        this.maskClientIp = maskClientIp;
        this.maskUserAgent = maskUserAgent;
        this.userAgentPreviewLength = userAgentPreviewLength > 0 ? userAgentPreviewLength : 20;
    }
}
