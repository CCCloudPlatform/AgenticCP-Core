package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicator {
    
    private final DataSource dataSource;
    
    @Override
    public String getName() {
        return "database";
    }
    
    @Override
    public HealthIndicatorResult check() {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            if (isValid) {
                return HealthIndicatorResult.healthy("Database connection is valid");
            } else {
                return HealthIndicatorResult.critical("Database connection is invalid");
            }
        } catch (SQLException e) {
            return HealthIndicatorResult.critical("Database connection failed: " + e.getMessage());
        }
    }
}
