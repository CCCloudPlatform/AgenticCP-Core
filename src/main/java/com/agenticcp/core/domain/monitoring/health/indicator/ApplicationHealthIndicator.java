package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApplicationHealthIndicator implements HealthIndicator {
    
    @Override
    public String getName() {
        return "application";
    }
    
    @Override
    public HealthIndicatorResult check() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long totalMemory = memoryBean.getHeapMemoryUsage().getMax();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long freeMemory = totalMemory - usedMemory;
        
        Map<String, Object> details = new HashMap<>();
        details.put("totalMemory", totalMemory);
        details.put("usedMemory", usedMemory);
        details.put("freeMemory", freeMemory);
        details.put("threadCount", Thread.activeCount());
        details.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        
        double memoryUsagePercent = (double) usedMemory / totalMemory * 100;
        
        if (memoryUsagePercent > 90) {
            return HealthIndicatorResult.critical("Application memory usage is critically high", details);
        } else if (memoryUsagePercent > 80) {
            return HealthIndicatorResult.warning("Application memory usage is high", details);
        } else {
            return HealthIndicatorResult.healthy("Application is running normally", details);
        }
    }
}
