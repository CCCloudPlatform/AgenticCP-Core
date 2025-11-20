package com.agenticcp.core.domain.cloud.capability;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CapabilityGuard {

    private final CapabilityRegistry capabilityRegistry;

    public void ensureSupported(ProviderType providerType, String serviceKey, String resourceType, Operation op) {
        CspCapability cap = Optional.ofNullable(capabilityRegistry.get(providerType, serviceKey, resourceType))
                .orElseThrow(() -> new BusinessException(CloudErrorCode.CAPABILITY_NOT_DEFINED));
        switch (op) {
            case START -> { if (!cap.isSupportsStart()) throw new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION); }
            case STOP -> { if (!cap.isSupportsStop()) throw new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION); }
            case TERMINATE -> { if (!cap.isSupportsTerminate()) throw new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION); }
            case TAGGING -> { if (!cap.isSupportsTagging()) throw new BusinessException(CloudErrorCode.UNSUPPORTED_OPERATION); }
            default -> { }
        }
    }

    public enum Operation { CREATE, UPDATE, START, STOP, TERMINATE, TAGGING }
}
