package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;

import java.util.Map;

public interface ResourceTaggingPort {
    void putTags(ResourceIdentity id, Map<String, String> tags);
    Map<String, String> getTags(ResourceIdentity id);
}
