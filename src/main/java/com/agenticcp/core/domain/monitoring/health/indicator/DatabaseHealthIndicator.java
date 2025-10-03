package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터베이스 헬스체크 인디케이터
 * 
 * 데이터베이스 연결 상태를 확인하는 헬스체크 인디케이터입니다.
 * 연결 유효성 검사와 응답 시간을 측정하여 상태를 반환합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public HealthIndicatorResult check() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> details = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5); // 5초 타임아웃
            long responseTime = System.currentTimeMillis() - startTime;
            
            details.put("responseTime", responseTime);
            details.put("connectionValid", isValid);
            
            if (isValid) {
                log.debug("Database health check passed in {}ms", responseTime);
                return HealthIndicatorResult.healthy("Database connection is valid", details);
            } else {
                log.warn("Database connection is invalid");
                return HealthIndicatorResult.critical("Database connection is invalid", details);
            }
        } catch (SQLException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            details.put("responseTime", responseTime);
            details.put("error", e.getMessage());
            
            log.error("Database connection failed: {}", e.getMessage());
            return HealthIndicatorResult.critical("Database connection failed: " + e.getMessage(), details);
        }
    }
}
