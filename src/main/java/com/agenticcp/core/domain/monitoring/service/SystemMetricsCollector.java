package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.monitoring.dto.SystemMetrics;
import com.agenticcp.core.common.enums.CommonErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 시스템 리소스 메트릭을 수집하는 컴포넌트
 * CPU, 메모리, 디스크 사용량을 실시간으로 수집
 */
@Slf4j
@Component
public class SystemMetricsCollector {

    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    /**
     * 시스템 메트릭 수집
     */
    public SystemMetrics collectSystemMetrics() {
        try {
            log.debug("Collecting system metrics...");
            
            // CPU 사용률 수집
            Double cpuUsage = getCpuUsage();
            
            // 메모리 사용량 수집
            Long memoryUsedMB = getMemoryUsedMB();
            Long memoryTotalMB = getMemoryTotalMB();
            Double memoryUsage = calculateMemoryUsage(memoryUsedMB, memoryTotalMB);
            
            // 디스크 사용량 수집
            Long diskUsedGB = getDiskUsedGB();
            Long diskTotalGB = getDiskTotalGB();
            Double diskUsage = calculateDiskUsage(diskUsedGB, diskTotalGB);
            
            // 시스템 정보 수집
            SystemMetrics.SystemInfo systemInfo = getSystemInfo();
            
            // 메타데이터 구성
            Map<String, Object> metadata = buildMetadata(systemInfo);
            
            return SystemMetrics.builder()
                    .cpuUsage(cpuUsage)
                    .memoryUsage(memoryUsage)
                    .memoryUsedMB(memoryUsedMB)
                    .memoryTotalMB(memoryTotalMB)
                    .diskUsage(diskUsage)
                    .diskUsedGB(diskUsedGB)
                    .diskTotalGB(diskTotalGB)
                    .collectedAt(LocalDateTime.now())
                    .metadata(metadata)
                    .systemInfo(systemInfo)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to collect system metrics", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "시스템 메트릭 수집 중 오류가 발생했습니다.");
        }
    }

    /**
     * CPU 사용률 수집
     */
    private Double getCpuUsage() {
        try {
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                return sunOsBean.getProcessCpuLoad() * 100.0;
            }
            return osBean.getSystemLoadAverage();
        } catch (Exception e) {
            log.warn("Failed to get CPU usage", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "CPU 메트릭을 사용할 수 없습니다.");
        }
    }

    /**
     * 메모리 사용량 수집 (MB)
     */
    private Long getMemoryUsedMB() {
        try {
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed() + 
                             memoryBean.getNonHeapMemoryUsage().getUsed();
            return usedMemory / (1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get memory usage", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메모리 메트릭을 사용할 수 없습니다.");
        }
    }

    /**
     * 메모리 총량 수집 (MB)
     */
    private Long getMemoryTotalMB() {
        try {
            long totalMemory = memoryBean.getHeapMemoryUsage().getMax() + 
                              memoryBean.getNonHeapMemoryUsage().getMax();
            return totalMemory / (1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get total memory", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "메모리 총량을 사용할 수 없습니다.");
        }
    }

    /**
     * 메모리 사용률 계산
     */
    private Double calculateMemoryUsage(Long usedMB, Long totalMB) {
        if (usedMB == null || totalMB == null || totalMB == 0) {
            return null;
        }
        return (usedMB.doubleValue() / totalMB.doubleValue()) * 100.0;
    }

    /**
     * 디스크 사용량 수집 (GB)
     */
    private Long getDiskUsedGB() {
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usedSpace = totalSpace - freeSpace;
            return usedSpace / (1024 * 1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get disk usage", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "디스크 메트릭을 사용할 수 없습니다.");
        }
    }

    /**
     * 디스크 총량 수집 (GB)
     */
    private Long getDiskTotalGB() {
        try {
            File root = new File("/");
            long totalSpace = root.getTotalSpace();
            return totalSpace / (1024 * 1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get total disk space", e);
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, 
                "디스크 총량을 사용할 수 없습니다.");
        }
    }

    /**
     * 디스크 사용률 계산
     */
    private Double calculateDiskUsage(Long usedGB, Long totalGB) {
        if (usedGB == null || totalGB == null || totalGB == 0) {
            return null;
        }
        return (usedGB.doubleValue() / totalGB.doubleValue()) * 100.0;
    }

    /**
     * 시스템 정보 수집
     */
    private SystemMetrics.SystemInfo getSystemInfo() {
        try {
            return SystemMetrics.SystemInfo.builder()
                    .hostname(getHostname())
                    .osName(System.getProperty("os.name"))
                    .osVersion(System.getProperty("os.version"))
                    .javaVersion(System.getProperty("java.version"))
                    .availableProcessors(osBean.getAvailableProcessors())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to get system info", e);
            return SystemMetrics.SystemInfo.builder().build();
        }
    }

    /**
     * 호스트명 수집
     */
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 메타데이터 구성
     */
    private Map<String, Object> buildMetadata(SystemMetrics.SystemInfo systemInfo) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("hostname", systemInfo.getHostname());
        metadata.put("os_name", systemInfo.getOsName());
        metadata.put("os_version", systemInfo.getOsVersion());
        metadata.put("java_version", systemInfo.getJavaVersion());
        metadata.put("available_processors", systemInfo.getAvailableProcessors());
        return metadata;
    }
}
