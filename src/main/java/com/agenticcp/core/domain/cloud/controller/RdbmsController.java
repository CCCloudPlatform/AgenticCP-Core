package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.audit.AuditRequired;
import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import com.agenticcp.core.domain.cloud.dto.RdbmsCreateRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsDeleteRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsQueryRequest;
import com.agenticcp.core.domain.cloud.dto.RdbmsResponse;
import com.agenticcp.core.domain.cloud.dto.RdbmsUpdateRequest;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.service.rdbms.RdbmsUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RDBMS 인스턴스 관리를 위한 REST API 컨트롤러
 * 
 * 멀티 클라우드(AWS, GCP, Azure) RDBMS 인스턴스의 생성, 조회, 수정, 삭제, 생명주기 관리 등의 기능을 제공합니다.
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/rdbms/instances")
@RequiredArgsConstructor
@Tag(name = "RDBMS Management", description = "RDBMS 인스턴스 관리 API (멀티 클라우드 지원)")
public class RdbmsController {

    private final RdbmsUseCaseService rdbmsUseCaseService;

    // ==================== 인스턴스 조회 ====================

    /**
     * RDBMS 인스턴스 목록을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @param request 조회 요청 (페이징, 필터링 포함)
     * @return RdbmsResponse 페이지
     */
    @GetMapping
    @AuditRequired(
        action = "LIST_RDBMS_INSTANCES",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 목록 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 목록 조회", description = "조건에 맞는 RDBMS 인스턴스 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Page<RdbmsResponse>>> listRdbmsInstances(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Valid RdbmsQueryRequest request) {
        
        log.info("[RdbmsController] listRdbmsInstances - provider={}, accountScope={}, page={}, size={}", 
                provider, accountScope, request.getPage(), request.getSize());
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        Page<CloudResource> result = rdbmsUseCaseService.listRdbmsInstances(provider, accountScope, request);
        
        // CloudResource를 RdbmsResponse로 변환
        List<RdbmsResponse> responses = result.getContent().stream()
                .map(RdbmsResponse::from)
                .collect(Collectors.toList());
        
        Page<RdbmsResponse> responsePage = new PageImpl<>(
                responses,
                result.getPageable(),
                result.getTotalElements()
        );
        
        log.info("[RdbmsController] listRdbmsInstances - success provider={}, count={}", 
                provider, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(responsePage, "RDBMS 인스턴스 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 RDBMS 인스턴스를 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return RdbmsResponse 또는 404
     */
    @GetMapping("/{instanceId}")
    @AuditRequired(
        action = "GET_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 상세 조회",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 상세 조회", description = "특정 RDBMS 인스턴스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<RdbmsResponse>> getRdbmsInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[RdbmsController] getRdbmsInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        Optional<CloudResource> result = rdbmsUseCaseService.getRdbmsInstance(provider, accountScope, instanceId);
        
        if (result.isPresent()) {
            RdbmsResponse response = RdbmsResponse.from(result.get());
            log.info("[RdbmsController] getRdbmsInstance - success provider={}, instanceId={}", 
                    provider, instanceId);
            return ResponseEntity.ok(ApiResponse.success(response, "RDBMS 인스턴스 조회에 성공했습니다."));
        } else {
            log.info("[RdbmsController] getRdbmsInstance - not found provider={}, instanceId={}", 
                    provider, instanceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 RDBMS 인스턴스를 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 생성 요청 정보
     * @return 생성된 RdbmsResponse
     */
    @PostMapping
    @AuditRequired(
        action = "CREATE_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 생성",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(summary = "RDBMS 인스턴스 생성", description = "새로운 RDBMS 인스턴스를 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<RdbmsResponse>> createRdbms(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "생성 요청 정보")
            @Valid @RequestBody RdbmsCreateRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[RdbmsController] createRdbms - provider={}, accountScope={}, instanceName={}, engine={}", 
                provider, accountScope, request.getInstanceName(), request.getEngine());
        
        CloudResource resource = rdbmsUseCaseService.createRdbms(request);
        RdbmsResponse response = RdbmsResponse.from(resource);
        
        log.info("[RdbmsController] createRdbms - success provider={}, instanceId={}, resourceId={}", 
                provider, resource.getResourceId(), resource.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "RDBMS 인스턴스 생성에 성공했습니다."));
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * RDBMS 인스턴스 정보를 수정합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param request 수정 요청 정보
     * @return 수정된 RdbmsResponse
     */
    @PutMapping("/{instanceId}")
    @AuditRequired(
        action = "UPDATE_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 수정",
        severity = AuditSeverity.HIGH,
        includeRequestData = true,
        includeResponseData = true
    )
    @Operation(summary = "RDBMS 인스턴스 수정", description = "RDBMS 인스턴스의 정보를 수정합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<RdbmsResponse>> updateRdbms(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "수정 요청 정보")
            @Valid @RequestBody RdbmsUpdateRequest request) {
        
        log.info("[RdbmsController] updateRdbms - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setInstanceId(instanceId);
        
        CloudResource resource = rdbmsUseCaseService.updateRdbms(request);
        RdbmsResponse response = RdbmsResponse.from(resource);
        
        log.info("[RdbmsController] updateRdbms - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(response, "RDBMS 인스턴스 수정에 성공했습니다."));
    }

    // ==================== 인스턴스 삭제 ====================

    /**
     * RDBMS 인스턴스를 삭제합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param request 삭제 요청 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{instanceId}")
    @AuditRequired(
        action = "DELETE_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 삭제",
        severity = AuditSeverity.CRITICAL,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 삭제", description = "RDBMS 인스턴스를 삭제합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteRdbms(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "삭제 요청 정보")
            @RequestBody(required = false) RdbmsDeleteRequest request) {
        
        log.info("[RdbmsController] deleteRdbms - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        // 요청이 없으면 기본 삭제 요청 생성
        if (request == null) {
            request = RdbmsDeleteRequest.basic(instanceId);
        }
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setInstanceId(instanceId);
        
        rdbmsUseCaseService.deleteRdbms(request);
        log.info("[RdbmsController] deleteRdbms - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * RDBMS 인스턴스를 시작합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/start")
    @AuditRequired(
        action = "START_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 시작",
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 시작", description = "중지된 RDBMS 인스턴스를 시작합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시작 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> startInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[RdbmsController] startInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        rdbmsUseCaseService.startInstance(provider, accountScope, instanceId);
        log.info("[RdbmsController] startInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "RDBMS 인스턴스 시작에 성공했습니다."));
    }

    /**
     * RDBMS 인스턴스를 중지합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/stop")
    @AuditRequired(
        action = "STOP_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 중지",
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 중지", description = "실행 중인 RDBMS 인스턴스를 중지합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중지 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> stopInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[RdbmsController] stopInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        rdbmsUseCaseService.stopInstance(provider, accountScope, instanceId);
        log.info("[RdbmsController] stopInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "RDBMS 인스턴스 중지에 성공했습니다."));
    }

    /**
     * RDBMS 인스턴스를 재시작합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/reboot")
    @AuditRequired(
        action = "REBOOT_RDBMS_INSTANCE",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 재시작",
        severity = AuditSeverity.MEDIUM,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 재시작", description = "실행 중인 RDBMS 인스턴스를 재시작합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재시작 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> rebootInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[RdbmsController] rebootInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        rdbmsUseCaseService.rebootInstance(provider, accountScope, instanceId);
        log.info("[RdbmsController] rebootInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "RDBMS 인스턴스 재시작에 성공했습니다."));
    }

    // ==================== 상태 확인 ====================

    /**
     * RDBMS 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    @GetMapping("/{instanceId}/status")
    @AuditRequired(
        action = "GET_RDBMS_INSTANCE_STATUS",
        resourceType = AuditResourceType.CLOUD_PROVIDER,
        description = "RDBMS 인스턴스 상태 확인",
        severity = AuditSeverity.LOW,
        includeRequestData = true,
        includeResponseData = false
    )
    @Operation(summary = "RDBMS 인스턴스 상태 확인", description = "RDBMS 인스턴스의 현재 상태를 확인합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<String>> getInstanceStatus(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[RdbmsController] getInstanceStatus - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        String status = rdbmsUseCaseService.getInstanceStatus(provider, accountScope, instanceId);
        log.info("[RdbmsController] getInstanceStatus - success provider={}, instanceId={}, status={}", 
                provider, instanceId, status);
        return ResponseEntity.ok(ApiResponse.success(status, "RDBMS 인스턴스 상태 조회에 성공했습니다."));
    }
}
