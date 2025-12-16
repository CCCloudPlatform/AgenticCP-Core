package com.agenticcp.core.domain.cloud.port.model.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 삭제 도메인 커맨드
 * 
 * 멀티 테넌트 환경에서 자격증명 격리를 위해 세션 정보를 포함합니다.
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

    /**
     * AWS 세션 자격증명
     * Management 작업 시 Service에서 획득하여 주입합니다.
     */
    private final CloudSessionCredential session;
}

