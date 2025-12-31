package com.agenticcp.core.domain.cloud.port.model.cdn;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import lombok.Builder;

import java.util.List;

/**
 * 캐시 무효화 생성 명령
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Builder
public record CreateInvalidationCommand(
    String accountScope,
    String distributionId,
    List<String> paths,  // 무효화할 경로 목록 (예: /images/*, /css/*)
    String callerReference,  // 고유 식별자 (중복 방지용)
    CloudSessionCredential session
) {
}

