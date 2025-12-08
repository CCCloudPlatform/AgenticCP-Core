package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.dto.ApprovalProcessRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagApproval;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.agenticcp.core.domain.platform.enums.FeatureFlagSeverity;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.repository.FeatureFlagApprovalRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FeatureFlagApprovalService 단위 테스트
 * 
 * 기능 플래그 승인 워크플로우 서비스의 핵심 기능을 검증합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeatureFlagApprovalService 테스트")
class FeatureFlagApprovalServiceTest {

    @Mock
    private FeatureFlagApprovalRepository approvalRepository;

    @Mock
    private FeatureFlagRepository featureFlagRepository;

    @InjectMocks
    private FeatureFlagApprovalService approvalService;

    private FeatureFlag testFlag;
    private FeatureFlagApproval testApproval;

    @BeforeEach
    void setUp() {
        // 테스트용 FeatureFlag 생성
        testFlag = FeatureFlag.builder()
                .flagKey("test-flag")
                .flagName("Test Flag")
                .severity(FeatureFlagSeverity.HIGH)
                .build();

        // 테스트용 FeatureFlagApproval 생성
        testApproval = FeatureFlagApproval.builder()
                .featureFlag(testFlag)
                .status(ApprovalStatus.PENDING)
                .requestedBy("user-123")
                .requestReason("테스트 승인 요청")
                .oldValue("{\"enabled\":false}")
                .newValue("{\"enabled\":true}")
                .requestedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("승인 요청 생성 성공")
    void testRequestApproval() {
        // Given
        FeatureFlagApprovalRequest request = FeatureFlagApprovalRequest.builder()
                .flagKey("test-flag")
                .requestReason("기능 활성화 요청")
                .oldValue("{\"enabled\":false}")
                .newValue("{\"enabled\":true}")
                .build();

        String userId = "user-123";

        when(featureFlagRepository.findByFlagKey("test-flag")).thenReturn(Optional.of(testFlag));
        when(approvalRepository.findByFeatureFlagAndStatus(testFlag, ApprovalStatus.PENDING))
                .thenReturn(Collections.emptyList());
        when(approvalRepository.save(any(FeatureFlagApproval.class))).thenAnswer(invocation -> {
            FeatureFlagApproval approval = invocation.getArgument(0);
            return approval;
        });

        // When
        FeatureFlagApprovalResponse response = approvalService.requestApproval(request, userId);

        // Then
        assertNotNull(response);
        assertEquals("test-flag", response.getFlagKey());
        assertEquals(ApprovalStatus.PENDING, response.getStatus());
        assertEquals(userId, response.getRequestedBy());
        assertEquals("기능 활성화 요청", response.getRequestReason());
        assertNotNull(response.getRequestedAt());

        ArgumentCaptor<FeatureFlagApproval> captor = ArgumentCaptor.forClass(FeatureFlagApproval.class);
        verify(approvalRepository).save(captor.capture());
        FeatureFlagApproval savedApproval = captor.getValue();
        assertEquals(testFlag, savedApproval.getFeatureFlag());
        assertEquals(ApprovalStatus.PENDING, savedApproval.getStatus());
        assertEquals(userId, savedApproval.getRequestedBy());
    }

    @Test
    @DisplayName("승인 요청 생성 실패 - 플래그가 없는 경우")
    void testRequestApproval_FlagNotFound() {
        // Given
        FeatureFlagApprovalRequest request = FeatureFlagApprovalRequest.builder()
                .flagKey("non-existent-flag")
                .requestReason("테스트")
                .newValue("{\"enabled\":true}")
                .build();

        when(featureFlagRepository.findByFlagKey("non-existent-flag")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            approvalService.requestApproval(request, "user-123");
        });

        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인 요청 생성 실패 - 이미 대기 중인 승인 요청이 있는 경우")
    void testRequestApproval_PendingApprovalExists() {
        // Given
        FeatureFlagApprovalRequest request = FeatureFlagApprovalRequest.builder()
                .flagKey("test-flag")
                .requestReason("테스트")
                .newValue("{\"enabled\":true}")
                .build();

        when(featureFlagRepository.findByFlagKey("test-flag")).thenReturn(Optional.of(testFlag));
        when(approvalRepository.findByFeatureFlagAndStatus(testFlag, ApprovalStatus.PENDING))
                .thenReturn(Collections.singletonList(testApproval));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.requestApproval(request, "user-123");
        });

        assertEquals(PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED, exception.getErrorCode());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인 처리 성공")
    void testApproveRequest() {
        // Given
        Long approvalId = 1L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("승인 사유: 테스트 통과")
                .build();
        String approverId = "approver-123";

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.of(testApproval));
        when(approvalRepository.save(any(FeatureFlagApproval.class))).thenAnswer(invocation -> {
            FeatureFlagApproval approval = invocation.getArgument(0);
            return approval;
        });

        // When
        FeatureFlagApprovalResponse response = approvalService.approveRequest(approvalId, request, approverId);

        // Then
        assertNotNull(response);
        assertEquals(ApprovalStatus.APPROVED, response.getStatus());
        assertEquals(approverId, response.getApprovedBy());
        assertEquals("승인 사유: 테스트 통과", response.getApprovalReason());
        assertNotNull(response.getApprovedAt());

        ArgumentCaptor<FeatureFlagApproval> captor = ArgumentCaptor.forClass(FeatureFlagApproval.class);
        verify(approvalRepository).save(captor.capture());
        FeatureFlagApproval savedApproval = captor.getValue();
        assertEquals(ApprovalStatus.APPROVED, savedApproval.getStatus());
        assertEquals(approverId, savedApproval.getApprovedBy());
        assertNotNull(savedApproval.getApprovedAt());
    }

    @Test
    @DisplayName("승인 처리 실패 - 승인 요청이 없는 경우")
    void testApproveRequest_NotFound() {
        // Given
        Long approvalId = 999L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("승인 사유")
                .build();

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            approvalService.approveRequest(approvalId, request, "approver-123");
        });

        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인 처리 실패 - 이미 처리된 승인 요청인 경우")
    void testApproveRequest_AlreadyProcessed() {
        // Given
        Long approvalId = 1L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("승인 사유")
                .build();

        // 이미 승인된 상태로 설정
        testApproval.setStatus(ApprovalStatus.APPROVED);
        testApproval.setApprovedBy("previous-approver");
        testApproval.setApprovedAt(LocalDateTime.now().minusHours(1));

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.of(testApproval));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.approveRequest(approvalId, request, "approver-123");
        });

        assertEquals(PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED, exception.getErrorCode());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("거부 처리 성공")
    void testRejectRequest() {
        // Given
        Long approvalId = 1L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("거부 사유: 테스트 실패")
                .build();
        String rejectorId = "rejector-123";

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.of(testApproval));
        when(approvalRepository.save(any(FeatureFlagApproval.class))).thenAnswer(invocation -> {
            FeatureFlagApproval approval = invocation.getArgument(0);
            return approval;
        });

        // When
        FeatureFlagApprovalResponse response = approvalService.rejectRequest(approvalId, request, rejectorId);

        // Then
        assertNotNull(response);
        assertEquals(ApprovalStatus.REJECTED, response.getStatus());
        assertEquals(rejectorId, response.getRejectedBy());
        assertEquals("거부 사유: 테스트 실패", response.getRejectionReason());
        assertNotNull(response.getRejectedAt());

        ArgumentCaptor<FeatureFlagApproval> captor = ArgumentCaptor.forClass(FeatureFlagApproval.class);
        verify(approvalRepository).save(captor.capture());
        FeatureFlagApproval savedApproval = captor.getValue();
        assertEquals(ApprovalStatus.REJECTED, savedApproval.getStatus());
        assertEquals(rejectorId, savedApproval.getRejectedBy());
        assertNotNull(savedApproval.getRejectedAt());
    }

    @Test
    @DisplayName("거부 처리 실패 - 승인 요청이 없는 경우")
    void testRejectRequest_NotFound() {
        // Given
        Long approvalId = 999L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("거부 사유")
                .build();

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            approvalService.rejectRequest(approvalId, request, "rejector-123");
        });

        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("거부 처리 실패 - 이미 처리된 승인 요청인 경우")
    void testRejectRequest_AlreadyProcessed() {
        // Given
        Long approvalId = 1L;
        ApprovalProcessRequest request = ApprovalProcessRequest.builder()
                .reason("거부 사유")
                .build();

        // 이미 거부된 상태로 설정
        testApproval.setStatus(ApprovalStatus.REJECTED);
        testApproval.setRejectedBy("previous-rejector");
        testApproval.setRejectedAt(LocalDateTime.now().minusHours(1));

        when(approvalRepository.findById(approvalId)).thenReturn(Optional.of(testApproval));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.rejectRequest(approvalId, request, "rejector-123");
        });

        assertEquals(PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED, exception.getErrorCode());
        verify(approvalRepository, never()).save(any());
    }

    @Test
    @DisplayName("승인된 승인 요청이 있는 경우 - true 반환")
    void testHasApproval_True() {
        // Given
        FeatureFlagApproval approvedApproval = FeatureFlagApproval.builder()
                .featureFlag(testFlag)
                .status(ApprovalStatus.APPROVED)
                .requestedBy("user-123")
                .approvedBy("approver-123")
                .approvedAt(LocalDateTime.now())
                .build();

        when(approvalRepository.findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED))
                .thenReturn(Collections.singletonList(approvedApproval));

        // When
        boolean result = approvalService.hasApproval(testFlag);

        // Then
        assertTrue(result);
        verify(approvalRepository).findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("승인된 승인 요청이 없는 경우 - false 반환")
    void testHasApproval_False() {
        // Given
        when(approvalRepository.findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED))
                .thenReturn(Collections.emptyList());

        // When
        boolean result = approvalService.hasApproval(testFlag);

        // Then
        assertFalse(result);
        verify(approvalRepository).findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("승인된 승인 요청이 없는 경우 - 플래그가 null인 경우")
    void testHasApproval_NullFlag() {
        // When
        boolean result = approvalService.hasApproval((FeatureFlag) null);

        // Then
        assertFalse(result);
        verify(approvalRepository, never()).findByFeatureFlagAndStatus(any(), any());
    }

    @Test
    @DisplayName("플래그 키로 승인 확인 - 승인된 승인 요청이 있는 경우")
    void testHasApproval_ByFlagKey_True() {
        // Given
        String flagKey = "test-flag";
        FeatureFlagApproval approvedApproval = FeatureFlagApproval.builder()
                .featureFlag(testFlag)
                .status(ApprovalStatus.APPROVED)
                .requestedBy("user-123")
                .approvedBy("approver-123")
                .approvedAt(LocalDateTime.now())
                .build();

        when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.of(testFlag));
        when(approvalRepository.findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED))
                .thenReturn(Collections.singletonList(approvedApproval));

        // When
        boolean result = approvalService.hasApproval(flagKey);

        // Then
        assertTrue(result);
        verify(featureFlagRepository).findByFlagKey(flagKey);
        verify(approvalRepository).findByFeatureFlagAndStatus(testFlag, ApprovalStatus.APPROVED);
    }

    @Test
    @DisplayName("플래그 키로 승인 확인 - 플래그가 없는 경우")
    void testHasApproval_ByFlagKey_FlagNotFound() {
        // Given
        String flagKey = "non-existent-flag";

        when(featureFlagRepository.findByFlagKey(flagKey)).thenReturn(Optional.empty());

        // When
        boolean result = approvalService.hasApproval(flagKey);

        // Then
        assertFalse(result);
        verify(featureFlagRepository).findByFlagKey(flagKey);
        verify(approvalRepository, never()).findByFeatureFlagAndStatus(any(), any());
    }

    @Test
    @DisplayName("플래그 키로 승인 확인 - 플래그 키가 null인 경우")
    void testHasApproval_ByFlagKey_Null() {
        // When
        boolean result = approvalService.hasApproval((String) null);

        // Then
        assertFalse(result);
        verify(featureFlagRepository, never()).findByFlagKey(any());
    }
}

