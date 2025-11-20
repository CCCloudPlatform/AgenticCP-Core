package com.agenticcp.core.domain.cloud.port.model.vm;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 업데이트 도메인 커맨드
 */
@Getter
@Builder
public class VmUpdateCommand {

    private final String instanceId;
    private final String instanceType;
    private final String userData;
    private final Map<String, String> tagsToAdd;
    private final Map<String, String> tagsToRemove;
}

