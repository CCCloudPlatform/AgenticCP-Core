package com.agenticcp.core.domain.cloud.port.outbound.vm;

import java.util.Map;

/**
 * VM 태그 관리 책임 포트
 */
public interface VmTaggingPort {

    void addTags(String instanceId, Map<String, String> tags);

    void removeTags(String instanceId, Map<String, String> tagKeys);

    Map<String, String> getTags(String instanceId);
}

