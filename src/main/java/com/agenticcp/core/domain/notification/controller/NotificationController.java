package com.agenticcp.core.domain.notification.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.entity.NotificationTemplate;
import com.agenticcp.core.domain.notification.service.NotificationService;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 알림 시스템 API 컨트롤러
 * 
 * <p>실시간 알림 시스템의 REST API를 제공합니다.</p>
 * <p>Issue #81: 실시간 알림 시스템 구현</p>
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification", description = "알림 시스템 API")
public class NotificationController {

    private final NotificationService notificationService;
    private final MonitoringNotificationService monitoringNotificationService;

    /**
     * 알림 발송
     * 
     * @param request 알림 요청
     * @return 알림 응답
     */
    @PostMapping("/send")
    @Operation(summary = "알림 발송", description = "실시간 알림을 발송합니다.")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("알림 발송 요청: {}", request.getNotificationId());
        
        // 자동으로 현재 테넌트 ID 설정
        Long currentTenantId = getCurrentTenantId();
        request.setTenantId(currentTenantId);
        
        NotificationResponse response = notificationService.sendNotification(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 비동기 알림 발송
     * 
     * @param request 알림 요청
     * @return 알림 응답 (비동기 처리)
     */
    @PostMapping("/send-async")
    @Operation(summary = "비동기 알림 발송", description = "알림을 비동기로 발송하여 응답 시간을 단축합니다.")
    public ResponseEntity<Map<String, Object>> sendNotificationAsync(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("비동기 알림 발송 요청: {}", request.getNotificationId());
        
        // 자동으로 현재 테넌트 ID 설정
        Long currentTenantId = getCurrentTenantId();
        request.setTenantId(currentTenantId);
        
        // 비동기 알림 발송 시작
        notificationService.sendNotificationAsync(request);
        
        // 즉시 응답 반환
        Map<String, Object> response = Map.of(
            "notificationId", request.getNotificationId(),
            "status", "PROCESSING",
            "message", "알림 발송이 비동기로 처리되었습니다.",
            "timestamp", java.time.LocalDateTime.now()
        );
        
        log.info("비동기 알림 발송 시작 완료: {}", request.getNotificationId());
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 알림 규칙 조회
     * 
     * @param tenantId 테넌트 ID
     * @param pageable 페이징 정보
     * @return 알림 규칙 목록
     */
    @GetMapping("/rules")
    @Operation(summary = "알림 규칙 조회", description = "테넌트별 알림 규칙을 조회합니다.")
    public ResponseEntity<Page<NotificationTemplate>> getNotificationRules(
            Pageable pageable) {
        
        Long tenantId = getCurrentTenantId();
        log.info("알림 규칙 조회: tenantId={}", tenantId);
        
        // TODO: 알림 규칙 조회 로직 구현
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * 알림 규칙 생성
     * 
     * @param template 알림 템플릿
     * @return 생성된 알림 템플릿
     */
    @PostMapping("/rules")
    @Operation(summary = "알림 규칙 생성", description = "새로운 알림 규칙을 생성합니다.")
    public ResponseEntity<NotificationTemplate> createNotificationRule(
            @Valid @RequestBody NotificationTemplate template) {
        
        log.info("알림 규칙 생성: {}", template.getTemplateName());
        
        // TODO: 알림 규칙 생성 로직 구현
        return ResponseEntity.ok(template);
    }

    /**
     * 알림 규칙 수정
     * 
     * @param ruleId 규칙 ID
     * @param template 수정할 템플릿
     * @return 수정된 알림 템플릿
     */
    @PutMapping("/rules/{ruleId}")
    @Operation(summary = "알림 규칙 수정", description = "기존 알림 규칙을 수정합니다.")
    public ResponseEntity<NotificationTemplate> updateNotificationRule(
            @Parameter(description = "규칙 ID") @PathVariable Long ruleId,
            @Valid @RequestBody NotificationTemplate template) {
        
        log.info("알림 규칙 수정: ruleId={}", ruleId);
        
        // TODO: 알림 규칙 수정 로직 구현
        return ResponseEntity.ok(template);
    }

    /**
     * 알림 히스토리 조회
     * 
     * @param tenantId 테넌트 ID
     * @param userId 사용자 ID (선택사항)
     * @param pageable 페이징 정보
     * @return 알림 히스토리
     */
    @GetMapping("/history")
    @Operation(summary = "알림 히스토리 조회", description = "알림 발송 히스토리를 조회합니다.")
    public ResponseEntity<Page<Notification>> getNotificationHistory(
            @Parameter(description = "사용자 ID") @RequestParam(required = false) Long userId,
            Pageable pageable) {
        
        Long tenantId = getCurrentTenantId();
        log.info("알림 히스토리 조회: tenantId={}, userId={}", tenantId, userId);
        
        // TODO: 알림 히스토리 조회 로직 구현
        return ResponseEntity.ok(Page.empty());
    }

    /**
     * 알림 테스트 발송
     * 
     * @param request 테스트 알림 요청
     * @return 테스트 알림 응답
     */
    @PostMapping("/test")
    @Operation(summary = "알림 테스트 발송", description = "알림 시스템을 테스트합니다.")
    public ResponseEntity<NotificationResponse> sendTestNotification(
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("알림 테스트 발송: {}", request.getNotificationId());
        
        // 테스트용 알림 요청 생성
        NotificationRequest testRequest = NotificationRequest.builder()
                .notificationId("test_" + System.currentTimeMillis())
                .tenantId(request.getTenantId())
                .userId(request.getUserId())
                .title("테스트 알림")
                .content("이것은 알림 시스템 테스트입니다.")
                .type(request.getType())
                .priority(request.getPriority())
                .recipient(request.getRecipient())
                .channelId(request.getChannelId())
                .build();
        
        NotificationResponse response = notificationService.sendNotification(testRequest);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 모니터링 알림 발송 (내부 API)
     * 
     * @param alertData 알림 데이터
     * @return 알림 응답
     */
    @PostMapping("/monitoring/send")
    @Operation(summary = "모니터링 알림 발송", description = "모니터링 시스템에서 알림을 발송합니다.")
    public ResponseEntity<NotificationResponse> sendMonitoringAlert(
            @RequestBody Map<String, Object> alertData) {
        
        log.info("모니터링 알림 발송: {}", alertData);
        
        // TODO: 모니터링 알림 발송 로직 구현
        NotificationResponse response = NotificationResponse.builder()
                .notificationId("monitoring_" + System.currentTimeMillis())
                .status(com.agenticcp.core.domain.notification.enums.NotificationStatus.SENT)
                .message("모니터링 알림이 발송되었습니다.")
                .success(true)
                .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * 알림 채널 목록 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 알림 채널 목록
     */
    @GetMapping("/channels")
    @Operation(summary = "알림 채널 조회", description = "사용 가능한 알림 채널을 조회합니다.")
    public ResponseEntity<List<NotificationChannelEntity>> getNotificationChannels() {
        
        Long tenantId = getCurrentTenantId();
        log.info("알림 채널 조회: tenantId={}", tenantId);
        
        List<NotificationChannelEntity> channels = notificationService.getActiveChannels(tenantId);
        return ResponseEntity.ok(channels);
    }

    /**
     * 알림 채널 생성
     * 
     * @param channel 알림 채널 정보
     * @return 생성된 알림 채널
     */
    @PostMapping("/channels")
    @Operation(summary = "알림 채널 생성", description = "새로운 알림 채널을 생성합니다.")
    public ResponseEntity<NotificationChannelEntity> createNotificationChannel(
            @Valid @RequestBody NotificationChannelEntity channel) {
        
        log.info("알림 채널 생성: channelName={}, tenantId={}", channel.getChannelName(), channel.getTenantId());
        
        NotificationChannelEntity createdChannel = notificationService.createChannel(channel);
        return ResponseEntity.ok(createdChannel);
    }

    /**
     * 알림 채널 수정
     * 
     * @param channelId 채널 ID
     * @param channel 수정할 채널 정보
     * @return 수정된 알림 채널
     */
    @PutMapping("/channels/{channelId}")
    @Operation(summary = "알림 채널 수정", description = "기존 알림 채널을 수정합니다.")
    public ResponseEntity<NotificationChannelEntity> updateNotificationChannel(
            @PathVariable Long channelId,
            @Valid @RequestBody NotificationChannelEntity channel) {
        
        log.info("알림 채널 수정: channelId={}", channelId);
        
        NotificationChannelEntity updatedChannel = notificationService.updateChannel(channelId, channel);
        return ResponseEntity.ok(updatedChannel);
    }

    /**
     * 알림 채널 삭제
     * 
     * @param channelId 채널 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/channels/{channelId}")
    @Operation(summary = "알림 채널 삭제", description = "알림 채널을 삭제합니다.")
    public ResponseEntity<Void> deleteNotificationChannel(@PathVariable Long channelId) {
        
        log.info("알림 채널 삭제: channelId={}", channelId);
        
        notificationService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 알림 채널 상세 조회
     * 
     * @param channelId 채널 ID
     * @return 알림 채널 상세 정보
     */
    @GetMapping("/channels/{channelId}")
    @Operation(summary = "알림 채널 상세 조회", description = "특정 알림 채널의 상세 정보를 조회합니다.")
    public ResponseEntity<NotificationChannelEntity> getNotificationChannel(@PathVariable Long channelId) {
        
        log.info("알림 채널 상세 조회: channelId={}", channelId);
        
        NotificationChannelEntity channel = notificationService.getChannel(channelId);
        return ResponseEntity.ok(channel);
    }

    /**
     * 알림 통계 조회
     * 
     * @param tenantId 테넌트 ID
     * @return 알림 통계
     */
    @GetMapping("/stats")
    @Operation(summary = "알림 통계 조회", description = "알림 발송 통계를 조회합니다.")
    public ResponseEntity<Map<String, Object>> getNotificationStats() {
        
        Long tenantId = getCurrentTenantId();
        log.info("알림 통계 조회: tenantId={}", tenantId);
        
        // TODO: 알림 통계 조회 로직 구현
        Map<String, Object> stats = Map.of(
            "totalSent", 0,
            "successRate", 0.0,
            "failureRate", 0.0
        );
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 현재 테넌트 ID 조회
     * 
     * @return 현재 테넌트 ID
     */
    private Long getCurrentTenantId() {
        try {
            String tenantKey = TenantContextHolder.getCurrentTenantKeyOrThrow();
            return Long.parseLong(tenantKey);
        } catch (Exception e) {
            log.warn("테넌트 컨텍스트를 찾을 수 없습니다. 기본값 사용: {}", e.getMessage());
            // TODO: 실제 운영에서는 예외를 발생시켜야 함
            return 1L; // 임시 기본값
        }
    }
}
