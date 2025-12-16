package com.agenticcp.core.domain.cloud.port.model.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 업데이트 도메인 커맨드
 * 
 * 멀티 테넌트 환경에서 자격증명 격리를 위해 세션 정보를 포함합니다.
 */
@Getter
@Builder
public class VmUpdateCommand {

    private final String instanceId;
    private final String instanceType;
    private final String userData;
    private final Map<String, String> tagsToAdd;
    private final Map<String, String> tagsToRemove;

    /**
     * AWS 세션 자격증명
     * Management 작업 시 Service에서 획득하여 주입합니다.
     */
    private final CloudSessionCredential session;
}

