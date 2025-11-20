package com.agenticcp.core.domain.cloud.port.model.vm;

import lombok.Builder;
import lombok.Getter;

/**
 * VM 삭제 도메인 커맨드
 */
@Getter
@Builder
public class VmDeleteCommand {

    private final String instanceId;
    @Builder.Default
    private boolean force = false;
    private final String reason;
    @Builder.Default
    private boolean createSnapshot = false;
}

