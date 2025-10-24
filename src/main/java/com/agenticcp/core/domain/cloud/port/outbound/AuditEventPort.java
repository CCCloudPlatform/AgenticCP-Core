package com.agenticcp.core.domain.cloud.port.outbound;

import java.util.Map;

public interface AuditEventPort {
    void record(String action, String subject, String outcome, Map<String, Object> attributes);
}
