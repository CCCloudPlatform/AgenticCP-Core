package com.agenticcp.core.domain.user.service;

import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.entity.UserAuthHistory;
import com.agenticcp.core.domain.user.enums.AuthType;
import com.agenticcp.core.domain.user.repository.UserAuthHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 인증 이력 서비스
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAuthHistoryService {

    private final UserAuthHistoryRepository authHistoryRepository;

    /**
     * 인증 이력 저장 (비동기)
     * 별도 트랜잭션으로 처리하여 메인 트랜잭션 롤백 시에도 이력 보존
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthHistory(User user, AuthType authType, boolean isSuccess, 
                                   String ipAddress, String userAgent, String failureReason) {
        try {
            UserAuthHistory history = UserAuthHistory.builder()
                    .user(user)
                    .authType(authType)
                    .isSuccess(isSuccess)
                    .authTimestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .failureReason(failureReason)
                    .isPending(false)
                    .build();

            authHistoryRepository.save(history);
            
            log.debug("[UserAuthHistoryService] Auth history recorded - user={}, type={}, success={}",
                    user.getUsername(), authType, isSuccess);
        } catch (Exception e) {
            // 이력 저장 실패해도 메인 로직에 영향 없도록 예외 무시
            log.error("[UserAuthHistoryService] Failed to record auth history", e);
        }
    }

    /**
     * 인증 이력 저장 (상세 정보 포함)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthHistoryDetailed(User user, AuthType authType, boolean isSuccess,
                                          String ipAddress, String userAgent, String failureReason,
                                          String authCodeMasked, String tokenId, String sessionId,
                                          String deviceInfo, String location, String metadata) {
        try {
            UserAuthHistory history = UserAuthHistory.builder()
                    .user(user)
                    .authType(authType)
                    .isSuccess(isSuccess)
                    .authTimestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .failureReason(failureReason)
                    .authCodeMasked(authCodeMasked)
                    .tokenId(tokenId)
                    .sessionId(sessionId)
                    .deviceInfo(deviceInfo)
                    .location(location)
                    .metadata(metadata)
                    .isPending(false)
                    .build();

            authHistoryRepository.save(history);
            
            log.debug("[UserAuthHistoryService] Detailed auth history recorded - user={}, type={}, success={}",
                    user.getUsername(), authType, isSuccess);
        } catch (Exception e) {
            log.error("[UserAuthHistoryService] Failed to record detailed auth history", e);
        }
    }

    /**
     * PENDING 인증 이력 저장 (예: 이메일 인증 대기)
     */
    @Transactional
    public UserAuthHistory recordPendingAuth(User user, AuthType authType, String authCode,
                                             int expirationMinutes, String ipAddress, String userAgent) {
        String maskedCode = maskAuthCode(authCode);
        
        UserAuthHistory history = UserAuthHistory.builder()
                .user(user)
                .authType(authType)
                .isSuccess(false)
                .isPending(true)
                .authTimestamp(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .authCodeMasked(maskedCode)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        UserAuthHistory saved = authHistoryRepository.save(history);
        
        log.info("[UserAuthHistoryService] Pending auth recorded - user={}, type={}, expires={}",
                user.getUsername(), authType, saved.getExpiresAt());
        
        return saved;
    }

    /**
     * HttpServletRequest에서 정보 추출하여 인증 이력 저장
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAuthHistoryFromRequest(User user, AuthType authType, boolean isSuccess,
                                            HttpServletRequest request, String failureReason) {
        try {
            String ipAddress = extractIpAddress(request);
            String userAgent = request.getHeader("User-Agent");

            recordAuthHistory(user, authType, isSuccess, ipAddress, userAgent, failureReason);
        } catch (Exception e) {
            log.error("[UserAuthHistoryService] Failed to record auth history from request", e);
        }
    }

    /**
     * 특정 사용자의 인증 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<UserAuthHistory> getUserAuthHistory(User user, Pageable pageable) {
        return authHistoryRepository.findByUserOrderByAuthTimestampDesc(user, pageable);
    }

    /**
     * 특정 사용자의 최근 로그인 이력 조회
     */
    @Transactional(readOnly = true)
    public Optional<UserAuthHistory> getLastLoginHistory(User user) {
        return authHistoryRepository.findFirstByUserAndAuthTypeAndIsSuccessTrueOrderByAuthTimestampDesc(
                user, AuthType.LOGIN);
    }

    /**
     * 특정 사용자의 최근 N개 인증 이력 조회
     */
    @Transactional(readOnly = true)
    public List<UserAuthHistory> getRecentAuthHistory(User user) {
        return authHistoryRepository.findTop10ByUserOrderByAuthTimestampDesc(user);
    }

    /**
     * 특정 기간 내 로그인 실패 횟수 조회
     */
    @Transactional(readOnly = true)
    public Long countFailedLoginAttempts(User user, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return authHistoryRepository.countFailedAttemptsSince(user, AuthType.LOGIN, since);
    }

    /**
     * 특정 IP에서의 로그인 실패 횟수 조회
     */
    @Transactional(readOnly = true)
    public Long countFailedLoginAttemptsByIp(String ipAddress, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return authHistoryRepository.countFailedAttemptsByIpSince(ipAddress, AuthType.LOGIN, since);
    }

    /**
     * 사용자의 2FA 관련 이력 조회
     */
    @Transactional(readOnly = true)
    public List<UserAuthHistory> getTwoFactorHistory(User user) {
        return authHistoryRepository.findTwoFactorHistoryByUser(user);
    }

    /**
     * 만료된 PENDING 인증 정리
     */
    @Transactional
    public void cleanupExpiredPendingAuth() {
        List<UserAuthHistory> expiredList = authHistoryRepository.findExpiredPendingAuth(LocalDateTime.now());
        
        for (UserAuthHistory history : expiredList) {
            history.setIsPending(false);
            history.setFailureReason("인증 시간 만료");
        }
        
        if (!expiredList.isEmpty()) {
            authHistoryRepository.saveAll(expiredList);
            log.info("[UserAuthHistoryService] Cleaned up {} expired pending auth records", expiredList.size());
        }
    }

    // Helper methods

    /**
     * 인증 코드 마스킹
     */
    private String maskAuthCode(String authCode) {
        if (authCode == null || authCode.isEmpty()) {
            return null;
        }
        // 6자리 TOTP 코드의 경우 "******" 로 마스킹
        return "******";
    }

    /**
     * IP 주소 추출 (프록시/로드밸런서 고려)
     */
    private String extractIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

