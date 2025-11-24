package com.agenticcp.core.domain.platform.service;

import com.agenticcp.core.common.exception.BusinessException;
import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.platform.dto.ApprovalProcessRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalRequest;
import com.agenticcp.core.domain.platform.dto.FeatureFlagApprovalResponse;
import com.agenticcp.core.domain.platform.entity.FeatureFlag;
import com.agenticcp.core.domain.platform.entity.FeatureFlagApproval;
import com.agenticcp.core.domain.platform.enums.ApprovalStatus;
import com.agenticcp.core.domain.platform.enums.PlatformConfigErrorCode;
import com.agenticcp.core.domain.platform.repository.FeatureFlagApprovalRepository;
import com.agenticcp.core.domain.platform.repository.FeatureFlagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 기능 플래그 승인 서비스
 * 
 * 기능 플래그 변경에 대한 승인 워크플로우를 관리합니다.
 * HIGH 또는 CRITICAL 심각도의 플래그 변경 시 승인 요청을 생성하고,
 * 승인/거부 처리를 수행합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureFlagApprovalService {

    private final FeatureFlagApprovalRepository approvalRepository;
    private final FeatureFlagRepository featureFlagRepository;

    /**
     * 승인 요청 생성
     * 
     * @param request 승인 요청 정보
     * @param userId 요청자 ID
     * @return 생성된 승인 요청
     */
    @Transactional
    public FeatureFlagApprovalResponse requestApproval(FeatureFlagApprovalRequest request, String userId) {
        log.info("[FeatureFlagApprovalService] requestApproval - flagKey={} userId={}", 
                request.getFlagKey(), userId);

        // 플래그 조회
        FeatureFlag featureFlag = featureFlagRepository.findByFlagKey(request.getFlagKey())
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", request.getFlagKey()));

        // 이미 대기 중인 승인 요청이 있는지 확인
        Optional<FeatureFlagApproval> existingPending = approvalRepository
                .findByFeatureFlagAndStatus(featureFlag, ApprovalStatus.PENDING)
                .stream()
                .filter(approval -> !approval.getIsDeleted())
                .findFirst();

        if (existingPending.isPresent()) {
            log.warn("[FeatureFlagApprovalService] requestApproval - pending approval already exists " +
                    "flagKey={} approvalId={}", request.getFlagKey(), existingPending.get().getId());
            throw new BusinessException(
                    PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED,
                    "이미 대기 중인 승인 요청이 있습니다."
            );
        }

        // 승인 요청 생성
        FeatureFlagApproval approval = FeatureFlagApproval.builder()
                .featureFlag(featureFlag)
                .status(ApprovalStatus.PENDING)
                .requestedBy(userId)
                .requestReason(request.getRequestReason())
                .oldValue(request.getOldValue())
                .newValue(request.getNewValue())
                .requestedAt(LocalDateTime.now())
                .build();

        FeatureFlagApproval saved = approvalRepository.save(approval);
        log.info("[FeatureFlagApprovalService] requestApproval - success approvalId={} flagKey={}", 
                saved.getId(), request.getFlagKey());

        return convertToResponse(saved);
    }

    /**
     * 승인 처리
     * 
     * @param approvalId 승인 요청 ID
     * @param request 승인 처리 요청
     * @param userId 승인자 ID
     * @return 승인 처리된 승인 요청
     */
    @Transactional
    public FeatureFlagApprovalResponse approveRequest(Long approvalId, ApprovalProcessRequest request, String userId) {
        log.info("[FeatureFlagApprovalService] approveRequest - approvalId={} userId={}", approvalId, userId);

        FeatureFlagApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagApproval", "id", approvalId.toString()));

        // 이미 처리된 승인 요청인지 확인
        if (approval.isCompleted()) {
            log.warn("[FeatureFlagApprovalService] approveRequest - approval already processed " +
                    "approvalId={} status={}", approvalId, approval.getStatus());
            throw new BusinessException(
                    PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED,
                    String.format("이미 처리된 승인 요청입니다. (상태: %s)", approval.getStatus().getDescription())
            );
        }

        // 승인 처리
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setApprovedBy(userId);
        approval.setApprovalReason(request.getReason());
        approval.setApprovedAt(LocalDateTime.now());

        FeatureFlagApproval saved = approvalRepository.save(approval);
        log.info("[FeatureFlagApprovalService] approveRequest - success approvalId={} flagKey={}", 
                saved.getId(), saved.getFeatureFlag().getFlagKey());

        return convertToResponse(saved);
    }

    /**
     * 거부 처리
     * 
     * @param approvalId 승인 요청 ID
     * @param request 거부 처리 요청
     * @param userId 거부자 ID
     * @return 거부 처리된 승인 요청
     */
    @Transactional
    public FeatureFlagApprovalResponse rejectRequest(Long approvalId, ApprovalProcessRequest request, String userId) {
        log.info("[FeatureFlagApprovalService] rejectRequest - approvalId={} userId={}", approvalId, userId);

        FeatureFlagApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagApproval", "id", approvalId.toString()));

        // 이미 처리된 승인 요청인지 확인
        if (approval.isCompleted()) {
            log.warn("[FeatureFlagApprovalService] rejectRequest - approval already processed " +
                    "approvalId={} status={}", approvalId, approval.getStatus());
            throw new BusinessException(
                    PlatformConfigErrorCode.FLAG_APPROVAL_ALREADY_PROCESSED,
                    String.format("이미 처리된 승인 요청입니다. (상태: %s)", approval.getStatus().getDescription())
            );
        }

        // 거부 처리
        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setRejectedBy(userId);
        approval.setRejectionReason(request.getReason());
        approval.setRejectedAt(LocalDateTime.now());

        FeatureFlagApproval saved = approvalRepository.save(approval);
        log.info("[FeatureFlagApprovalService] rejectRequest - success approvalId={} flagKey={}", 
                saved.getId(), saved.getFeatureFlag().getFlagKey());

        return convertToResponse(saved);
    }

    /**
     * 승인 요청 조회
     * 
     * @param approvalId 승인 요청 ID
     * @return 승인 요청
     */
    public FeatureFlagApprovalResponse getApproval(Long approvalId) {
        log.info("[FeatureFlagApprovalService] getApproval - approvalId={}", approvalId);

        FeatureFlagApproval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlagApproval", "id", approvalId.toString()));

        log.info("[FeatureFlagApprovalService] getApproval - success approvalId={} status={}", 
                approvalId, approval.getStatus());
        return convertToResponse(approval);
    }

    /**
     * 승인 요청 목록 조회 (페이징)
     * 
     * @param pageable 페이징 정보
     * @return 승인 요청 목록
     */
    public Page<FeatureFlagApprovalResponse> getApprovals(Pageable pageable) {
        log.info("[FeatureFlagApprovalService] getApprovals - page={} size={}", 
                pageable.getPageNumber(), pageable.getPageSize());

        Page<FeatureFlagApproval> approvals = approvalRepository.findAll(pageable);
        Page<FeatureFlagApprovalResponse> responses = approvals.map(this::convertToResponse);

        log.info("[FeatureFlagApprovalService] getApprovals - success total={}", responses.getTotalElements());
        return responses;
    }

    /**
     * 플래그별 승인 요청 목록 조회
     * 
     * @param flagKey 플래그 키
     * @return 승인 요청 목록
     */
    public List<FeatureFlagApprovalResponse> getApprovalsByFlagKey(String flagKey) {
        log.info("[FeatureFlagApprovalService] getApprovalsByFlagKey - flagKey={}", flagKey);

        FeatureFlag featureFlag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new ResourceNotFoundException("FeatureFlag", "flagKey", flagKey));

        List<FeatureFlagApproval> approvals = approvalRepository.findByFeatureFlag(featureFlag);
        List<FeatureFlagApprovalResponse> responses = approvals.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        log.info("[FeatureFlagApprovalService] getApprovalsByFlagKey - success flagKey={} count={}", 
                flagKey, responses.size());
        return responses;
    }

    /**
     * 상태별 승인 요청 목록 조회 (페이징)
     * 
     * @param status 승인 상태
     * @param pageable 페이징 정보
     * @return 승인 요청 목록
     */
    public Page<FeatureFlagApprovalResponse> getApprovalsByStatus(ApprovalStatus status, Pageable pageable) {
        log.info("[FeatureFlagApprovalService] getApprovalsByStatus - status={} page={} size={}", 
                status, pageable.getPageNumber(), pageable.getPageSize());

        Page<FeatureFlagApproval> approvals = approvalRepository.findByStatus(status, pageable);
        Page<FeatureFlagApprovalResponse> responses = approvals.map(this::convertToResponse);

        log.info("[FeatureFlagApprovalService] getApprovalsByStatus - success status={} total={}", 
                status, responses.getTotalElements());
        return responses;
    }

    /**
     * 플래그에 대한 승인된 승인 요청이 있는지 확인
     * 
     * @param featureFlag 기능 플래그
     * @return 승인된 승인 요청이 있으면 true
     */
    public boolean hasApproval(FeatureFlag featureFlag) {
        if (featureFlag == null) {
            return false;
        }

        List<FeatureFlagApproval> approvals = approvalRepository
                .findByFeatureFlagAndStatus(featureFlag, ApprovalStatus.APPROVED);

        boolean hasApproved = approvals.stream()
                .anyMatch(approval -> !approval.getIsDeleted() && approval.isApproved());

        log.debug("[FeatureFlagApprovalService] hasApproval - flagKey={} hasApproved={}", 
                featureFlag.getFlagKey(), hasApproved);
        return hasApproved;
    }

    /**
     * 플래그 키로 승인된 승인 요청이 있는지 확인
     * 
     * @param flagKey 플래그 키
     * @return 승인된 승인 요청이 있으면 true
     */
    public boolean hasApproval(String flagKey) {
        if (flagKey == null) {
            return false;
        }

        Optional<FeatureFlag> featureFlagOpt = featureFlagRepository.findByFlagKey(flagKey);
        if (featureFlagOpt.isEmpty()) {
            return false;
        }

        return hasApproval(featureFlagOpt.get());
    }

    /**
     * 플래그에 대한 대기 중인 승인 요청 조회
     * 
     * @param featureFlag 기능 플래그
     * @return 대기 중인 승인 요청 (Optional)
     */
    public Optional<FeatureFlagApproval> findPendingApproval(FeatureFlag featureFlag) {
        if (featureFlag == null) {
            return Optional.empty();
        }

        List<FeatureFlagApproval> pendingApprovals = approvalRepository
                .findByFeatureFlagAndStatus(featureFlag, ApprovalStatus.PENDING);

        return pendingApprovals.stream()
                .filter(approval -> !approval.getIsDeleted())
                .findFirst();
    }

    /**
     * 엔티티를 응답 DTO로 변환
     * 
     * @param approval 승인 엔티티
     * @return 응답 DTO
     */
    private FeatureFlagApprovalResponse convertToResponse(FeatureFlagApproval approval) {
        if (approval == null) {
            return null;
        }

        return FeatureFlagApprovalResponse.builder()
                .id(approval.getId())
                .featureFlagId(approval.getFeatureFlag() != null ? approval.getFeatureFlag().getId() : null)
                .flagKey(approval.getFeatureFlag() != null ? approval.getFeatureFlag().getFlagKey() : null)
                .status(approval.getStatus())
                .requestedBy(approval.getRequestedBy())
                .requestReason(approval.getRequestReason())
                .oldValue(approval.getOldValue())
                .newValue(approval.getNewValue())
                .requestedAt(approval.getRequestedAt())
                .approvedBy(approval.getApprovedBy())
                .approvalReason(approval.getApprovalReason())
                .approvedAt(approval.getApprovedAt())
                .rejectedBy(approval.getRejectedBy())
                .rejectionReason(approval.getRejectionReason())
                .rejectedAt(approval.getRejectedAt())
                .createdAt(approval.getCreatedAt())
                .updatedAt(approval.getUpdatedAt())
                .build();
    }
}

