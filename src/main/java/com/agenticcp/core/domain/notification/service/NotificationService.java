package com.agenticcp.core.domain.notification.service;

import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.entity.NotificationTemplate;
import com.agenticcp.core.domain.notification.enums.NotificationStatus;
import com.agenticcp.core.domain.notification.repository.NotificationChannelRepository;
import com.agenticcp.core.domain.notification.repository.NotificationRepository;
import com.agenticcp.core.domain.notification.repository.NotificationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 알림 서비스
 * 
 * <p>확장 가능한 알림 시스템의 핵심 서비스로, 다음과 같은 기능을 제공합니다:</p>
 * <ul>
 *   <li>알림 발송 및 상태 관리</li>
 *   <li>예약된 알림 처리</li>
 *   <li>실패한 알림 재시도</li>
 *   <li>모니터링 도메인과의 연동</li>
 * </ul>
 * 
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    /** 알림 데이터 저장소 */
    private final NotificationRepository notificationRepository;
    
    /** 알림 템플릿 저장소 */
    private final NotificationTemplateRepository templateRepository;
    
    /** 알림 채널 설정 저장소 */
    private final NotificationChannelRepository channelRepository;
    
    /** 알림 채널 팩토리 (확장 가능한 채널 관리) */
    private final NotificationChannelFactory channelFactory;
    
    /** JSON 변환을 위한 ObjectMapper */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 알림 발송
     * 
     * <p>알림 발송의 전체 프로세스를 관리합니다:</p>
     * <ol>
     *   <li>알림 엔티티 생성 및 데이터베이스 저장</li>
     *   <li>채널별 알림 발송 (이메일, 슬랙, 웹훅 등)</li>
     *   <li>발송 결과에 따른 상태 업데이트</li>
     * </ol>
     * 
     * @param request 알림 발송 요청 (제목, 내용, 수신자, 채널 등)
     * @return 알림 발송 결과 (성공/실패, 발송 시간, 오류 메시지 등)
     * @throws RuntimeException 채널을 찾을 수 없거나 지원하지 않는 채널 타입인 경우
     */
    @Transactional
    public NotificationResponse sendNotification(NotificationRequest request) {
        try {
            log.info("알림 발송 시작: {}", request.getNotificationId());

            // 1. 알림 엔티티 생성 및 저장
            // - 요청 데이터를 Notification 엔티티로 변환
            // - 데이터베이스에 저장하여 발송 이력 관리
            Notification notification = createNotificationEntity(request);
            notification = notificationRepository.save(notification);

            // 2. 채널별 알림 발송
            // - 채널 설정 조회 (이메일, 슬랙, 웹훅 등)
            // - 해당 채널 구현체로 실제 발송 수행
            NotificationResponse response = sendViaChannel(request, notification);

            // 3. 알림 상태 업데이트
            // - 발송 결과에 따른 상태 변경 (SENT, FAILED 등)
            // - 발송 시간, 오류 메시지 등 기록
            updateNotificationStatus(notification, response);

            log.info("알림 발송 완료: {}, 상태: {}", request.getNotificationId(), response.getStatus());
            return response;

        } catch (Exception e) {
            log.error("알림 발송 실패: {}", request.getNotificationId(), e);
            // 실패 시 실패 응답 생성
            return NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    /**
     * 모니터링 알림 발송 (Alert 엔티티 기반)
     * 
     * <p>모니터링 도메인의 Alert 엔티티와 연동하여 알림을 발송합니다.</p>
     * <p>feature/39 브랜치의 모니터링 도메인이 머지된 후 구현 예정입니다.</p>
     * 
     * @param alertId 모니터링 Alert ID
     * @param alertData 알림 데이터 (메트릭 정보, 임계값 등)
     * @return 알림 발송 결과
     * @deprecated 모니터링 도메인 머지 후 구현 예정
     */
    @Transactional
    public NotificationResponse sendMonitoringAlert(Long alertId, Map<String, Object> alertData) {
        // TODO: Alert 엔티티에서 알림 정보를 가져와서 알림 발송
        // 이 부분은 모니터링 도메인과의 연동에서 구현
        // feature/39 브랜치 머지 후 구현 예정
        return null;
    }

    /**
     * 예약된 알림 처리
     * 
     * <p>현재 시간에 도달한 예약된 알림들을 발송합니다.</p>
     * <p>스케줄러에 의해 주기적으로 호출되어야 합니다.</p>
     * 
     * <p>처리 과정:</p>
     * <ol>
     *   <li>현재 시간에 도달한 PENDING 상태의 알림 조회</li>
     *   <li>각 알림을 NotificationRequest로 변환</li>
     *   <li>알림 발송 수행</li>
     * </ol>
     * 
     * @see NotificationRepository#findScheduledNotifications(LocalDateTime, NotificationStatus)
     */
    @Transactional
    public void processScheduledNotifications() {
        // 현재 시간에 도달한 예약된 알림 조회
        List<Notification> scheduledNotifications = notificationRepository
                .findScheduledNotifications(LocalDateTime.now(), NotificationStatus.PENDING);

        // 각 예약된 알림 처리
        for (Notification notification : scheduledNotifications) {
            try {
                // Notification 엔티티를 NotificationRequest로 변환
                NotificationRequest request = convertToRequest(notification);
                // 알림 발송
                sendNotification(request);
            } catch (Exception e) {
                log.error("예약된 알림 처리 실패: {}", notification.getNotificationId(), e);
            }
        }
    }

    /**
     * 재시도가 필요한 알림 처리
     * 
     * <p>실패한 알림 중 재시도 가능한 알림들을 다시 발송합니다.</p>
     * <p>스케줄러에 의해 주기적으로 호출되어야 합니다.</p>
     * 
     * <p>재시도 조건:</p>
     * <ul>
     *   <li>상태가 FAILED인 알림</li>
     *   <li>재시도 횟수가 최대 재시도 횟수(3회) 미만</li>
     * </ul>
     * 
     * <p>처리 과정:</p>
     * <ol>
     *   <li>재시도 가능한 알림 조회</li>
     *   <li>재시도 횟수 증가</li>
     *   <li>알림 재발송</li>
     * </ol>
     * 
     * @see NotificationRepository#findRetryableNotifications(NotificationStatus, Integer)
     */
    @Transactional
    public void processRetryableNotifications() {
        // 재시도 가능한 알림 조회 (FAILED 상태, 재시도 횟수 3회 미만)
        List<Notification> retryableNotifications = notificationRepository
                .findRetryableNotifications(NotificationStatus.FAILED, 3);

        // 각 재시도 가능한 알림 처리
        for (Notification notification : retryableNotifications) {
            try {
                // 재시도 횟수 증가
                notification.setRetryCount(notification.getRetryCount() + 1);
                notificationRepository.save(notification);

                // Notification 엔티티를 NotificationRequest로 변환
                NotificationRequest request = convertToRequest(notification);
                // 알림 재발송
                sendNotification(request);
            } catch (Exception e) {
                log.error("재시도 알림 처리 실패: {}", notification.getNotificationId(), e);
            }
        }
    }

    /**
     * 알림 엔티티 생성
     * 
     * <p>NotificationRequest를 Notification 엔티티로 변환합니다.</p>
     * <p>데이터베이스 저장을 위한 엔티티를 생성하고 초기 상태를 설정합니다.</p>
     * 
     * @param request 알림 요청 데이터
     * @return 저장용 Notification 엔티티
     */
    private Notification createNotificationEntity(NotificationRequest request) {
        return Notification.builder()
                .notificationId(request.getNotificationId())                    // 알림 고유 ID
                .tenantId(request.getTenantId())                              // 테넌트 ID (멀티테넌트 지원)
                .userId(request.getUserId())                                  // 사용자 ID
                .templateId(request.getTemplateId() != null ? Long.parseLong(request.getTemplateId()) : null)  // 템플릿 ID (선택사항)
                .channelId(Long.parseLong(request.getChannelId()))            // 채널 ID (이메일, 슬랙 등)
                .title(request.getTitle())                                    // 알림 제목
                .content(request.getContent())                                // 알림 내용
                .notificationType(request.getType())                          // 알림 타입 (SYSTEM, ALERT 등)
                .priority(request.getPriority())                              // 우선순위 (LOW, MEDIUM, HIGH, URGENT)
                .status(NotificationStatus.PENDING)                           // 초기 상태: 대기
                .data(convertMapToJson(request.getData()))                    // 추가 데이터 (JSON)
                .metadata(convertMapToJson(request.getMetadata()))             // 메타데이터 (JSON)
                .scheduledAt(request.getScheduledAt())                         // 예약 시간 (즉시 발송 시 null)
                .retryCount(0)                                                // 재시도 횟수 초기화
                .build();
    }

    /**
     * 채널별 알림 발송
     * 
     * <p>채널 설정을 조회하고 해당 채널 구현체로 알림을 발송합니다.</p>
     * <p>확장 가능한 채널 시스템의 핵심 로직입니다.</p>
     * 
     * <p>처리 과정:</p>
     * <ol>
     *   <li>채널 ID로 채널 설정 조회</li>
     *   <li>채널 타입에 따른 구현체 선택</li>
     *   <li>실제 알림 발송 수행</li>
     * </ol>
     * 
     * @param request 알림 요청 데이터
     * @param notification 알림 엔티티 (참조용)
     * @return 알림 발송 결과
     * @throws RuntimeException 채널을 찾을 수 없거나 지원하지 않는 채널 타입인 경우
     */
    private NotificationResponse sendViaChannel(NotificationRequest request, Notification notification) {
        // 1. 채널 설정 조회
        // - 채널 ID로 데이터베이스에서 채널 설정 조회
        // - 채널 타입, 설정 정보, 인증 정보 등 포함
        NotificationChannelEntity channelEntity = channelRepository.findById(Long.parseLong(request.getChannelId()))
                .orElseThrow(() -> new RuntimeException("채널을 찾을 수 없습니다: " + request.getChannelId()));

        // 2. 채널 구현체 선택
        // - 채널 타입에 따른 구현체 선택 (이메일, 슬랙, 웹훅 등)
        // - NotificationChannelFactory를 통해 동적 선택
        NotificationChannel channel = channelFactory.getChannel(channelEntity.getChannelType().name());
        
        if (channel == null) {
            throw new RuntimeException("지원하지 않는 채널 타입입니다: " + channelEntity.getChannelType());
        }

        // 3. 실제 알림 발송
        // - 선택된 채널 구현체로 알림 발송
        // - 각 채널별 특화된 발송 로직 수행
        return channel.send(request);
    }

    /**
     * 알림 상태 업데이트
     * 
     * <p>알림 발송 결과에 따라 알림 엔티티의 상태를 업데이트합니다.</p>
     * <p>발송 시간, 오류 메시지 등도 함께 기록합니다.</p>
     * 
     * @param notification 업데이트할 알림 엔티티
     * @param response 알림 발송 결과
     */
    private void updateNotificationStatus(Notification notification, NotificationResponse response) {
        // 발송 결과에 따른 상태 업데이트
        notification.setStatus(response.getStatus());                    // 발송 상태 (SENT, FAILED 등)
        notification.setSentAt(response.getSentAt());                   // 발송 시간
        notification.setErrorMessage(response.getErrorMessage());       // 오류 메시지 (실패 시)
        
        // 데이터베이스에 상태 저장
        notificationRepository.save(notification);
    }

    /**
     * Notification 엔티티를 NotificationRequest로 변환
     * 
     * <p>데이터베이스에 저장된 Notification 엔티티를 NotificationRequest로 변환합니다.</p>
     * <p>예약된 알림 처리나 재시도 시 사용됩니다.</p>
     * 
     * @param notification 변환할 Notification 엔티티
     * @return NotificationRequest 객체
     */
    private NotificationRequest convertToRequest(Notification notification) {
        return NotificationRequest.builder()
                .notificationId(notification.getNotificationId())                    // 알림 고유 ID
                .tenantId(notification.getTenantId())                              // 테넌트 ID
                .userId(notification.getUserId())                                  // 사용자 ID
                .templateId(notification.getTemplateId() != null ? notification.getTemplateId().toString() : null)  // 템플릿 ID (String 변환)
                .channelId(notification.getChannelId().toString())                 // 채널 ID (String 변환)
                .title(notification.getTitle())                                    // 알림 제목
                .content(notification.getContent())                                // 알림 내용
                .type(notification.getNotificationType())                          // 알림 타입
                .priority(notification.getPriority())                              // 우선순위
                .data(convertJsonToMap(notification.getData()))                    // 추가 데이터 (JSON → Map)
                .metadata(convertJsonToMap(notification.getMetadata()))             // 메타데이터 (JSON → Map)
                .scheduledAt(notification.getScheduledAt())                       // 예약 시간
                .retryCount(notification.getRetryCount())                          // 재시도 횟수
                .build();
    }

    /**
     * Map을 JSON 문자열로 변환
     * 
     * <p>알림 데이터나 메타데이터를 JSON 형태로 변환하여 데이터베이스에 저장합니다.</p>
     * <p>Jackson ObjectMapper를 사용하여 변환합니다.</p>
     * 
     * @param map 변환할 Map 객체
     * @return JSON 문자열 (null인 경우 null 반환)
     */
    private String convertMapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("Map을 JSON으로 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * JSON 문자열을 Map으로 변환
     * 
     * <p>데이터베이스에서 조회한 JSON 데이터를 Map으로 변환합니다.</p>
     * <p>Jackson ObjectMapper를 사용하여 변환합니다.</p>
     * 
     * @param json 변환할 JSON 문자열
     * @return Map 객체 (null인 경우 null 반환)
     */
    private Map<String, Object> convertJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("JSON을 Map으로 변환 중 오류 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    // ==================== 채널 관리 메서드들 ====================

    /**
     * 활성화된 알림 채널 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 활성화된 알림 채널 목록
     */
    public List<NotificationChannelEntity> getActiveChannels(Long tenantId) {
        return channelRepository.findActiveChannelsByTenant(tenantId);
    }

    /**
     * 알림 채널 생성
     * 
     * @param channel 생성할 채널 정보
     * @return 생성된 채널
     */
    @Transactional
    public NotificationChannelEntity createChannel(NotificationChannelEntity channel) {
        log.info("알림 채널 생성: channelName={}, tenantId={}", channel.getChannelName(), channel.getTenantId());
        
        // 채널 설정 검증
        validateChannelConfiguration(channel);
        
        // 채널 저장
        NotificationChannelEntity savedChannel = channelRepository.save(channel);
        
        log.info("알림 채널 생성 완료: channelId={}", savedChannel.getId());
        return savedChannel;
    }

    /**
     * 알림 채널 수정
     * 
     * @param channelId 수정할 채널 ID
     * @param channel 수정할 채널 정보
     * @return 수정된 채널
     */
    @Transactional
    public NotificationChannelEntity updateChannel(Long channelId, NotificationChannelEntity channel) {
        log.info("알림 채널 수정: channelId={}", channelId);
        
        // 기존 채널 조회
        NotificationChannelEntity existingChannel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("채널을 찾을 수 없습니다: " + channelId));
        
        // 채널 정보 업데이트
        existingChannel.setChannelName(channel.getChannelName());
        existingChannel.setDescription(channel.getDescription());
        existingChannel.setChannelType(channel.getChannelType());
        existingChannel.setConfiguration(channel.getConfiguration());
        existingChannel.setCredentials(channel.getCredentials());
        existingChannel.setIsActive(channel.getIsActive());
        existingChannel.setMetadata(channel.getMetadata());
        
        // 채널 설정 검증
        validateChannelConfiguration(existingChannel);
        
        // 채널 저장
        NotificationChannelEntity updatedChannel = channelRepository.save(existingChannel);
        
        log.info("알림 채널 수정 완료: channelId={}", updatedChannel.getId());
        return updatedChannel;
    }

    /**
     * 알림 채널 삭제
     * 
     * @param channelId 삭제할 채널 ID
     */
    @Transactional
    public void deleteChannel(Long channelId) {
        log.info("알림 채널 삭제: channelId={}", channelId);
        
        // 기존 채널 조회
        NotificationChannelEntity existingChannel = channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("채널을 찾을 수 없습니다: " + channelId));
        
        // 소프트 삭제 (isDeleted = true)
        existingChannel.setIsDeleted(true);
        channelRepository.save(existingChannel);
        
        log.info("알림 채널 삭제 완료: channelId={}", channelId);
    }

    /**
     * 알림 채널 상세 조회
     * 
     * @param channelId 조회할 채널 ID
     * @return 알림 채널 정보
     */
    public NotificationChannelEntity getChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new RuntimeException("채널을 찾을 수 없습니다: " + channelId));
    }

    /**
     * 채널 설정 검증
     * 
     * @param channel 검증할 채널
     */
    private void validateChannelConfiguration(NotificationChannelEntity channel) {
        // 채널 타입에 따른 설정 검증
        NotificationChannel channelImpl = channelFactory.getChannel(channel.getChannelType().name());
        
        if (channelImpl != null) {
            // 채널 구현체의 설정 검증 메서드 호출
            boolean isValid = channelImpl.validateConfiguration();
            if (!isValid) {
                throw new RuntimeException("채널 설정이 유효하지 않습니다: " + channel.getChannelName());
            }
        }
        
        log.debug("채널 설정 검증 완료: channelName={}", channel.getChannelName());
    }

    // ==================== 비동기 알림 발송 메서드들 ====================

    /**
     * 비동기 알림 발송
     * 
     * @param request 알림 발송 요청
     * @return CompletableFuture로 래핑된 알림 응답
     */
    @Async("notificationExecutor")
    public CompletableFuture<NotificationResponse> sendNotificationAsync(NotificationRequest request) {
        try {
            log.info("비동기 알림 발송 시작: {}", request.getNotificationId());
            
            NotificationResponse response = sendNotification(request);
            
            log.info("비동기 알림 발송 완료: {}", request.getNotificationId());
            return CompletableFuture.completedFuture(response);
            
        } catch (Exception e) {
            log.error("비동기 알림 발송 실패: {}", request.getNotificationId(), e);
            return CompletableFuture.completedFuture(
                NotificationResponse.builder()
                    .notificationId(request.getNotificationId())
                    .status(NotificationStatus.FAILED)
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * 비동기 예약된 알림 처리
     * 
     * <p>예약된 알림들을 비동기로 처리합니다.</p>
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> processScheduledNotificationsAsync() {
        try {
            log.info("비동기 예약된 알림 처리 시작");
            processScheduledNotifications();
            log.info("비동기 예약된 알림 처리 완료");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 예약된 알림 처리 실패", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 비동기 재시도 가능한 알림 처리
     * 
     * <p>재시도가 필요한 알림들을 비동기로 처리합니다.</p>
     */
    @Async("notificationExecutor")
    public CompletableFuture<Void> processRetryableNotificationsAsync() {
        try {
            log.info("비동기 재시도 가능한 알림 처리 시작");
            processRetryableNotifications();
            log.info("비동기 재시도 가능한 알림 처리 완료");
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("비동기 재시도 가능한 알림 처리 실패", e);
            return CompletableFuture.failedFuture(e);
        }
    }
}
