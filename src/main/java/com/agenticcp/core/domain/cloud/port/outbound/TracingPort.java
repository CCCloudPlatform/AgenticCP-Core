package com.agenticcp.core.domain.cloud.port.outbound;

import java.util.Map;

public interface TracingPort {
    AutoCloseable startSpan(String name, Map<String, String> tags);
    void tag(String key, String value);
    void event(String name, Map<String, String> attrs);
}
