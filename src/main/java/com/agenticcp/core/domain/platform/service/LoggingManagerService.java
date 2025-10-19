package com.agenticcp.core.domain.platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 로깅 관리 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class LoggingManagerService {

    /**
     * 로깅 레벨 업데이트
     * 
     * @param level 로깅 레벨
     */
    public void updateLoggingLevel(String level) {
        log.info("[LoggingManagerService] Updating logging level to {}", level);
        // 실제 로깅 레벨 업데이트 로직 구현
    }
}
