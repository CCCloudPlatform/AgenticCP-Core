package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.PolicyViolationEvent;
import com.agenticcp.core.domain.security.dto.ViolationStatistics;
import com.agenticcp.core.domain.security.entity.PolicyViolation;
import com.agenticcp.core.domain.security.entity.SecurityPolicy;
import com.agenticcp.core.domain.security.entity.ViolationResponseConfig;
import com.agenticcp.core.domain.security.repository.PolicyViolationRepository;
import com.agenticcp.core.domain.security.repository.ViolationResponseConfigRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 정책 위반 대응 서비스
 * 
 * <p>정책 위반 감지, 자동 대응, 알림 발송, 통계 수집을 담당합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyViolationResponseService {

    private final PolicyViolationRepository violationRepository;
    private final ViolationResponseConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    /**
     * 정책 위반 이벤트 처리
     * 
     * @param event 위반 이벤트
     */
    @Async
    @EventListener
    @Transactional
    public void handleViolationEvent(PolicyViolationEvent event) {
        log.info("🚨 [정책 위반 감지] 이벤트 처리 시작 - 이벤트ID: {}, 테넌트: {}, 위반타입: {}", 
                 event.getEventId(), event.getTenantId(), event.getViolationType());

        try {
            // 1. 위반 내역 저장
            PolicyViolation violation = saveViolation(event);
            log.info("✅ 위반 내역 저장 완료 - ID: {}", violation.getId());

            // 2. 적용 가능한 대응 설정 조회
            List<ViolationResponseConfig> applicableConfigs = findApplicableConfigs(event);
            log.info("📋 적용 가능한 대응 설정 {}개 발견", applicableConfigs.size());

            // 3. 자동 대응 실행
            if (event.getRequiresAutoResponse() && !applicableConfigs.isEmpty()) {
                executeAutoResponse(violation, applicableConfigs);
            }

            // 4. 알림 발송
            if (event.getRequiresNotification()) {
                sendNotification(violation);
            }

            log.info("✅ 정책 위반 이벤트 처리 완료 - 위반ID: {}", violation.getId());

        } catch (Exception e) {
            log.error("❌ 정책 위반 이벤트 처리 실패 - 이벤트ID: {}", event.getEventId(), e);
            // 재시도 로직은 Spring Retry로 구현 가능
        }
    }

    /**
     * 위반 내역 저장
     */
    private PolicyViolation saveViolation(PolicyViolationEvent event) {
        String violationDetailsJson = null;
        if (event.getViolationDetails() != null) {
            try {
                violationDetailsJson = objectMapper.writeValueAsString(event.getViolationDetails());
            } catch (JsonProcessingException e) {
                log.warn("위반 상세 정보 JSON 변환 실패", e);
            }
        }

        PolicyViolation violation = PolicyViolation.builder()
                .tenantId(event.getTenantId())
                .policyId(event.getPolicyId())
                .policyName(event.getPolicyName())
                .userId(event.getUserId())
                .username(event.getUsername())
                .ipAddress(event.getIpAddress())
                .userAgent(event.getUserAgent())
                .violationType(event.getViolationType())
                .severity(event.getSeverity())
                .description(event.getDescription())
                .detectedAt(event.getDetectedAt() != null ? event.getDetectedAt() : LocalDateTime.now())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .actionAttempted(event.getActionAttempted())
                .violationDetails(violationDetailsJson)
                .status(PolicyViolation.ViolationStatus.DETECTED)
                .autoResponseExecuted(false)
                .notificationSent(false)
                .falsePositive(false)
                .build();

        return violationRepository.save(violation);
    }

    /**
     * 적용 가능한 대응 설정 조회
     */
    private List<ViolationResponseConfig> findApplicableConfigs(PolicyViolationEvent event) {
        return configRepository.findApplicableConfigs(
                event.getTenantId(),
                event.getViolationType(),
                event.getSeverity(),
                event.getPolicyId()
        );
    }

    /**
     * 자동 대응 실행
     */
    private void executeAutoResponse(PolicyViolation violation, List<ViolationResponseConfig> configs) {
        log.info("🤖 자동 대응 실행 시작 - 위반ID: {}", violation.getId());

        List<String> executedActions = new ArrayList<>();
        violation.setStatus(PolicyViolation.ViolationStatus.PROCESSING);
        violationRepository.save(violation);

        for (ViolationResponseConfig config : configs) {
            if (!config.getAutoExecute()) {
                log.info("⏭️  대응 설정 {} - 자동 실행 비활성화, 스킵", config.getConfigName());
                continue;
            }

            try {
                executeResponseAction(violation, config);
                executedActions.add(config.getResponseAction().name());
                log.info("✅ 대응 액션 실행 완료: {}", config.getResponseAction());

            } catch (Exception e) {
                log.error("❌ 대응 액션 실행 실패: {}", config.getResponseAction(), e);
            }
        }

        // 대응 결과 저장
        if (!executedActions.isEmpty()) {
            try {
                violation.setAutoResponseExecuted(true);
                violation.setResponseAction(objectMapper.writeValueAsString(executedActions));
                violation.setResponseExecutedAt(LocalDateTime.now());
                violation.setStatus(PolicyViolation.ViolationStatus.RESPONDED);
                violationRepository.save(violation);
                log.info("✅ 자동 대응 완료 - {}개 액션 실행", executedActions.size());
            } catch (JsonProcessingException e) {
                log.error("대응 액션 JSON 변환 실패", e);
            }
        }
    }

    /**
     * 개별 대응 액션 실행
     */
    private void executeResponseAction(PolicyViolation violation, ViolationResponseConfig config) {
        ViolationResponseConfig.ResponseAction action = config.getResponseAction();
        
        log.info("🎯 대응 액션 실행: {} - 위반ID: {}, 사용자: {}", 
                 action, violation.getId(), violation.getUsername());

        switch (action) {
            case BLOCK_USER:
                blockUser(violation, config);
                break;
            case BLOCK_IP:
                blockIp(violation, config);
                break;
            case REQUIRE_2FA:
                require2FA(violation, config);
                break;
            case SEND_NOTIFICATION:
                sendNotification(violation);
                break;
            case LOG_ONLY:
                logViolation(violation);
                break;
            case RESET_PASSWORD:
                resetPassword(violation, config);
                break;
            case SUSPEND_SESSION:
                suspendSession(violation, config);
                break;
            case QUARANTINE_USER:
                quarantineUser(violation, config);
                break;
            case ESCALATE:
                escalate(violation, config);
                break;
            case AUTO_REMEDIATE:
                autoRemediate(violation, config);
                break;
            default:
                log.warn("⚠️  알 수 없는 대응 액션: {}", action);
        }
    }

    /**
     * 사용자 차단
     */
    private void blockUser(PolicyViolation violation, ViolationResponseConfig config) {
        log.warn("🚫 [사용자 차단] UserID: {}, Username: {}", violation.getUserId(), violation.getUsername());
        // TODO: 실제 사용자 차단 로직 구현 (User 서비스 연동)
        // userService.blockUser(violation.getUserId(), "정책 위반으로 인한 자동 차단");
    }

    /**
     * IP 주소 차단
     */
    private void blockIp(PolicyViolation violation, ViolationResponseConfig config) {
        log.warn("🚫 [IP 차단] IP: {}", violation.getIpAddress());
        // TODO: 실제 IP 차단 로직 구현 (방화벽 또는 블랙리스트 서비스 연동)
        // firewallService.blockIp(violation.getIpAddress());
    }

    /**
     * 2FA 강제 요구
     */
    private void require2FA(PolicyViolation violation, ViolationResponseConfig config) {
        log.info("🔐 [2FA 요구] UserID: {}", violation.getUserId());
        // TODO: 2FA 강제 활성화 로직 구현
        // userService.require2FA(violation.getUserId());
    }

    /**
     * 비밀번호 재설정 요구
     */
    private void resetPassword(PolicyViolation violation, ViolationResponseConfig config) {
        log.info("🔑 [비밀번호 재설정] UserID: {}", violation.getUserId());
        // TODO: 비밀번호 재설정 요구 로직 구현
        // userService.requirePasswordReset(violation.getUserId());
    }

    /**
     * 세션 중단
     */
    private void suspendSession(PolicyViolation violation, ViolationResponseConfig config) {
        log.warn("⏸️ [세션 중단] UserID: {}", violation.getUserId());
        // TODO: 세션 중단 로직 구현
        // sessionService.suspendUserSessions(violation.getUserId());
    }

    /**
     * 사용자 격리
     */
    private void quarantineUser(PolicyViolation violation, ViolationResponseConfig config) {
        log.warn("🏥 [사용자 격리] UserID: {}", violation.getUserId());
        // TODO: 사용자 격리 로직 구현 (제한된 권한으로 변경)
        // userService.quarantineUser(violation.getUserId());
    }

    /**
     * 에스컬레이션
     */
    private void escalate(PolicyViolation violation, ViolationResponseConfig config) {
        log.info("⬆️  [에스컬레이션] 상위 관리자에게 알림");
        // TODO: 에스컬레이션 로직 구현
        sendEscalationNotification(violation);
    }

    /**
     * 자동 복구
     */
    private void autoRemediate(PolicyViolation violation, ViolationResponseConfig config) {
        log.info("🔧 [자동 복구] 시도");
        // TODO: 자동 복구 로직 구현
    }

    /**
     * 알림 발송
     */
    private void sendNotification(PolicyViolation violation) {
        log.info("📧 [알림 발송] 위반ID: {}, 심각도: {}", violation.getId(), violation.getSeverity());
        
        // 실제 알림 발송 (로그로 대체)
        log.warn("====================================");
        log.warn("🚨 보안 정책 위반 알림");
        log.warn("====================================");
        log.warn("위반 ID: {}", violation.getId());
        log.warn("정책: {}", violation.getPolicyName());
        log.warn("위반 타입: {}", violation.getViolationType());
        log.warn("심각도: {}", violation.getSeverity());
        log.warn("사용자: {}", violation.getUsername());
        log.warn("IP 주소: {}", violation.getIpAddress());
        log.warn("감지 시간: {}", violation.getDetectedAt());
        log.warn("설명: {}", violation.getDescription());
        log.warn("====================================");

        // 알림 발송 기록
        violation.setNotificationSent(true);
        violation.setNotificationSentAt(LocalDateTime.now());
        violationRepository.save(violation);
    }

    /**
     * 에스컬레이션 알림
     */
    private void sendEscalationNotification(PolicyViolation violation) {
        log.error("====================================");
        log.error("⚠️  긴급: 보안 정책 위반 에스컬레이션");
        log.error("====================================");
        log.error("위반 ID: {}", violation.getId());
        log.error("심각도: {} (긴급 조치 필요)", violation.getSeverity());
        log.error("위반 타입: {}", violation.getViolationType());
        log.error("사용자: {}", violation.getUsername());
        log.error("IP: {}", violation.getIpAddress());
        log.error("감지 시간: {}", violation.getDetectedAt());
        log.error("====================================");
    }

    /**
     * 로그 기록
     */
    private void logViolation(PolicyViolation violation) {
        log.info("📝 [위반 로그] ID: {}, 타입: {}, 사용자: {}", 
                 violation.getId(), violation.getViolationType(), violation.getUsername());
    }

    /**
     * 위반 내역 조회
     */
    @Transactional(readOnly = true)
    public List<PolicyViolation> getViolations(Long tenantId) {
        return violationRepository.findByTenantId(tenantId);
    }

    /**
     * 위반 내역 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<PolicyViolation> getViolations(Long tenantId, Pageable pageable) {
        return violationRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * 위반 상세 조회
     */
    @Transactional(readOnly = true)
    public Optional<PolicyViolation> getViolation(Long violationId) {
        return violationRepository.findById(violationId);
    }

    /**
     * 위반 통계 조회
     */
    @Transactional(readOnly = true)
    public ViolationStatistics getStatistics(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        log.info("📊 위반 통계 조회 - 테넌트: {}, 기간: {} ~ {}", tenantId, startTime, endTime);

        List<PolicyViolation> violations = violationRepository.findByTenantIdAndDetectedAtBetween(
                tenantId, startTime, endTime);

        // 통계 계산
        long totalViolations = violations.size();
        long resolvedViolations = violations.stream()
                .filter(v -> v.getStatus() == PolicyViolation.ViolationStatus.RESOLVED)
                .count();
        long pendingViolations = violations.stream()
                .filter(v -> v.getStatus() == PolicyViolation.ViolationStatus.DETECTED || 
                           v.getStatus() == PolicyViolation.ViolationStatus.PROCESSING)
                .count();
        long falsePositives = violations.stream()
                .filter(PolicyViolation::getFalsePositive)
                .count();
        long autoResponseExecuted = violations.stream()
                .filter(PolicyViolation::getAutoResponseExecuted)
                .count();

        // 위반 타입별 통계
        Map<PolicyViolation.ViolationType, Long> violationsByType = violations.stream()
                .collect(Collectors.groupingBy(PolicyViolation::getViolationType, Collectors.counting()));

        // 심각도별 통계
        Map<SecurityPolicy.Severity, Long> violationsBySeverity = violations.stream()
                .collect(Collectors.groupingBy(PolicyViolation::getSeverity, Collectors.counting()));

        // 정책별 통계 (Top 10)
        Map<String, Long> violationsByPolicy = violations.stream()
                .collect(Collectors.groupingBy(PolicyViolation::getPolicyName, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // 사용자별 통계 (Top 10)
        Map<String, Long> violationsByUser = violations.stream()
                .filter(v -> v.getUsername() != null)
                .collect(Collectors.groupingBy(PolicyViolation::getUsername, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // IP별 통계 (Top 10)
        Map<String, Long> violationsByIp = violations.stream()
                .filter(v -> v.getIpAddress() != null)
                .collect(Collectors.groupingBy(PolicyViolation::getIpAddress, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        // 평균 해결 시간
        Double averageResolutionTime = violationRepository.calculateAverageResolutionTime(tenantId, startTime, endTime);

        // 가장 많이 위반된 정책
        String mostViolatedPolicy = violationsByPolicy.isEmpty() ? null : 
                violationsByPolicy.entrySet().iterator().next().getKey();

        // 가장 문제가 많은 사용자
        String mostProblematicUser = violationsByUser.isEmpty() ? null :
                violationsByUser.entrySet().iterator().next().getKey();

        return ViolationStatistics.builder()
                .tenantId(tenantId)
                .startTime(startTime)
                .endTime(endTime)
                .totalViolations(totalViolations)
                .resolvedViolations(resolvedViolations)
                .pendingViolations(pendingViolations)
                .falsePositives(falsePositives)
                .autoResponseExecuted(autoResponseExecuted)
                .violationsByType(violationsByType)
                .violationsBySeverity(violationsBySeverity)
                .violationsByPolicy(violationsByPolicy)
                .violationsByUser(violationsByUser)
                .violationsByIp(violationsByIp)
                .averageResolutionTimeMinutes(averageResolutionTime)
                .mostViolatedPolicy(mostViolatedPolicy)
                .mostProblematicUser(mostProblematicUser)
                .build();
    }

    /**
     * 위반 해결 처리
     */
    @Transactional
    public PolicyViolation resolveViolation(Long violationId, String resolvedBy, String notes) {
        PolicyViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("위반 내역을 찾을 수 없습니다: " + violationId));

        violation.setStatus(PolicyViolation.ViolationStatus.RESOLVED);
        violation.setResolvedAt(LocalDateTime.now());
        violation.setResolvedBy(resolvedBy);
        violation.setResolutionNotes(notes);

        return violationRepository.save(violation);
    }

    /**
     * 오탐지 처리
     */
    @Transactional
    public PolicyViolation markAsFalsePositive(Long violationId, String resolvedBy, String notes) {
        PolicyViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("위반 내역을 찾을 수 없습니다: " + violationId));

        violation.setStatus(PolicyViolation.ViolationStatus.FALSE_POSITIVE);
        violation.setFalsePositive(true);
        violation.setResolvedAt(LocalDateTime.now());
        violation.setResolvedBy(resolvedBy);
        violation.setResolutionNotes(notes);

        return violationRepository.save(violation);
    }
}

