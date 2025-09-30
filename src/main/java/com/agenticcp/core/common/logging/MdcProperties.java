package com.agenticcp.core.common.logging;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

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
