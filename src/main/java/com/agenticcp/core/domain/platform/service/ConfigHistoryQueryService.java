package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.dto.ConfigHistoryResponse;
import com.agenticcp.core.domain.security.entity.AuditLog;
import com.agenticcp.core.domain.security.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigHistoryQueryService {

    private final AuditLogRepository auditLogRepository;

    public Page<ConfigHistoryResponse> getHistory(String configKey, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100));
        Page<AuditLog> logs = auditLogRepository
                .findByResourceTypeAndResourceIdAndEventTypeOrderByEventTimestampDesc(
                        "PlatformConfig",
                        configKey,
                        AuditLog.EventType.CONFIGURATION_CHANGE,
                        pageable
                );

        return logs.map(log -> new ConfigHistoryResponse(
                log.getAction(),
                log.getUser() != null ? String.valueOf(log.getUser().getId()) : null,
                null, // reason: details JSON에서 추출은 후속 커밋에서 처리
                null, // valueType: details JSON에서 추출은 후속 커밋에서 처리
                null, // prevValue: details JSON에서 추출은 후속 커밋에서 처리(마스킹)
                null, // newValue: details JSON에서 추출은 후속 커밋에서 처리(마스킹)
                log.getEventTimestamp()
        ));
    }
}



