package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 헬스체크 서비스
 * 
 * <p>시스템의 주요 컴포넌트 상태를 주기적으로 체크하고
 * 상태 변화 시 알림을 발송합니다.</p>
 * 
 * <p>Issue #15: 실시간 알림 시스템 - 시나리오 2 (서비스 장애 알림)</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final DataSource dataSource;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final MaintenanceModeService maintenanceModeService;
    
    // 이전 상태 저장 (상태 변화 감지용)
    private final Map<String, String> previousStatuses = new HashMap<>();

    /**
     * 전체 시스템 헬스체크 (확장 가능)
     * 
     * <p>매 30초마다 모든 주요 컴포넌트의 상태를 체크합니다.</p>
     * <p>상태 변화 감지 시 알림을 발송합니다.</p>
     * 
     * <p>확장 가능한 구조: 새로운 컴포넌트 추가 시 메서드만 추가하면 됩니다.</p>
     */
    @Scheduled(fixedRate = 30000) // 30초마다 체크
    public void checkAllSystemComponents() {
        // MVP: MySQL만 체크
        checkDatabaseHealth();
        
        // 유지보수 모드 상태 체크
        checkMaintenanceModeHealth();
        
        // 확장: 나중에 추가 가능
        // checkRedisHealth();
        // checkExternalApiHealth();
    }

    /**
     * 데이터베이스 헬스체크
     * 
     * <p>MySQL 데이터베이스 연결 상태를 체크합니다.</p>
     */
    private void checkDatabaseHealth() {
        checkComponentHealth("database", this::performDatabaseHealthCheck);
    }

    /**
     * 유지보수 모드 헬스체크
     * 
     * <p>유지보수 모드 상태를 체크합니다.</p>
     */
    private void checkMaintenanceModeHealth() {
        checkComponentHealth("maintenance-mode", this::performMaintenanceModeHealthCheck);
    }

    /**
     * 컴포넌트 헬스체크 공통 로직 (확장 가능한 패턴)
     * 
     * @param componentName 컴포넌트 이름 (database, redis, external-api 등)
     * @param healthChecker 헬스체크 함수
     */
    private void checkComponentHealth(String componentName, java.util.function.Supplier<String> healthChecker) {
        String currentStatus = healthChecker.get();
        
        // 상태 변화 감지
        String previousStatus = previousStatuses.get(componentName);
        
        if (previousStatus != null && !previousStatus.equals(currentStatus)) {
            log.warn("🔄 {} 상태 변화 감지: {} -> {}", componentName, previousStatus, currentStatus);
            
            // 시스템 레벨 알림 발송
            sendSystemLevelAlert(componentName, previousStatus, currentStatus);
        }
        
        // 현재 상태 저장
        previousStatuses.put(componentName, currentStatus);
    }

    /**
     * 실제 데이터베이스 헬스체크 수행
     * 
     * @return 상태 (HEALTHY, WARNING, CRITICAL)
     */
    private String performDatabaseHealthCheck() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) { // 2초 타임아웃
                log.debug("✅ 데이터베이스 연결 정상");
                return "HEALTHY";
            } else {
                log.warn("⚠️ 데이터베이스 연결 불안정");
                return "WARNING";
            }
        } catch (Exception e) {
            log.error("❌ 데이터베이스 연결 실패: {}", e.getMessage(), e);
            return "CRITICAL";
        }
    }

    /**
     * 실제 유지보수 모드 헬스체크 수행
     * 
     * @return 상태 (HEALTHY, WARNING)
     */
    private String performMaintenanceModeHealthCheck() {
        try {
            boolean isMaintenanceMode = maintenanceModeService.isMaintenanceModeEnabled();
            
            if (isMaintenanceMode) {
                log.debug("🔧 유지보수 모드 활성화됨");
                return "WARNING";
            } else {
                log.debug("✅ 유지보수 모드 비활성화됨");
                return "HEALTHY";
            }
        } catch (Exception e) {
            log.error("❌ 유지보수 모드 상태 확인 실패: {}", e.getMessage(), e);
            return "CRITICAL";
        }
    }

    /**
     * 시스템 레벨 알림 발송 (이벤트 기반)
     * 
     * <p>데이터베이스 장애는 시스템 전체에 영향을 미치므로
     * SUPER_ADMIN 역할을 가진 플랫폼 운영자에게만 알림을 발송합니다.</p>
     * 
     * <p>시나리오 2 요구사항: 데이터베이스 연결 실패 시 긴급 알림</p>
     * 
     * <p>Issue #15 가이드: HealthStatusChangedEvent를 발행하여 MonitoringAlertService에서 처리합니다.</p>
     */
    private void sendSystemLevelAlert(String serviceName, String previousStatus, String currentStatus) {
        try {
            // SUPER_ADMIN 역할을 가진 시스템 관리자 조회
            List<User> systemAdmins = userRepository
                .findActiveUsersByRole(UserRole.SUPER_ADMIN, Status.ACTIVE);
            
            if (systemAdmins.isEmpty()) {
                log.error("❌ 시스템 관리자(SUPER_ADMIN)를 찾을 수 없습니다! 시스템 알림 발송 실패");
                return;
            }
            
            // 첫 번째 시스템 관리자의 테넌트 ID 사용
            User systemAdmin = systemAdmins.get(0);
            String tenantId = systemAdmin.getTenant().getTenantKey();
            
            log.info("🚨 시스템 장애 이벤트 발행: serviceName={}, status={}->{}, admin={}", 
                serviceName, previousStatus, currentStatus, systemAdmin.getName());
            
            // 이벤트 발행 (Issue #15 가이드: 이벤트 기반 알림)
            eventPublisher.publishEvent(
                new HealthStatusChangedEvent(this, serviceName, previousStatus, currentStatus, tenantId)
            );
            
            log.debug("✅ HealthStatusChangedEvent published for service: {}", serviceName);
            
        } catch (Exception e) {
            log.error("❌ 시스템 레벨 이벤트 발행 중 오류 발생", e);
        }
    }

    // =========================================================================
    // 확장 예시: Redis, 외부 API 헬스체크 (나중에 추가)
    // =========================================================================
    
    /**
     * Redis 헬스체크 (나중에 활성화)
     * 
     * <p>Redis 연결 상태를 체크합니다.</p>
     */
    // private void checkRedisHealth() {
    //     checkComponentHealth("redis", this::performRedisHealthCheck);
    // }
    
    /**
     * Redis 헬스체크 수행 (나중에 구현)
     */
    // private String performRedisHealthCheck() {
    //     try {
    //         redisTemplate.getConnectionFactory().getConnection().ping();
    //         return "HEALTHY";
    //     } catch (Exception e) {
    //         log.error("❌ Redis 연결 실패: {}", e.getMessage(), e);
    //         return "CRITICAL";
    //     }
    // }
    
    /**
     * 외부 API 헬스체크 (나중에 활성화)
     */
    // private void checkExternalApiHealth() {
    //     checkComponentHealth("aws-api", this::performAwsApiHealthCheck);
    // }

    // =========================================================================
    // 수동 트리거 및 유틸리티 메서드
    // =========================================================================

    /**
     * 수동 헬스체크 트리거
     * 
     * <p>테스트 또는 수동 체크 시 사용</p>
     * 
     * @return 현재 데이터베이스 상태
     */
    public String triggerHealthCheck() {
        log.info("수동 헬스체크 트리거");
        checkDatabaseHealth();
        return previousStatuses.getOrDefault("database", "UNKNOWN");
    }
    
    /**
     * 특정 컴포넌트의 현재 상태 조회
     * 
     * @param componentName 컴포넌트 이름
     * @return 현재 상태 (HEALTHY, WARNING, CRITICAL, UNKNOWN)
     */
    public String getComponentStatus(String componentName) {
        return previousStatuses.getOrDefault(componentName, "UNKNOWN");
    }
}

