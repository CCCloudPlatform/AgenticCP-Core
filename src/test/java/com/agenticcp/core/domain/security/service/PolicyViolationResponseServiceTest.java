package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.PolicyViolationEvent;
import com.agenticcp.core.domain.security.dto.ViolationStatistics;
import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.entity.ViolationResponseConfig;
import com.agenticcp.core.domain.security.repository.PolicyViolationRepository;
import com.agenticcp.core.domain.security.repository.ViolationResponseConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PolicyViolationResponseService 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("정책 위반 대응 서비스 테스트")
class PolicyViolationResponseServiceTest {

    @Mock
    private PolicyViolationRepository violationRepository;

    @Mock
    private ViolationResponseConfigRepository configRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PolicyViolationResponseService violationService;

    private PolicyViolationEvent testEvent;
    private PolicyViolation testViolation;
    private ViolationResponseConfig testConfig;

    @BeforeEach
    void setUp() {
        // 테스트 이벤트 생성
        testEvent = PolicyViolationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .tenantId(1L)
                .policyId(100L)
                .policyName("로그인 정책")
                .userId(10L)
                .username("testuser")
                .ipAddress("192.168.1.100")
                .userAgent("Mozilla/5.0")
                .violationType(PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK)
                .severity(SecurityPolicy.Severity.HIGH)
                .description("5회 연속 로그인 실패")
                .detectedAt(LocalDateTime.now())
                .resourceType("USER_ACCOUNT")
                .resourceId("testuser")
                .actionAttempted("LOGIN")
                .requiresAutoResponse(true)
                .requiresNotification(true)
                .build();

        // 테스트 위반 엔티티 생성
        testViolation = PolicyViolation.builder()
                .tenantId(1L)
                .policyId(100L)
                .policyName("로그인 정책")
                .userId(10L)
                .username("testuser")
                .ipAddress("192.168.1.100")
                .violationType(PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK)
                .severity(SecurityPolicy.Severity.HIGH)
                .description("5회 연속 로그인 실패")
                .detectedAt(LocalDateTime.now())
                .status(PolicyViolation.ViolationStatus.DETECTED)
                .build();
        testViolation.setId(1L);

        // 테스트 대응 설정 생성
        testConfig = ViolationResponseConfig.builder()
                .tenantId(1L)
                .configName("무차별 대입 공격 차단")
                .violationType(PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK)
                .severity(SecurityPolicy.Severity.HIGH)
                .isEnabled(true)
                .responseAction(ViolationResponseConfig.ResponseAction.BLOCK_USER)
                .autoExecute(true)
                .sendNotification(true)
                .priority(10)
                .build();
    }

    @Test
    @DisplayName("위반 이벤트 처리 - 성공")
    void testHandleViolationEvent_Success() {
        // Given
        when(violationRepository.save(any(PolicyViolation.class))).thenReturn(testViolation);
        when(configRepository.findApplicableConfigs(anyLong(), any(), any(), anyLong()))
                .thenReturn(List.of(testConfig));

        // When
        violationService.handleViolationEvent(testEvent);

        // Then
        verify(violationRepository, atLeastOnce()).save(any(PolicyViolation.class));
        verify(configRepository).findApplicableConfigs(anyLong(), any(), any(), anyLong());
    }

    @Test
    @DisplayName("위반 내역 저장 테스트")
    void testSaveViolation() {
        // Given
        when(violationRepository.save(any(PolicyViolation.class))).thenReturn(testViolation);

        // When
        violationService.handleViolationEvent(testEvent);

        // Then
        ArgumentCaptor<PolicyViolation> captor = ArgumentCaptor.forClass(PolicyViolation.class);
        verify(violationRepository, atLeastOnce()).save(captor.capture());

        PolicyViolation savedViolation = captor.getValue();
        assertThat(savedViolation.getTenantId()).isEqualTo(testEvent.getTenantId());
        assertThat(savedViolation.getViolationType()).isEqualTo(testEvent.getViolationType());
        assertThat(savedViolation.getSeverity()).isEqualTo(testEvent.getSeverity());
    }

    @Test
    @DisplayName("위반 통계 조회 - 성공")
    void testGetStatistics_Success() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        List<PolicyViolation> violations = Arrays.asList(
                createViolation(1L, PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK, SecurityPolicy.Severity.HIGH),
                createViolation(2L, PolicyViolation.ViolationType.ACCESS_DENIED, SecurityPolicy.Severity.MEDIUM),
                createViolation(3L, PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK, SecurityPolicy.Severity.HIGH)
        );

        when(violationRepository.findByTenantIdAndDetectedAtBetween(anyLong(), any(), any()))
                .thenReturn(violations);
        when(violationRepository.calculateAverageResolutionTime(anyLong(), any(), any()))
                .thenReturn(45.5);

        // When
        ViolationStatistics statistics = violationService.getStatistics(1L, startTime, endTime);

        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getTotalViolations()).isEqualTo(3L);
        assertThat(statistics.getViolationsByType()).containsKey(PolicyViolation.ViolationType.BRUTE_FORCE_ATTACK);
        assertThat(statistics.getViolationsBySeverity()).containsKey(SecurityPolicy.Severity.HIGH);
        assertThat(statistics.getAverageResolutionTimeMinutes()).isEqualTo(45.5);
    }

    @Test
    @DisplayName("위반 해결 처리 - 성공")
    void testResolveViolation_Success() {
        // Given
        when(violationRepository.findById(1L)).thenReturn(Optional.of(testViolation));
        when(violationRepository.save(any(PolicyViolation.class))).thenReturn(testViolation);

        // When
        PolicyViolation resolved = violationService.resolveViolation(1L, "admin", "수동으로 해결함");

        // Then
        assertThat(resolved).isNotNull();
        assertThat(resolved.getStatus()).isEqualTo(PolicyViolation.ViolationStatus.RESOLVED);
        assertThat(resolved.getResolvedBy()).isEqualTo("admin");
        assertThat(resolved.getResolutionNotes()).isEqualTo("수동으로 해결함");
        assertThat(resolved.getResolvedAt()).isNotNull();

        verify(violationRepository).findById(1L);
        verify(violationRepository).save(testViolation);
    }

    @Test
    @DisplayName("오탐지 처리 - 성공")
    void testMarkAsFalsePositive_Success() {
        // Given
        when(violationRepository.findById(1L)).thenReturn(Optional.of(testViolation));
        when(violationRepository.save(any(PolicyViolation.class))).thenReturn(testViolation);

        // When
        PolicyViolation marked = violationService.markAsFalsePositive(1L, "admin", "오탐지 확인");

        // Then
        assertThat(marked).isNotNull();
        assertThat(marked.getStatus()).isEqualTo(PolicyViolation.ViolationStatus.FALSE_POSITIVE);
        assertThat(marked.getFalsePositive()).isTrue();
        assertThat(marked.getResolvedBy()).isEqualTo("admin");
        assertThat(marked.getResolutionNotes()).isEqualTo("오탐지 확인");

        verify(violationRepository).findById(1L);
        verify(violationRepository).save(testViolation);
    }

    @Test
    @DisplayName("위반 조회 - 존재하지 않는 ID")
    void testGetViolation_NotFound() {
        // Given
        when(violationRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PolicyViolation> result = violationService.getViolation(999L);

        // Then
        assertThat(result).isEmpty();
        verify(violationRepository).findById(999L);
    }

    @Test
    @DisplayName("통계 조회 - 위반 내역 없음")
    void testGetStatistics_NoViolations() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();

        when(violationRepository.findByTenantIdAndDetectedAtBetween(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(violationRepository.calculateAverageResolutionTime(anyLong(), any(), any()))
                .thenReturn(null);

        // When
        ViolationStatistics statistics = violationService.getStatistics(1L, startTime, endTime);

        // Then
        assertThat(statistics).isNotNull();
        assertThat(statistics.getTotalViolations()).isEqualTo(0L);
        assertThat(statistics.getResolvedViolations()).isEqualTo(0L);
        assertThat(statistics.getPendingViolations()).isEqualTo(0L);
    }

    // Helper 메서드
    private PolicyViolation createViolation(Long id, PolicyViolation.ViolationType type, SecurityPolicy.Severity severity) {
        PolicyViolation violation = PolicyViolation.builder()
                .tenantId(1L)
                .policyId(100L)
                .policyName("테스트 정책")
                .userId(10L)
                .username("testuser")
                .violationType(type)
                .severity(severity)
                .detectedAt(LocalDateTime.now().minusHours(id))
                .status(PolicyViolation.ViolationStatus.DETECTED)
                .build();
        violation.setId(id);
        return violation;
    }
}

