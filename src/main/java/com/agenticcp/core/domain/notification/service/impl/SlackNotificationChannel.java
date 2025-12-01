package com.agenticcp.core.domain.notification.service.impl;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.service.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 슬랙 웹훅 알림 채널 구현체
 * 
 * <p>Slack Incoming Webhook을 사용하여 슬랙 채널로 알림을 발송합니다.</p>
 * <p>확장 가능한 알림 시스템의 슬랙 채널 구현체입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SlackNotificationChannel implements NotificationChannel {

    private final RestTemplate restTemplate;

    @Value("${slack.webhook.url:}")
    private String webhookUrl;

    /**
     * 채널 타입 반환
     * 
     * @return 채널 타입 (SLACK)
     */
    @Override
    public String getChannelType() {
        return "SLACK";
    }

    /**
     * 채널 활성화 여부 확인
     * 
     * @return 활성화 여부
     */
    @Override
    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    /**
     * 알림 발송
     * 
     * @param request 알림 요청 정보
     * @return 알림 응답 정보
     */
    @Override
    public NotificationResponse send(NotificationRequest request) {
        try {
            log.info("슬랙 알림 발송 시작: {}", request.getNotificationId());

            // 슬랙 메시지 생성
            Map<String, Object> slackMessage = createSlackMessage(request);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(slackMessage, headers);
            
            // 슬랙 웹훅으로 POST 요청
            restTemplate.postForEntity(webhookUrl, entity, String.class);
            
            log.info("슬랙 알림 발송 성공: {}", request.getNotificationId());
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.SENT)
                    .message("슬랙 메시지가 성공적으로 발송되었습니다.")
                    .sentAt(LocalDateTime.now())
                    .success(true)
                    .build();

        } catch (RestClientException e) {
            log.error("슬랙 웹훅 요청 실패: {}", request.getNotificationId(), e);
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .errorMessage("슬랙 웹훅 요청 실패: " + e.getMessage())
                    .success(false)
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("슬랙 메시지 생성 실패: {}", request.getNotificationId(), e);
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .errorMessage("슬랙 메시지 생성 실패: " + e.getMessage())
                    .success(false)
                    .build();
        } catch (Exception e) {
            log.error("슬랙 알림 발송 실패: {}", request.getNotificationId(), e);
            
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .errorMessage("슬랙 메시지 발송 중 오류가 발생했습니다: " + e.getMessage())
                    .success(false)
                    .build();
        }
    }

    /**
     * 연결 테스트
     * 
     * @return 연결 성공 여부
     */
    @Override
    public boolean testConnection() {
        if (!isEnabled()) {
            log.warn("슬랙 웹훅 URL이 설정되지 않았습니다.");
            return false;
        }
        
        try {
            // 테스트 메시지 발송
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("text", "✅ 슬랙 연결 테스트 성공!");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(testMessage, headers);
            
            restTemplate.postForEntity(webhookUrl, entity, String.class);
            return true;
        } catch (RestClientException e) {
            log.error("슬랙 연결 테스트 실패", e);
            return false;
        } catch (Exception e) {
            log.error("슬랙 연결 테스트 실패", e);
            return false;
        }
    }

    /**
     * 설정 검증
     * 
     * @return 설정 유효 여부
     */
    @Override
    public boolean validateConfiguration() {
        return isEnabled();
    }

    /**
     * 슬랙 메시지 생성
     * 
     * <p>Slack Block Kit 형식으로 구조화된 메시지를 생성합니다.</p>
     * 
     * @param request 알림 요청 정보
     * @return 슬랙 메시지 맵
     */
    private Map<String, Object> createSlackMessage(NotificationRequest request) {
        Map<String, Object> message = new HashMap<>();
        
        // 우선순위에 따른 이모지 선택
        String emoji = getPriorityEmoji(request.getPriority());
        
        // 메시지 텍스트 (폴백용)
        message.put("text", emoji + " " + request.getTitle());
        
        // Block Kit 형식의 구조화된 메시지
        Map<String, Object>[] blocks = createBlocks(request, emoji);
        message.put("blocks", blocks);
        
        return message;
    }

    /**
     * 슬랙 Block Kit 블록 생성
     * 
     * @param request 알림 요청 정보
     * @param emoji 우선순위 이모지
     * @return Block Kit 블록 배열
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object>[] createBlocks(NotificationRequest request, String emoji) {
        Map<String, Object>[] blocks = new Map[3];
        
        // 1. 헤더 블록
        Map<String, Object> headerBlock = new HashMap<>();
        headerBlock.put("type", "header");
        Map<String, Object> headerText = new HashMap<>();
        headerText.put("type", "plain_text");
        headerText.put("text", emoji + " " + request.getTitle());
        headerBlock.put("text", headerText);
        blocks[0] = headerBlock;
        
        // 2. 본문 블록
        Map<String, Object> contentBlock = new HashMap<>();
        contentBlock.put("type", "section");
        Map<String, Object> contentText = new HashMap<>();
        contentText.put("type", "mrkdwn");
        contentText.put("text", formatContentForSlack(request));
        contentBlock.put("text", contentText);
        blocks[1] = contentBlock;
        
        // 3. 컨텍스트 블록 (메타데이터)
        Map<String, Object> contextBlock = new HashMap<>();
        contextBlock.put("type", "context");
        Map<String, Object>[] contextElements = new Map[1];
        Map<String, Object> contextText = new HashMap<>();
        contextText.put("type", "mrkdwn");
        contextText.put("text", String.format("*알림 ID:* `%s` | *발송 시간:* %s", 
            request.getNotificationId(), LocalDateTime.now()));
        contextElements[0] = contextText;
        contextBlock.put("elements", contextElements);
        blocks[2] = contextBlock;
        
        return blocks;
    }

    /**
     * 슬랙 포맷으로 내용 변환
     * 
     * @param request 알림 요청 정보
     * @return 포맷팅된 내용 문자열
     */
    private String formatContentForSlack(NotificationRequest request) {
        StringBuilder content = new StringBuilder();
        
        // 기본 내용
        content.append(request.getContent()).append("\n\n");
        
        // 알림 데이터 추가
        if (request.getData() != null && !request.getData().isEmpty()) {
            content.append("*📊 알림 데이터:*\n");
            for (Map.Entry<String, Object> entry : request.getData().entrySet()) {
                content.append(String.format("• *%s:* %s\n", 
                    formatFieldName(entry.getKey()), entry.getValue()));
            }
        }
        
        // 우선순위 표시
        if (request.getPriority() != null) {
            content.append("\n*우선순위:* `").append(request.getPriority()).append("`");
        }
        
        return content.toString();
    }

    /**
     * 필드명을 읽기 쉽게 변환
     * 
     * @param fieldName 필드명
     * @return 변환된 필드명
     */
    private String formatFieldName(String fieldName) {
        return switch (fieldName) {
            case "metricName" -> "메트릭명";
            case "metricValue" -> "현재 값";
            case "thresholdValue" -> "임계값";
            case "operator" -> "연산자";
            case "severity" -> "심각도";
            case "violationTime" -> "위반 시간";
            case "collectorType" -> "수집기 타입";
            case "errorMessage" -> "오류 메시지";
            case "serviceName" -> "서비스명";
            case "previousStatus" -> "이전 상태";
            case "currentStatus" -> "현재 상태";
            default -> fieldName;
        };
    }

    /**
     * 우선순위에 따른 이모지 선택
     * 
     * @param priority 알림 우선순위
     * @return 우선순위 이모지
     */
    private String getPriorityEmoji(com.agenticcp.core.domain.notification.enums.NotificationPriority priority) {
        if (priority == null) {
            return "📢";
        }
        
        return switch (priority) {
            case URGENT -> "🔥";
            case HIGH -> "⚠️";
            case MEDIUM -> "📊";
            case LOW -> "ℹ️";
        };
    }
}

