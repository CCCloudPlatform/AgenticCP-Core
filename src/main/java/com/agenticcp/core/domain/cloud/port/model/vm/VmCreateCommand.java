package com.agenticcp.core.domain.cloud.port.model.vm;

import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 생성 도메인 커맨드
 *
 * 컨트롤러 요청 DTO와 분리된 애플리케이션 내부 명령 모델입니다.
 */
@Getter
@Builder
public class VmCreateCommand {

    private final String imageId;
    private final String instanceType;
    private final String keyName;
    private final String securityGroupId;
    private final String subnetId;
    private final String userData;
    private final Map<String, String> tags;
    @Builder.Default
    private int minCount = 1;
    @Builder.Default
    private int maxCount = 1;
}

