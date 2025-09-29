package com.agenticcp.core.common.service;

import com.agenticcp.core.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * 2FA (Two-Factor Authentication) 서비스
 * TOTP (Time-based One-Time Password) 기반 2FA 구현
 */
@Service
@Slf4j
public class TwoFactorService {

    private final RedisTemplate<String, String> redisTemplate;
    
    public TwoFactorService(@org.springframework.beans.factory.annotation.Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    private static final String ALGORITHM = "HmacSHA1";
    private static final int CODE_LENGTH = 6;
    private static final int TIME_STEP = 30; // 30초
    private static final String TOTP_ISSUER = "AgenticCP";
    private static final String TEMP_SECRET_PREFIX = "temp_2fa_secret:";
    private static final String VERIFICATION_ATTEMPTS_PREFIX = "2fa_attempts:";

    /**
     * 2FA 시크릿 키 생성
     */
    public String generateSecretKey() {
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }

    /**
     * QR 코드용 URI 생성
     */
    public String generateQRCodeUri(User user, String secretKey) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                TOTP_ISSUER,
                user.getEmail(),
                secretKey,
                TOTP_ISSUER);
    }

    /**
     * 2FA 코드 검증
     */
    public boolean verifyCode(String secretKey, String code) {
        try {
            long currentTime = System.currentTimeMillis() / 1000 / TIME_STEP;
            
            // 현재 시간 기준으로 ±1 시간 윈도우에서 검증
            for (int i = -1; i <= 1; i++) {
                if (generateCode(secretKey, currentTime + i).equals(code)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("2FA 코드 검증 실패: {}", e.getMessage());
            return false;
        }
    }

    /**
     * TOTP 코드 생성
     */
    public String generateCode(String secretKey, long timeStep) {
        try {
            byte[] key = Base64.getDecoder().decode(secretKey);
            byte[] time = ByteBuffer.allocate(8).putLong(timeStep).array();
            
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, ALGORITHM);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(time);
            int offset = hash[hash.length - 1] & 0xf;
            int code = ((hash[offset] & 0x7f) << 24) |
                      ((hash[offset + 1] & 0xff) << 16) |
                      ((hash[offset + 2] & 0xff) << 8) |
                      (hash[offset + 3] & 0xff);
            
            code = code % (int) Math.pow(10, CODE_LENGTH);
            return String.format("%0" + CODE_LENGTH + "d", code);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TOTP 코드 생성 실패: {}", e.getMessage());
            throw new RuntimeException("2FA 코드 생성 실패", e);
        }
    }

    /**
     * 현재 시간 기준 TOTP 코드 생성
     */
    public String generateCurrentCode(String secretKey) {
        long currentTime = System.currentTimeMillis() / 1000 / TIME_STEP;
        return generateCode(secretKey, currentTime);
    }

    /**
     * 2FA 설정을 위한 임시 시크릿 저장
     */
    public void storeTemporarySecret(String username, String secretKey) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(
                    TEMP_SECRET_PREFIX + username,
                    secretKey,
                    Duration.ofMinutes(10) // 10분간 유효
            );
        } else {
            log.warn("Redis가 비활성화되어 임시 시크릿 저장을 건너뜁니다: username={}", username);
        }
    }

    /**
     * 임시 시크릿 조회
     */
    public String getTemporarySecret(String username) {
        return redisTemplate.opsForValue().get(TEMP_SECRET_PREFIX + username);
    }

    /**
     * 임시 시크릿 삭제
     */
    public void removeTemporarySecret(String username) {
        redisTemplate.delete(TEMP_SECRET_PREFIX + username);
    }

    /**
     * 2FA 검증 시도 횟수 증가
     */
    public void incrementVerificationAttempts(String username) {
        String key = VERIFICATION_ATTEMPTS_PREFIX + username;
        String attempts = redisTemplate.opsForValue().get(key);
        int count = attempts != null ? Integer.parseInt(attempts) : 0;
        redisTemplate.opsForValue().set(key, String.valueOf(count + 1), Duration.ofMinutes(15));
    }

    /**
     * 2FA 검증 시도 횟수 조회
     */
    public int getVerificationAttempts(String username) {
        String key = VERIFICATION_ATTEMPTS_PREFIX + username;
        String attempts = redisTemplate.opsForValue().get(key);
        return attempts != null ? Integer.parseInt(attempts) : 0;
    }

    /**
     * 2FA 검증 시도 횟수 초기화
     */
    public void resetVerificationAttempts(String username) {
        redisTemplate.delete(VERIFICATION_ATTEMPTS_PREFIX + username);
    }

    /**
     * 2FA 검증 시도 횟수 제한 확인
     */
    public boolean isVerificationAttemptsExceeded(String username) {
        return getVerificationAttempts(username) >= 5; // 5회 제한
    }
}
