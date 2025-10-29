package com.agenticcp.core.domain.cloud.capability;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CspCapability {
    boolean supportsStart;
    boolean supportsStop;
    boolean supportsTerminate;
    boolean supportsTagging;
    boolean supportsListByTag;
}
