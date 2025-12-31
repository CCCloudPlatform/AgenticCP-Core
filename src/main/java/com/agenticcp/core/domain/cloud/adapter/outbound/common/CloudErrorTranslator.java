package com.agenticcp.core.domain.cloud.adapter.outbound.common;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;

public final class CloudErrorTranslator {
    private CloudErrorTranslator() {}

    public static RuntimeException translate(Throwable t) {
        if (t instanceof BusinessException) {
            return (BusinessException) t;
        }
        
        String msg = t.getMessage() == null ? "" : t.getMessage().toLowerCase();
        if (msg.contains("rate") && msg.contains("limit")) {
            return new BusinessException(CloudErrorCode.API_RATE_LIMIT_EXCEEDED);
        }
        if (msg.contains("timeout")) {
            return new BusinessException(CloudErrorCode.API_TIMEOUT);
        }
        return new BusinessException(CloudErrorCode.CLOUD_PROVIDER_UNAVAILABLE);
    }
}
