package com.agenticcp.core.domain.cloud.port.model.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * VM 생성 도메인 커맨드
 *
 * 컨트롤러 요청 DTO와 분리된 애플리케이션 내부 명령 모델입니다.
 * 멀티 테넌트 환경에서 자격증명 격리를 위해 세션 정보를 포함합니다.
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

    /**
     * AWS 세션 자격증명
     * Management 작업 시 Service에서 획득하여 주입합니다.
     */
    private final CloudSessionCredential session;
}

