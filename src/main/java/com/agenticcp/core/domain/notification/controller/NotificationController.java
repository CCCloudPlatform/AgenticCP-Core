package com.agenticcp.core.domain.notification.controller;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.notification.dto.NotificationRequest;
import com.agenticcp.core.domain.notification.dto.NotificationResponse;
import com.agenticcp.core.domain.notification.entity.Notification;
import com.agenticcp.core.domain.notification.entity.NotificationChannelEntity;
import com.agenticcp.core.domain.notification.service.NotificationService;
import com.agenticcp.core.domain.notification.service.MonitoringNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-11-13
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
    @Operation(
        summary = "알림 발송", 
        description = "실시간 알림을 발송합니다. 지정된 채널(이메일 등)을 통해 즉시 알림을 전송합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "알림 발송 성공",
                content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<NotificationResponse>> sendNotification(
            @Parameter(description = "알림 발송 요청 정보")
            @Valid @RequestBody NotificationRequest request) {
        
        log.info("[NotificationController] sendNotification - 알림 발송 요청: {}", request.getNotificationId());
        
        // 자동으로 현재 테넌트 ID 설정
        String currentTenantId = getCurrentTenantId();
        request.setTenantId(currentTenantId);
        
        NotificationResponse response = notificationService.sendNotification(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "알림이 성공적으로 발송되었습니다."));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(null, response.getMessage()));
        }
    }

    /**
     * 알림 히스토리 조회
     * 
     * @param userId 사용자 ID (선택사항)
     * @param pageable 페이징 정보
     * @return 알림 히스토리
     */
    @GetMapping("/history")
    @Operation(
        summary = "알림 히스토리 조회", 
        description = "발송된 알림의 이력을 조회합니다. 사용자 ID를 지정하면 해당 사용자의 알림만 조회합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = Page.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<Page<Notification>>> getNotificationHistory(
            @Parameter(description = "사용자 ID (선택사항)", example = "1") 
            @RequestParam(required = false) Long userId,
            @Parameter(description = "페이징 정보 (page, size, sort)", example = "page=0&size=20&sort=createdAt,desc")
            Pageable pageable) {
        
        String tenantId = getCurrentTenantId();
        log.info("[NotificationController] getNotificationHistory - 알림 히스토리 조회: tenantId={}, userId={}", tenantId, userId);
        
        Page<Notification> history;
        if (userId != null) {
            history = notificationService.getNotificationHistoryByUser(tenantId, userId, pageable);
        } else {
            history = notificationService.getNotificationHistory(tenantId, pageable);
        }
        
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    /**
     * 알림 채널 목록 조회
     * 
     * @return 알림 채널 목록
     */
    @GetMapping("/channels")
    @Operation(
        summary = "알림 채널 조회", 
        description = "테넌트에 설정된 알림 채널 목록을 조회합니다. 이메일, 슬랙 등 다양한 채널을 지원합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                content = @Content(schema = @Schema(implementation = NotificationChannelEntity.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<List<NotificationChannelEntity>>> getNotificationChannels() {
        
        String tenantId = getCurrentTenantId();
        log.info("[NotificationController] getNotificationChannels - 알림 채널 조회: tenantId={}", tenantId);
        
        List<NotificationChannelEntity> channels = notificationService.getActiveChannels(tenantId);
        return ResponseEntity.ok(ApiResponse.success(channels, "알림 채널 목록을 성공적으로 조회했습니다."));
    }

    /**
     * 알림 채널 생성
     * 
     * @param channel 알림 채널 정보
     * @return 생성된 알림 채널
     */
    @PostMapping("/channels")
    @Operation(summary = "알림 채널 생성", description = "새로운 알림 채널을 생성합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "채널 생성 성공",
                content = @Content(schema = @Schema(implementation = NotificationChannelEntity.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "채널 이름 중복"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<NotificationChannelEntity>> createNotificationChannel(
            @Valid @RequestBody NotificationChannelEntity channel) {
        
        log.info("[NotificationController] createNotificationChannel - 알림 채널 생성: channelName={}, tenantId={}", 
                channel.getChannelName(), channel.getTenantId());
        
        NotificationChannelEntity createdChannel = notificationService.createChannel(channel);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdChannel, "알림 채널이 성공적으로 생성되었습니다."));
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
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채널 수정 성공",
                content = @Content(schema = @Schema(implementation = NotificationChannelEntity.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채널을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<ApiResponse<NotificationChannelEntity>> updateNotificationChannel(
            @Parameter(description = "채널 ID", required = true, example = "1")
            @PathVariable Long channelId,
            @Valid @RequestBody NotificationChannelEntity channel) {
        
        log.info("[NotificationController] updateNotificationChannel - 알림 채널 수정: channelId={}", channelId);
        
        NotificationChannelEntity updatedChannel = notificationService.updateChannel(channelId, channel);
        return ResponseEntity.ok(ApiResponse.success(updatedChannel, "알림 채널이 성공적으로 수정되었습니다."));
    }

    /**
     * 알림 채널 삭제
     * 
     * @param channelId 채널 ID
     */
    @DeleteMapping("/channels/{channelId}")
    @Operation(summary = "알림 채널 삭제", description = "알림 채널을 삭제합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "채널 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채널을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> deleteNotificationChannel(
            @Parameter(description = "채널 ID", required = true, example = "1")
            @PathVariable Long channelId) {
        
        log.info("[NotificationController] deleteNotificationChannel - 알림 채널 삭제: channelId={}", channelId);
        
        notificationService.deleteChannel(channelId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 현재 테넌트 ID 조회
     * 
     * @return 현재 테넌트 ID (tenantKey)
     * @throws RuntimeException 테넌트 컨텍스트를 찾을 수 없는 경우
     */
    private String getCurrentTenantId() {
        return TenantContextHolder.getCurrentTenantKeyOrThrow();
    }
}
