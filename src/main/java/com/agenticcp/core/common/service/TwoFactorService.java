package com.agenticcp.core.common.service;

import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * 2FA (Two-Factor Authentication) 서비스
 * TOTP 기반 2FA 기능을 제공합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@Slf4j
public class TwoFactorService {
    
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QRCodeWriter qrCodeWriter = new QRCodeWriter();
    
    /**
     * TOTP 시크릿 키 생성
     * 
     * @return Base32 인코딩된 시크릿 키
     */
    public String generateSecretKey() {
        log.debug("[TwoFactorService] generateSecretKey - 시크릿 키 생성");
        String secretKey = secretGenerator.generate();
        log.debug("[TwoFactorService] generateSecretKey - 시크릿 키 생성 완료");
        return secretKey;
    }
    
    /**
     * QR 코드 URL 생성
     * 
     * @param username 사용자명
     * @param secretKey TOTP 시크릿 키
     * @return otpauth:// 형식의 QR 코드 URL
     */
    public String generateQrCodeUrl(String username, String secretKey) {
        log.debug("[TwoFactorService] generateQrCodeUrl - username={}", username);
        
        String appName = "AgenticCP";
        String qrCodeUrl = String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            appName, username, secretKey, appName
        );
        
        log.debug("[TwoFactorService] generateQrCodeUrl - QR 코드 URL 생성 완료");
        return qrCodeUrl;
    }
    
    /**
     * QR 코드 이미지 생성 (Base64 인코딩)
     * 
     * @param qrCodeUrl QR 코드 URL
     * @return Base64 인코딩된 PNG 이미지
     */
    public String generateQrCodeImage(String qrCodeUrl) {
        log.debug("[TwoFactorService] generateQrCodeImage - QR 코드 이미지 생성 시작");
        
        try {
            BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 200, 200);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();
            
            String base64Image = Base64.getEncoder().encodeToString(pngData);
            log.debug("[TwoFactorService] generateQrCodeImage - QR 코드 이미지 생성 완료");
            
            return base64Image;
        } catch (WriterException e) {
            log.error("[TwoFactorService] generateQrCodeImage - QR 코드 생성 실패", e);
            throw new BusinessException(AuthErrorCode.QR_CODE_GENERATION_FAILED);
        } catch (Exception e) {
            log.error("[TwoFactorService] generateQrCodeImage - 예상치 못한 오류", e);
            throw new BusinessException(AuthErrorCode.QR_CODE_GENERATION_FAILED);
        }
    }
    
    /**
     * TOTP 코드 검증 (±1 윈도우 허용)
     * 
     * @param secretKey TOTP 시크릿 키
     * @param code 검증할 TOTP 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCode(String secretKey, String code) {
        log.debug("[TwoFactorService] verifyCode - TOTP 코드 검증 시작");
        
        try {
            boolean isValid = codeVerifier.isValidCode(secretKey, code);
            log.debug("[TwoFactorService] verifyCode - TOTP 코드 검증 결과: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("[TwoFactorService] verifyCode - TOTP 코드 검증 실패", e);
            return false;
        }
    }
    
    /**
     * TOTP 코드 검증 (엄격한 모드)
     * 
     * @param secretKey TOTP 시크릿 키
     * @param code 검증할 TOTP 코드
     * @return 검증 성공 여부
     */
    public boolean verifyCodeStrict(String secretKey, String code) {
        log.debug("[TwoFactorService] verifyCodeStrict - TOTP 코드 엄격 검증 시작");
        
        try {
            // 윈도우를 0으로 설정하여 엄격한 검증
            boolean isValid = codeVerifier.isValidCode(secretKey, code, 0);
            log.debug("[TwoFactorService] verifyCodeStrict - TOTP 코드 엄격 검증 결과: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("[TwoFactorService] verifyCodeStrict - TOTP 코드 엄격 검증 실패", e);
            return false;
        }
    }
}
