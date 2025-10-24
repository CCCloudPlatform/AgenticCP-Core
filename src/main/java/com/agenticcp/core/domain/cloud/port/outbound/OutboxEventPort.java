package com.agenticcp.core.domain.cloud.port.outbound;

import java.util.Map;

public interface OutboxEventPort {
    void publish(String topic, String key, Map<String, Object> payload);
}
