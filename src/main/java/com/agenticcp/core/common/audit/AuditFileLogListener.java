package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 감사 이벤트를 수신하여 파일로 기록하는 리스너입니다.
 * 동기 방식으로 즉시 파일 로그를 남깁니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditFileLogListener {

    private final AuditLogger auditLogger;

    @EventListener
    public void handleAuditEvent(AuditPublishEvent event) {
        try {
            AuditEventDto auditEventDto = event.getAuditEventDto();
            
            auditLogger.log(auditEventDto);
            log.debug("감사 로그 파일 기록 완료 [Action: {}, RequestId: {}]", 
                     auditEventDto.action(), auditEventDto.requestId());

        } catch (Exception e) {
            log.error("감사 로그 파일 기록 실패 [Action: {}]: {}",
                     event.getAuditEventDto().action(), e.getMessage(), e);
        }
    }
}

