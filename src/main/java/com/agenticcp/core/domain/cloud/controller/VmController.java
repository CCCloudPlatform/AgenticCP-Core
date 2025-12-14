package com.agenticcp.core.domain.cloud.controller;

import com.agenticcp.core.common.dto.exception.ApiResponse;
import com.agenticcp.core.domain.cloud.entity.CloudProvider;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.exception.CloudErrorCode;
import com.agenticcp.core.domain.cloud.dto.VmCreateRequest;
import com.agenticcp.core.domain.cloud.dto.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.dto.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.Optional;

/**
 * VM 인스턴스 관리를 위한 REST API 컨트롤러
 * 
 * 멀티 클라우드(AWS, GCP, Azure) VM 인스턴스의 생성, 조회, 수정, 삭제, 생명주기 관리 등의 기능을 제공합니다.
 * 헥사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cloud/providers/{provider}/accounts/{accountScope}/vms/instances")
@RequiredArgsConstructor
@Tag(name = "VM Management", description = "VM 인스턴스 관리 API (멀티 클라우드 지원)")
public class VmController {

    private final VmUseCaseService vmUseCaseService;

    // ==================== 인스턴스 조회 ====================

    /**
     * VM 인스턴스 목록을 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입 (AWS, GCP, AZURE)
     * @param accountScope 계정 스코프
     * @return CloudResource 페이지
     */
    @GetMapping
    @Operation(summary = "VM 인스턴스 목록 조회", description = "조건에 맞는 VM 인스턴스 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Page<CloudResource>>> listInstances(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "인스턴스 ID") @RequestParam(required = false) String instanceId,
            @Parameter(description = "인스턴스 이름") @RequestParam(required = false) String instanceName,
            @Parameter(description = "인스턴스 상태") @RequestParam(required = false) String state,
            @Parameter(description = "인스턴스 타입") @RequestParam(required = false) String instanceType,
            @Parameter(description = "가용 영역") @RequestParam(required = false) String availabilityZone) {
        
        log.info("[VmController] listInstances - provider={}, accountScope={}, page={}, size={}", 
                provider, accountScope, page, size);
        
        VmQuery query = VmQuery.builder()
            .page(page)
            .size(size)
            .instanceId(instanceId)
            .instanceName(instanceName)
            .state(state)
            .instanceType(instanceType)
            .availabilityZone(availabilityZone)
            .build();
        
        Page<CloudResource> result = vmUseCaseService.listInstances(provider, accountScope, query);
        log.info("[VmController] listInstances - success provider={}, count={}", provider, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(result, "VM 인스턴스 목록 조회에 성공했습니다."));
    }

    /**
     * 특정 VM 인스턴스를 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return CloudResource 또는 404
     */
    @GetMapping("/{instanceId}")
    @Operation(summary = "VM 인스턴스 상세 조회", description = "특정 VM 인스턴스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> getInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[VmController] getInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        Optional<CloudResource> result = vmUseCaseService.getInstance(provider, accountScope, instanceId);
        
        if (result.isPresent()) {
            log.info("[VmController] getInstance - success provider={}, instanceId={}", provider, instanceId);
            return ResponseEntity.ok(ApiResponse.success(result.get(), "VM 인스턴스 조회에 성공했습니다."));
        } else {
            log.info("[VmController] getInstance - not found provider={}, instanceId={}", provider, instanceId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(CloudErrorCode.CLOUD_RESOURCE_NOT_FOUND));
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 VM 인스턴스를 생성합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param request 생성 요청 정보
     * @return 생성된 CloudResource 엔티티
     */
    @PostMapping
    @Operation(summary = "VM 인스턴스 생성", description = "새로운 VM 인스턴스를 생성합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<CloudResource>> createInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "생성 요청 정보")
            @Valid @RequestBody VmCreateRequest request) {
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        
        log.info("[VmController] createInstance - provider={}, accountScope={}, image={}, instanceSize={}", 
                provider, accountScope, request.getImage(), request.getInstanceSize());
        
        CloudResource cloudResource = vmUseCaseService.createInstance(request);
        log.info("[VmController] createInstance - success provider={}, instanceId={}, resourceId={}", 
                provider, cloudResource.getResourceId(), cloudResource.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cloudResource, "VM 인스턴스 생성에 성공했습니다."));
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * VM 인스턴스를 시작합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/start")
    @Operation(summary = "VM 인스턴스 시작", description = "중지된 VM 인스턴스를 시작합니다.")
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
        
        log.info("[VmController] startInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        vmUseCaseService.startInstance(provider, accountScope, instanceId);
        log.info("[VmController] startInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 시작에 성공했습니다."));
    }

    /**
     * VM 인스턴스를 중지합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/stop")
    @Operation(summary = "VM 인스턴스 중지", description = "실행 중인 VM 인스턴스를 중지합니다.")
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
        
        log.info("[VmController] stopInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        vmUseCaseService.stopInstance(provider, accountScope, instanceId);
        log.info("[VmController] stopInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 중지에 성공했습니다."));
    }

    /**
     * VM 인스턴스를 재부팅합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/reboot")
    @Operation(summary = "VM 인스턴스 재부팅", description = "실행 중인 VM 인스턴스를 재부팅합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재부팅 성공"),
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
        
        log.info("[VmController] rebootInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        vmUseCaseService.rebootInstance(provider, accountScope, instanceId);
        log.info("[VmController] rebootInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 재부팅에 성공했습니다."));
    }

    /**
     * VM 인스턴스를 종료합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/terminate")
    @Operation(summary = "VM 인스턴스 종료", description = "VM 인스턴스를 종료합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> terminateInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[VmController] terminateInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        vmUseCaseService.terminateInstance(provider, accountScope, instanceId);
        log.info("[VmController] terminateInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 종료에 성공했습니다."));
    }

    /**
     * VM 인스턴스를 삭제합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param request 삭제 요청 정보
     * @return 성공 응답
     */
    @DeleteMapping("/{instanceId}")
    @Operation(summary = "VM 인스턴스 삭제", description = "VM 인스턴스를 삭제합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> deleteInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "삭제 요청 정보")
            @RequestBody(required = false) VmDeleteRequest request) {
        
        log.info("[VmController] deleteInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        // 요청이 없으면 기본 삭제 요청 생성
        if (request == null) {
            request = VmDeleteRequest.basic(instanceId);
        }
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setInstanceId(instanceId);
        
        vmUseCaseService.deleteInstance(request);
        log.info("[VmController] deleteInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * VM 인스턴스 정보를 수정합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param request 수정 요청 정보
     * @return 성공 응답
     */
    @PutMapping("/{instanceId}")
    @Operation(summary = "VM 인스턴스 수정", description = "VM 인스턴스의 정보를 수정합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> updateInstance(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "수정 요청 정보")
            @RequestBody VmUpdateRequest request) {
        
        log.info("[VmController] updateInstance - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        // PathVariable 값을 Request 객체에 주입
        request.setProviderType(provider);
        request.setAccountScope(accountScope);
        request.setInstanceId(instanceId);
        
        vmUseCaseService.updateInstance(request);
        log.info("[VmController] updateInstance - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 수정에 성공했습니다."));
    }

    // ==================== 태그 관리 ====================

    /**
     * VM 인스턴스에 태그를 추가합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     * @return 성공 응답
     */
    @PostMapping("/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 추가", description = "VM 인스턴스에 태그를 추가합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "태그 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> addTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "추가할 태그")
            @RequestBody Map<String, String> tags) {
        
        log.info("[VmController] addTags - provider={}, accountScope={}, instanceId={}, tags={}", 
                provider, accountScope, instanceId, tags);
        
        vmUseCaseService.addTags(provider, accountScope, instanceId, tags);
        log.info("[VmController] addTags - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 태그 추가에 성공했습니다."));
    }

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     * @return 성공 응답
     */
    @DeleteMapping("/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 제거", description = "VM 인스턴스에서 태그를 제거합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "태그 제거 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Void>> removeTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "제거할 태그 키들")
            @RequestBody Map<String, String> tagKeys) {
        
        log.info("[VmController] removeTags - provider={}, accountScope={}, instanceId={}, tagKeys={}", 
                provider, accountScope, instanceId, tagKeys.keySet());
        
        vmUseCaseService.removeTags(provider, accountScope, instanceId, tagKeys);
        log.info("[VmController] removeTags - success provider={}, instanceId={}", provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(null, "VM 인스턴스 태그 제거에 성공했습니다."));
    }

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    @GetMapping("/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 조회", description = "VM 인스턴스의 모든 태그를 조회합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Map<String, String>>> getTags(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId) {
        
        log.info("[VmController] getTags - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        Map<String, String> tags = vmUseCaseService.getTags(provider, accountScope, instanceId);
        log.info("[VmController] getTags - success provider={}, instanceId={}, tagCount={}", 
                provider, instanceId, tags.size());
        return ResponseEntity.ok(ApiResponse.success(tags, "VM 인스턴스 태그 조회에 성공했습니다."));
    }

    // ==================== 상태 확인 ====================

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    @GetMapping("/{instanceId}/status")
    @Operation(summary = "VM 인스턴스 상태 확인", description = "VM 인스턴스의 현재 상태를 확인합니다.")
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
        
        log.info("[VmController] getInstanceStatus - provider={}, accountScope={}, instanceId={}", 
                provider, accountScope, instanceId);
        
        String status = vmUseCaseService.getInstanceStatus(provider, accountScope, instanceId);
        log.info("[VmController] getInstanceStatus - success provider={}, instanceId={}, status={}", 
                provider, instanceId, status);
        return ResponseEntity.ok(ApiResponse.success(status, "VM 인스턴스 상태 조회에 성공했습니다."));
    }

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param provider 클라우드 프로바이더 타입
     * @param accountScope 계정 스코프
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    @PostMapping("/{instanceId}/wait")
    @Operation(summary = "VM 인스턴스 상태 대기", description = "VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대기 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<ApiResponse<Boolean>> waitForInstanceStatus(
            @Parameter(description = "클라우드 프로바이더 타입", required = true, example = "AWS")
            @PathVariable CloudProvider.ProviderType provider,
            @Parameter(description = "계정 스코프", required = true, example = "123456789012")
            @PathVariable String accountScope,
            @Parameter(description = "인스턴스 ID", required = true)
            @PathVariable String instanceId,
            @Parameter(description = "목표 상태")
            @RequestParam String targetStatus,
            @Parameter(description = "타임아웃 (초)")
            @RequestParam(defaultValue = "300") int timeoutSeconds) {
        
        log.info("[VmController] waitForInstanceStatus - provider={}, accountScope={}, instanceId={}, targetStatus={}, timeout={}s", 
                provider, accountScope, instanceId, targetStatus, timeoutSeconds);
        
        boolean success = vmUseCaseService.waitForInstanceStatus(provider, accountScope, instanceId, targetStatus, timeoutSeconds);
        log.info("[VmController] waitForInstanceStatus - success={} provider={}, instanceId={}", 
                success, provider, instanceId);
        return ResponseEntity.ok(ApiResponse.success(success, "VM 인스턴스 상태 대기 결과입니다."));
    }
}
