package com.agenticcp.core.domain.cloud.port.outbound;

import com.agenticcp.core.domain.cloud.port.model.ResourceIdentity;

public interface ResourceLifecyclePort {
    void start(ResourceIdentity id);
    void stop(ResourceIdentity id);
    void terminate(ResourceIdentity id);
}
