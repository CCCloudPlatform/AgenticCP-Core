package com.agenticcp.core.common.service;

import com.agenticcp.core.common.enums.AuthErrorCode;
import com.agenticcp.core.common.exception.BusinessException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 이메일 발송 서비스
 * Thymeleaf 템플릿 기반 HTML 이메일 발송
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${spring.mail.from:noreply@agenticcp.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * 비밀번호 재설정 이메일 발송
     * 
     * @param to 수신자 이메일
     * @param username 사용자명
     * @param resetToken 재설정 토큰
     */
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        log.info("[EmailService] sendPasswordResetEmail - to={}, username={}", to, username);
        
        try {
            String resetUrl = frontendUrl + "/password/reset?token=" + resetToken;
            
            Context context = new Context();
            context.setVariable("username", username);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("resetToken", resetToken);
            context.setVariable("expirationMinutes", 60); // 1시간
            
            String htmlContent = templateEngine.process("email/password-reset", context);
            
            sendEmail(to, "비밀번호 재설정 요청", htmlContent);
            
            log.info("[EmailService] sendPasswordResetEmail - success to={}", to);
            
        } catch (Exception e) {
            log.error("[EmailService] sendPasswordResetEmail - failed to={}", to, e);
            throw new BusinessException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }

    /**
     * 이메일 발송 (공통 메서드)
     * 
     * @param to 수신자 이메일
     * @param subject 제목
     * @param htmlContent HTML 내용
     */
    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML
            
            mailSender.send(message);
            
            log.info("[EmailService] sendEmail - success to={}, subject={}", to, subject);
            
        } catch (MessagingException e) {
            log.error("[EmailService] sendEmail - failed to={}, subject={}", to, subject, e);
            throw new BusinessException(AuthErrorCode.EMAIL_SEND_FAILED);
        }
    }
}

