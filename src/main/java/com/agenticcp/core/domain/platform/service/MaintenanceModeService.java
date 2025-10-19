package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.domain.platform.entity.PlatformHealth;
import com.agenticcp.core.domain.platform.repository.PlatformHealthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 유지보수 모드 관리 서비스
 * 
 * PlatformHealth와 연동하여 유지보수 모드 상태를 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceModeService {

    private final PlatformHealthRepository platformHealthRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    private volatile boolean maintenanceModeEnabled = false;

    /**
     * 유지보수 모드 활성화
     * 
     * @param reason 유지보수 모드 활성화 사유
     */
    @Transactional
    public void enable(String reason) {
        log.info("[MaintenanceModeService] Enabling maintenance mode - reason: {}", reason);
        
        boolean wasEnabled = maintenanceModeEnabled;
        maintenanceModeEnabled = true;
        
        // PlatformHealth에 상태 저장
        saveMaintenanceModeStatus(true, reason);
        
        // 상태 변경 이벤트 발행
        if (!wasEnabled) {
            publishMaintenanceModeEvent(true, reason);
        }
        
        log.info("[MaintenanceModeService] Maintenance mode enabled successfully");
    }

    /**
     * 유지보수 모드 활성화 (기본 메서드)
     */
    public void enable() {
        enable("Manual activation");
    }

    /**
     * 유지보수 모드 비활성화
     */
    @Transactional
    public void disable() {
        log.info("[MaintenanceModeService] Disabling maintenance mode");
        
        boolean wasEnabled = maintenanceModeEnabled;
        maintenanceModeEnabled = false;
        
        // PlatformHealth에 상태 저장
        saveMaintenanceModeStatus(false, null);
        
        // 상태 변경 이벤트 발행
        if (wasEnabled) {
            publishMaintenanceModeEvent(false, "Maintenance completed");
        }
        
        log.info("[MaintenanceModeService] Maintenance mode disabled successfully");
    }

    /**
     * 유지보수 모드 상태 확인
     * 
     * @return 유지보수 모드 활성화 여부
     */
    public boolean isMaintenanceModeEnabled() {
        return maintenanceModeEnabled;
    }

    /**
     * PlatformHealth에 유지보수 모드 상태 저장
     * 
     * @param enabled 유지보수 모드 활성화 여부
     * @param reason 유지보수 모드 사유
     */
    private void saveMaintenanceModeStatus(boolean enabled, String reason) {
        try {
            // 기존 PlatformHealth 레코드 찾기 또는 생성
            PlatformHealth platformHealth = platformHealthRepository.findByServiceName("maintenance-mode")
                    .orElse(PlatformHealth.builder()
                            .serviceName("maintenance-mode")
                            .status(enabled ? PlatformHealth.HealthStatus.WARNING : PlatformHealth.HealthStatus.HEALTHY)
                            .lastCheckTime(LocalDateTime.now())
                            .build());

            // 상태 업데이트
            platformHealth.setMaintenanceMode(enabled);
            platformHealth.setMaintenanceReason(reason);
            platformHealth.setStatus(enabled ? PlatformHealth.HealthStatus.WARNING : PlatformHealth.HealthStatus.HEALTHY);
            platformHealth.setLastCheckTime(LocalDateTime.now());

            platformHealthRepository.save(platformHealth);
            
            log.debug("[MaintenanceModeService] Maintenance mode status saved to PlatformHealth");
        } catch (Exception e) {
            log.error("[MaintenanceModeService] Failed to save maintenance mode status", e);
        }
    }

    /**
     * 유지보수 모드 상태 변경 이벤트 발행
     * 
     * @param enabled 유지보수 모드 활성화 여부
     * @param reason 사유
     */
    private void publishMaintenanceModeEvent(boolean enabled, String reason) {
        try {
            // TODO: MaintenanceModeChangedEvent 구현 필요
            log.info("[MaintenanceModeService] Maintenance mode changed: enabled={}, reason={}", enabled, reason);
        } catch (Exception e) {
            log.error("[MaintenanceModeService] Failed to publish maintenance mode event", e);
        }
    }
}
