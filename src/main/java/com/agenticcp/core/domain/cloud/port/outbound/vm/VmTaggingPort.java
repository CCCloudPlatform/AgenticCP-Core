package com.agenticcp.core.domain.cloud.port.outbound.vm;

import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import java.util.Map;

/**
 * VM 태그 관리 책임 포트
 * 
 * 모든 Tagging 작업에서 세션 자격증명을 전달받아 사용합니다.
 * PR #142의 JIT 세션 관리 패턴을 따릅니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
public interface VmTaggingPort {

    /**
     * VM 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     * @param session 세션 자격증명
     */
    void addTags(String instanceId, Map<String, String> tags, CloudSessionCredential session);

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     * @param session 세션 자격증명
     */
    void removeTags(String instanceId, Map<String, String> tagKeys, CloudSessionCredential session);

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param session 세션 자격증명
     * @return 태그 맵
     */
    Map<String, String> getTags(String instanceId, CloudSessionCredential session);
}
