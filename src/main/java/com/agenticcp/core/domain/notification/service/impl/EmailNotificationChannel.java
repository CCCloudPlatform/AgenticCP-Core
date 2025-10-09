package com.agenticcp.core.domain.notification.service.impl;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 이메일 알림 채널 구현체 (MVP)
 * 
 * <p>Spring Mail을 사용하여 이메일 알림을 발송합니다.</p>
 * <p>확장 가능한 알림 시스템의 첫 번째 채널 구현체입니다.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationChannel implements NotificationChannel {

    private final JavaMailSender mailSender;

    @Override
    public String getChannelType() {
        return "EMAIL";
    }

    @Override
    public boolean isEnabled() {
        return mailSender != null;
    }

    @Override
    public NotificationResponse send(NotificationRequest request) {
        try {
            log.info("이메일 알림 발송 시작: {}", request.getNotificationId());

            // 이메일 메시지 생성
            SimpleMailMessage message = createEmailMessage(request);
            
            // 이메일 발송
            mailSender.send(message);
            
            log.info("이메일 알림 발송 성공: {}", request.getNotificationId());
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.SENT)
                    .message("이메일이 성공적으로 발송되었습니다.")
                    .sentAt(LocalDateTime.now())
                    .success(true)
                    .build();

        } catch (Exception e) {
            log.error("이메일 알림 발송 실패: {}", request.getNotificationId(), e);
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .errorMessage("이메일 발송 중 오류가 발생했습니다: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    @Override
    public boolean testConnection() {
        if (mailSender == null) {
            log.warn("JavaMailSender가 설정되지 않았습니다.");
            return false;
        }
        
        try {
            // 간단한 테스트 이메일 발송
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setTo("test@example.com");
            testMessage.setSubject("연결 테스트");
            testMessage.setText("이메일 서비스 연결 테스트입니다.");
            
            mailSender.send(testMessage);
            return true;
        } catch (Exception e) {
            log.error("이메일 서비스 연결 테스트 실패", e);
            return false;
        }
    }

    @Override
    public boolean validateConfiguration() {
        // 이메일 서비스 설정 검증
        return mailSender != null;
    }

    /**
     * 이메일 메시지 생성
     */
    private SimpleMailMessage createEmailMessage(NotificationRequest request) {
        SimpleMailMessage message = new SimpleMailMessage();
        
        // 수신자 설정
        message.setTo(request.getRecipient());
        
        // 제목 설정
        message.setSubject(request.getTitle());
        
        // 내용 설정
        String emailContent = buildEmailContent(request);
        message.setText(emailContent);
        
        // 발신자 설정 (application.yml에서 설정)
        // message.setFrom("noreply@agenticcp.com");
        
        return message;
    }

    /**
     * 이메일 내용 구성
     */
    private String buildEmailContent(NotificationRequest request) {
        StringBuilder content = new StringBuilder();
        
        // 기본 내용
        content.append(request.getContent()).append("\n\n");
        
        // 메타데이터 추가
        if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
            content.append("=== 추가 정보 ===\n");
            for (Map.Entry<String, Object> entry : request.getMetadata().entrySet()) {
                content.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        
        // 데이터 추가
        if (request.getData() != null && !request.getData().isEmpty()) {
            content.append("\n=== 알림 데이터 ===\n");
            for (Map.Entry<String, Object> entry : request.getData().entrySet()) {
                content.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        
        // 푸터 추가
        content.append("\n---\n");
        content.append("이 알림은 AgenticCP 플랫폼에서 자동으로 발송되었습니다.\n");
        content.append("발송 시간: ").append(LocalDateTime.now()).append("\n");
        content.append("알림 ID: ").append(request.getNotificationId());
        
        return content.toString();
    }
}
