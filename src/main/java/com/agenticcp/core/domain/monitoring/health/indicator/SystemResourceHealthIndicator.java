package com.agenticcp.core.domain.monitoring.health.indicator;

import com.agenticcp.core.domain.monitoring.health.dto.HealthIndicatorResult;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

@Component
public class SystemResourceHealthIndicator implements HealthIndicator {
    
    @Override
    public String getName() {
        return "system";
    }
    
    @Override
    public HealthIndicatorResult check() {
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        double diskUsage = getDiskUsage();
        
        Map<String, Object> details = new HashMap<>();
        details.put("cpuUsage", cpuUsage);
        details.put("memoryUsage", memoryUsage);
        details.put("diskUsage", diskUsage);
        
        if (cpuUsage > 90 || memoryUsage > 90 || diskUsage > 90) {
            return HealthIndicatorResult.critical("System resources are critically high", details);
        } else if (cpuUsage > 80 || memoryUsage > 80 || diskUsage > 80) {
            return HealthIndicatorResult.warning("System resources are high", details);
        } else {
            return HealthIndicatorResult.healthy("System resources are normal", details);
        }
    }
    
    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            return sunOsBean.getProcessCpuLoad() * 100;
        }
        return 0.0;
    }
    
    private double getMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        return (double) usedMemory / maxMemory * 100;
    }
    
    private double getDiskUsage() {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        return (double) usedSpace / totalSpace * 100;
    }
}
