package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.PlatformComplianceConfig;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagPolicyValidator 단위 테스트
 * 
 * 기능 플래그 정책 검증기의 핵심 기능을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagPolicyValidator 테스트")
class FeatureFlagPolicyValidatorTest {

    @Mock
    private PlatformComplianceService complianceService;

    @InjectMocks
    private FeatureFlagPolicyValidator policyValidator;

    private PlatformComplianceConfig defaultConfig;
    private FeatureFlag testFlag;

    @BeforeEach
    void setUp() {
        // 기본 컴플라이언스 설정 생성 (requiredApprovalSeverity = "HIGH")
        defaultConfig = PlatformComplianceConfig.builder()
                .configKey("PLATFORM_COMPLIANCE_CONFIG")
                .requiredApprovalSeverity("HIGH")
                .allowAutoApproval(false)
                .enableViolationAlerts(true)
                .reportFormat("JSON")
                .build();

        // 테스트용 FeatureFlag 생성
        testFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Test Flag")
                .severity(FeatureFlagSeverity.HIGH)
                .build();

        // 기본 설정 반환
        lenient().when(complianceService.getConfig()).thenReturn(defaultConfig);
    }

    @Test
    @DisplayName("HIGH 심각도 플래그는 승인이 필요함")
    void testRequiresApproval_HighSeverity() {
        // Given
        FeatureFlagSeverity severity = FeatureFlagSeverity.HIGH;

        // When
        boolean result = policyValidator.requiresApproval(severity);

        // Then
        assertTrue(result);
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("CRITICAL 심각도 플래그는 승인이 필요함")
    void testRequiresApproval_CriticalSeverity() {
        // Given
        FeatureFlagSeverity severity = FeatureFlagSeverity.CRITICAL;

        // When
        boolean result = policyValidator.requiresApproval(severity);

        // Then
        assertTrue(result);
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("LOW 심각도 플래그는 승인이 필요 없음")
    void testRequiresApproval_LowSeverity() {
        // Given
        FeatureFlagSeverity severity = FeatureFlagSeverity.LOW;

        // When
        boolean result = policyValidator.requiresApproval(severity);

        // Then
        assertFalse(result);
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("MEDIUM 심각도 플래그는 승인이 필요 없음")
    void testRequiresApproval_MediumSeverity() {
        // Given
        FeatureFlagSeverity severity = FeatureFlagSeverity.MEDIUM;

        // When
        boolean result = policyValidator.requiresApproval(severity);

        // Then
        assertFalse(result);
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("null 심각도는 승인이 필요 없음")
    void testRequiresApproval_NullSeverity() {
        // Given
        FeatureFlagSeverity severity = null;

        // When
        boolean result = policyValidator.requiresApproval(severity);

        // Then
        assertFalse(result);
        verify(complianceService, never()).getConfig();
    }

    @Test
    @DisplayName("CRITICAL 심각도 설정에서 CRITICAL만 승인 필요")
    void testRequiresApproval_CriticalConfig() {
        // Given
        PlatformComplianceConfig criticalConfig = PlatformComplianceConfig.builder()
                .configKey("PLATFORM_COMPLIANCE_CONFIG")
                .requiredApprovalSeverity("CRITICAL")
                .build();

        when(complianceService.getConfig()).thenReturn(criticalConfig);

        // When & Then
        assertFalse(policyValidator.requiresApproval(FeatureFlagSeverity.LOW));
        assertFalse(policyValidator.requiresApproval(FeatureFlagSeverity.MEDIUM));
        assertFalse(policyValidator.requiresApproval(FeatureFlagSeverity.HIGH));
        assertTrue(policyValidator.requiresApproval(FeatureFlagSeverity.CRITICAL));
    }

    @Test
    @DisplayName("LOW 심각도 플래그 변경 - 승인 없이도 허용")
    void testValidateFlagChange_LowSeverity_NoApprovalNeeded() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.LOW);
        boolean hasApproval = false;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("HIGH 심각도 플래그 변경 - 승인 없으면 예외 발생")
    void testValidateFlagChange_HighSeverity_NoApproval_Blocked() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.HIGH);
        boolean hasApproval = false;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        assertEquals(PlatformConfigErrorCode.FLAG_CHANGE_REQUIRES_APPROVAL, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("승인이 필요합니다"));
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("HIGH 심각도 플래그 변경 - 승인이 있으면 허용")
    void testValidateFlagChange_HighSeverity_WithApproval_Allowed() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.HIGH);
        boolean hasApproval = true;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("CRITICAL 심각도 플래그 변경 - 승인 없으면 예외 발생")
    void testValidateFlagChange_CriticalSeverity_NoApproval_Blocked() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.CRITICAL);
        boolean hasApproval = false;

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        assertEquals(PlatformConfigErrorCode.FLAG_CHANGE_REQUIRES_APPROVAL, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("승인이 필요합니다"));
        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("CRITICAL 심각도 플래그 변경 - 승인이 있으면 허용")
    void testValidateFlagChange_CriticalSeverity_WithApproval_Allowed() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.CRITICAL);
        boolean hasApproval = true;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("플래그가 null인 경우 - 예외 발생하지 않음")
    void testValidateFlagChange_NullFlag() {
        // Given
        FeatureFlag flag = null;
        boolean hasApproval = false;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(flag, hasApproval);
        });

        verify(complianceService, never()).getConfig();
    }

    @Test
    @DisplayName("심각도가 null인 플래그 - LOW로 간주되어 승인 불필요")
    void testValidateFlagChange_NullSeverity() {
        // Given
        testFlag.setSeverity(null);
        boolean hasApproval = false;

        // When & Then - 예외가 발생하지 않아야 함 (null은 LOW로 간주)
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        verify(complianceService).getConfig();
    }

    @Test
    @DisplayName("MEDIUM 심각도 플래그 변경 - 승인 없이도 허용")
    void testValidateFlagChange_MediumSeverity_NoApprovalNeeded() {
        // Given
        testFlag.setSeverity(FeatureFlagSeverity.MEDIUM);
        boolean hasApproval = false;

        // When & Then - 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> {
            policyValidator.validateFlagChange(testFlag, hasApproval);
        });

        verify(complianceService).getConfig();
    }
}

