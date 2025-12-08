package com.agenticcp.core.domain.monitoring.service;

import com.agenticcp.core.common.enums.Status;
import com.agenticcp.core.common.enums.UserRole;
import com.agenticcp.core.domain.platform.service.MaintenanceModeService;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 헬스체크 서비스 테스트
 * 
 * <p>시나리오 2: 서비스 장애 알림 테스트</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthCheckService 테스트")
class HealthCheckServiceTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MaintenanceModeService maintenanceModeService;

    @Mock
    private Connection connection;

    @InjectMocks
    private HealthCheckService healthCheckService;

    private User testSuperAdmin;
    private Tenant testAdminTenant;

    @BeforeEach
    void setUp() throws Exception {
        // 시스템 관리자 테넌트 설정
        testAdminTenant = Tenant.builder()
                .tenantKey("admin-tenant")
                .tenantName("시스템 관리 테넌트")
                .status(Status.ACTIVE)
                .build();
        setId(testAdminTenant, 1L);
        
        // SUPER_ADMIN 사용자 설정
        testSuperAdmin = User.builder()
                .username("superadmin")
                .email("admin@agenticcp.com")
                .name("플랫폼 운영자")
                .role(UserRole.SUPER_ADMIN)
                .status(Status.ACTIVE)
                .build();
        testSuperAdmin.setTenant(testAdminTenant);
        setId(testSuperAdmin, 1L);
        
        // Mock 기본 동작 설정
        lenient().when(userRepository.findActiveUsersByRole(UserRole.SUPER_ADMIN, Status.ACTIVE))
                .thenReturn(List.of(testSuperAdmin));
        lenient().when(maintenanceModeService.isMaintenanceModeEnabled())
                .thenReturn(false);
    }

    /**
     * Reflection을 사용하여 BaseEntity의 id 필드 설정
     */
    private void setId(Object entity, Long id) throws Exception {
        Class<?> currentClass = entity.getClass();
        java.lang.reflect.Field idField = null;
        
        // BaseEntity 또는 TenantAwareEntity에서 id 필드 찾기
        while (currentClass != null && !currentClass.equals(Object.class)) {
            try {
                idField = currentClass.getDeclaredField("id");
                break;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        if (idField != null) {
            idField.setAccessible(true);
            idField.set(entity, id);
        }
    }

    @Nested
    @DisplayName("데이터베이스 헬스체크 테스트")
    class DatabaseHealthCheckTest {

        @Test
        @DisplayName("DB 연결 실패 시 CRITICAL 상태 감지")
        void checkAllSystemComponents_WhenDatabaseFails_DetectsCriticalStatus() throws Exception {
            // Given: DB 연결 실패
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

            // When: 첫 번째 헬스체크 (상태 저장)
            healthCheckService.checkAllSystemComponents();

            // Then: 알림 발송 안 됨 (이전 상태 없음)
            verify(eventPublisher, never()).publishEvent(any());

            // When: 두 번째 헬스체크에서도 실패 (상태 변화 없음)
            healthCheckService.checkAllSystemComponents();

            // Then: 여전히 알림 발송 안 됨 (CRITICAL → CRITICAL, 변화 없음)
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("HEALTHY → CRITICAL 변화 시 시스템 관리자에게 긴급 알림")
        void checkAllSystemComponents_WhenStatusChangesToCritical_SendsUrgentAlert() throws Exception {
            // Given: 첫 번째 헬스체크 - 정상
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            
            // When: 첫 번째 헬스체크 (HEALTHY 상태 저장)
            healthCheckService.checkAllSystemComponents();
            
            // Then: 알림 발송 안 됨 (이전 상태 없음)
            verify(eventPublisher, never()).publishEvent(any());
            
            // Given: 두 번째 헬스체크 - 연결 실패
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection timeout"));
            
            // When: 두 번째 헬스체크 (HEALTHY → CRITICAL)
            healthCheckService.checkAllSystemComponents();
            
            // Then: 시스템 관리자에게 이벤트 발행
            verify(eventPublisher, times(1)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
            
            // Then: SUPER_ADMIN 조회 확인
            verify(userRepository, times(1)).findActiveUsersByRole(UserRole.SUPER_ADMIN, Status.ACTIVE);
        }

        @Test
        @DisplayName("CRITICAL → HEALTHY 복구 시 복구 알림 발송")
        void checkAllSystemComponents_WhenDatabaseRecovers_SendsRecoveryAlert() throws Exception {
            // Given: 첫 번째 - 연결 실패
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            healthCheckService.checkAllSystemComponents();
            
            // Given: 두 번째 - 복구됨
            reset(dataSource, connection);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            
            // When: 헬스체크 (CRITICAL → HEALTHY)
            healthCheckService.checkAllSystemComponents();
            
            // Then: 복구 알림 발송
            verify(eventPublisher, times(1)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
        }

        @Test
        @DisplayName("DB 연결 불안정 시 WARNING 상태 감지 및 알림")
        void checkAllSystemComponents_WhenDatabaseUnstable_DetectsWarningStatus() throws Exception {
            // Given: 첫 번째 - 정상
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            
            // Given: 두 번째 - 불안정 (연결은 되지만 isValid false)
            reset(connection);
            when(connection.isValid(2)).thenReturn(false);
            
            // When: 헬스체크 (HEALTHY → WARNING)
            healthCheckService.checkAllSystemComponents();
            
            // Then: WARNING 알림 발송
            verify(eventPublisher, times(1)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
        }

        @Test
        @DisplayName("동일 상태 유지 시 알림 미발송")
        void checkAllSystemComponents_WhenStatusRemainsSame_DoesNotSendAlert() throws Exception {
            // Given: 첫 번째 - CRITICAL
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            healthCheckService.checkAllSystemComponents();
            
            // When: 두 번째 - 여전히 CRITICAL
            healthCheckService.checkAllSystemComponents();
            
            // Then: 알림 발송 안 됨 (상태 변화 없음)
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("여러 상태 변화 시 각각 알림 발송")
        void checkAllSystemComponents_WhenMultipleStatusChanges_SendsAlertForEach() throws Exception {
            // 1. HEALTHY 상태 저장
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            
            // 2. HEALTHY → WARNING
            reset(connection);
            when(connection.isValid(2)).thenReturn(false);
            healthCheckService.checkAllSystemComponents();
            verify(eventPublisher, times(1)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
            
            // 3. WARNING → CRITICAL
            reset(dataSource);
            when(dataSource.getConnection()).thenThrow(new SQLException("Failed"));
            healthCheckService.checkAllSystemComponents();
            verify(eventPublisher, times(2)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
            
            // 4. CRITICAL → HEALTHY (복구)
            reset(dataSource, connection);
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            verify(eventPublisher, times(3)).publishEvent(
                argThat(event -> event instanceof com.agenticcp.core.domain.monitoring.event.HealthStatusChangedEvent)
            );
        }
    }

    @Nested
    @DisplayName("알림 발송 테스트")
    class AlertNotificationTest {

        @Test
        @DisplayName("SUPER_ADMIN 없으면 알림 발송 실패")
        void checkAllSystemComponents_WhenNoSuperAdminExists_DoesNotSendAlert() throws Exception {
            // Given: SUPER_ADMIN 없음
            when(userRepository.findActiveUsersByRole(UserRole.SUPER_ADMIN, Status.ACTIVE))
                    .thenReturn(List.of());
            
            // Given: HEALTHY 상태 저장
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            
            // Given: CRITICAL 상태로 변화
            reset(dataSource);
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            
            // When: 헬스체크 (HEALTHY → CRITICAL)
            healthCheckService.checkAllSystemComponents();
            
            // Then: 알림 발송 시도하지 않음 (SUPER_ADMIN 없음)
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("시스템 장애는 SUPER_ADMIN만 알림 받음")
        void checkAllSystemComponents_WhenStatusChanges_OnlySendsToSuperAdmin() throws Exception {
            // Given: HEALTHY 저장
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            
            // Given: CRITICAL 상태로 변화
            reset(dataSource);
            when(dataSource.getConnection()).thenThrow(new SQLException("Failed"));
            
            // When: 헬스체크
            healthCheckService.checkAllSystemComponents();
            
            // Then: SUPER_ADMIN 역할로만 조회
            verify(userRepository, times(1)).findActiveUsersByRole(
                eq(UserRole.SUPER_ADMIN),  // ✅ SUPER_ADMIN만
                eq(Status.ACTIVE)
            );
            
            // Then: TENANT_ADMIN은 조회 안 함
            verify(userRepository, never()).findActiveUsersByRole(
                eq(UserRole.TENANT_ADMIN),
                any()
            );
        }

        @Test
        @DisplayName("알림 발송 실패해도 헬스체크는 계속됨")
        void checkAllSystemComponents_WhenAlertFails_ContinuesHealthCheck() throws Exception {
            // Given: HEALTHY 상태 저장
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            healthCheckService.checkAllSystemComponents();
            
            // Given: CRITICAL 상태로 변화 + 이벤트 발행 실패
            reset(dataSource);
            when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));
            doThrow(new RuntimeException("Event publishing failed"))
                .when(eventPublisher)
                .publishEvent(any());
            
            // When & Then: 예외 발생하지 않음
            assertThatCode(() -> {
                healthCheckService.checkAllSystemComponents();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("수동 헬스체크 테스트")
    class ManualHealthCheckTest {

        @Test
        @DisplayName("수동 헬스체크 트리거 기능")
        void triggerHealthCheck_WhenDatabaseHealthy_ReturnsHealthy() throws Exception {
            // Given: DB 정상
            when(dataSource.getConnection()).thenReturn(connection);
            when(connection.isValid(2)).thenReturn(true);
            
            // When: 수동 트리거
            String status = healthCheckService.triggerHealthCheck();
            
            // Then: HEALTHY 상태 반환
            assertThat(status).isEqualTo("HEALTHY");
        }
    }
}
