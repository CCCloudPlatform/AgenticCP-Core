package com.agenticcp.core.controller;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.service.aws.VmUseCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
 * AWS VM 인스턴스의 생성, 조회, 수정, 삭제, 생명주기 관리 등의 기능을 제공합니다.
 * 핵사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/vms")
@RequiredArgsConstructor
@Tag(name = "VM Management", description = "VM 인스턴스 관리 API")
public class VmController {

    private final VmUseCaseService vmUseCaseService;

    // ==================== 인스턴스 조회 ====================

    /**
     * VM 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건 (페이지, 필터 등)
     * @return CloudResource 페이지
     */
    @GetMapping("/instances")
    @Operation(summary = "VM 인스턴스 목록 조회", description = "조건에 맞는 VM 인스턴스 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<CloudResource>> listInstances(
            @Parameter(description = "페이지 번호") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "인스턴스 ID") @RequestParam(required = false) String instanceId,
            @Parameter(description = "인스턴스 이름") @RequestParam(required = false) String instanceName,
            @Parameter(description = "인스턴스 상태") @RequestParam(required = false) String state,
            @Parameter(description = "인스턴스 타입") @RequestParam(required = false) String instanceType,
            @Parameter(description = "가용 영역") @RequestParam(required = false) String availabilityZone) {
        
        // VmQuery 객체 생성
        VmQuery query = VmQuery.builder()
            .page(page)
            .size(size)
            .instanceId(instanceId)
            .instanceName(instanceName)
            .state(state)
            .instanceType(instanceType)
            .availabilityZone(availabilityZone)
            .build();
        
        log.info("[VmController] listInstances - query={}", query);
        
        try {
            Page<CloudResource> result = vmUseCaseService.listInstances(query);
            log.info("[VmController] listInstances - success count={}", result.getTotalElements());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[VmController] listInstances - failed", e);
            throw e;
        }
    }

    /**
     * 특정 VM 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return CloudResource 또는 404
     */
    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "VM 인스턴스 상세 조회", description = "특정 VM 인스턴스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CloudResource> getInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] getInstance - instanceId={}", instanceId);
        
        try {
            Optional<CloudResource> result = vmUseCaseService.getInstance(instanceId);
            
            if (result.isPresent()) {
                log.info("[VmController] getInstance - success instanceId={}", instanceId);
                return ResponseEntity.ok(result.get());
            } else {
                log.info("[VmController] getInstance - not found instanceId={}", instanceId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("[VmController] getInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 VM 인스턴스를 생성합니다.
     * 
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     */
    @PostMapping("/instances")
    @Operation(summary = "VM 인스턴스 생성", description = "새로운 VM 인스턴스를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> createInstance(
            @Parameter(description = "생성 요청 정보") @Valid @RequestBody VmCreateRequest request) {
        
        log.info("[VmController] createInstance - imageId={}, instanceType={}", 
                request.getImageId(), request.getInstanceType());
        
        try {
            String instanceId = vmUseCaseService.createInstance(request);
            log.info("[VmController] createInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok(instanceId);
            
        } catch (Exception e) {
            log.error("[VmController] createInstance - failed", e);
            throw e;
        }
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * VM 인스턴스를 시작합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/start")
    @Operation(summary = "VM 인스턴스 시작", description = "중지된 VM 인스턴스를 시작합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "시작 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> startInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] startInstance - instanceId={}", instanceId);
        
        try {
            vmUseCaseService.startInstance(instanceId);
            log.info("[VmController] startInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] startInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스를 중지합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/stop")
    @Operation(summary = "VM 인스턴스 중지", description = "실행 중인 VM 인스턴스를 중지합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "중지 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> stopInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] stopInstance - instanceId={}", instanceId);
        
        try {
            vmUseCaseService.stopInstance(instanceId);
            log.info("[VmController] stopInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] stopInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스를 재부팅합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/reboot")
    @Operation(summary = "VM 인스턴스 재부팅", description = "실행 중인 VM 인스턴스를 재부팅합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "재부팅 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> rebootInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] rebootInstance - instanceId={}", instanceId);
        
        try {
            vmUseCaseService.rebootInstance(instanceId);
            log.info("[VmController] rebootInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] rebootInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스를 종료합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/terminate")
    @Operation(summary = "VM 인스턴스 종료", description = "VM 인스턴스를 종료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "종료 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> terminateInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] terminateInstance - instanceId={}", instanceId);
        
        try {
            vmUseCaseService.terminateInstance(instanceId);
            log.info("[VmController] terminateInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] terminateInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스를 삭제합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param request 삭제 요청 정보
     * @return 성공 응답
     */
    @DeleteMapping("/instances/{instanceId}")
    @Operation(summary = "VM 인스턴스 삭제", description = "VM 인스턴스를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "삭제 요청 정보") @RequestBody(required = false) VmDeleteRequest request) {
        
        log.info("[VmController] deleteInstance - instanceId={}", instanceId);
        
        try {
            // 요청이 없으면 기본 삭제 요청 생성
            if (request == null) {
                request = VmDeleteRequest.basic(instanceId);
            }
            
            vmUseCaseService.deleteInstance(request);
            log.info("[VmController] deleteInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] deleteInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * VM 인스턴스 정보를 수정합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param request 수정 요청 정보
     * @return 성공 응답
     */
    @PutMapping("/instances/{instanceId}")
    @Operation(summary = "VM 인스턴스 수정", description = "VM 인스턴스의 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> updateInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "수정 요청 정보") @RequestBody VmUpdateRequest request) {
        
        log.info("[VmController] updateInstance - instanceId={}", instanceId);
        
        try {
            vmUseCaseService.updateInstance(request);
            log.info("[VmController] updateInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] updateInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 태그 관리 ====================

    /**
     * VM 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 추가", description = "VM 인스턴스에 태그를 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "태그 추가 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> addTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "추가할 태그") @RequestBody Map<String, String> tags) {
        
        log.info("[VmController] addTags - instanceId={}, tags={}", instanceId, tags);
        
        try {
            vmUseCaseService.addTags(instanceId, tags);
            log.info("[VmController] addTags - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] addTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     * @return 성공 응답
     */
    @DeleteMapping("/instances/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 제거", description = "VM 인스턴스에서 태그를 제거합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "태그 제거 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> removeTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "제거할 태그 키들") @RequestBody Map<String, String> tagKeys) {
        
        log.info("[VmController] removeTags - instanceId={}, tagKeys={}", instanceId, tagKeys.keySet());
        
        try {
            vmUseCaseService.removeTags(instanceId, tagKeys);
            log.info("[VmController] removeTags - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[VmController] removeTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    @GetMapping("/instances/{instanceId}/tags")
    @Operation(summary = "VM 인스턴스 태그 조회", description = "VM 인스턴스의 모든 태그를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> getTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] getTags - instanceId={}", instanceId);
        
        try {
            Map<String, String> tags = vmUseCaseService.getTags(instanceId);
            log.info("[VmController] getTags - success instanceId={}, tagCount={}", instanceId, tags.size());
            return ResponseEntity.ok(tags);
            
        } catch (Exception e) {
            log.error("[VmController] getTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 상태 확인 ====================

    /**
     * VM 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    @GetMapping("/instances/{instanceId}/status")
    @Operation(summary = "VM 인스턴스 상태 확인", description = "VM 인스턴스의 현재 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> getInstanceStatus(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[VmController] getInstanceStatus - instanceId={}", instanceId);
        
        try {
            String status = vmUseCaseService.getInstanceStatus(instanceId);
            log.info("[VmController] getInstanceStatus - success instanceId={}, status={}", instanceId, status);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("[VmController] getInstanceStatus - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    @PostMapping("/instances/{instanceId}/wait")
    @Operation(summary = "VM 인스턴스 상태 대기", description = "VM 인스턴스가 특정 상태에 도달할 때까지 대기합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "대기 완료"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Boolean> waitForInstanceStatus(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "목표 상태") @RequestParam String targetStatus,
            @Parameter(description = "타임아웃 (초)") @RequestParam(defaultValue = "300") int timeoutSeconds) {
        
        log.info("[VmController] waitForInstanceStatus - instanceId={}, targetStatus={}, timeout={}s", 
                instanceId, targetStatus, timeoutSeconds);
        
        try {
            boolean success = vmUseCaseService.waitForInstanceStatus(instanceId, targetStatus, timeoutSeconds);
            log.info("[VmController] waitForInstanceStatus - success={} instanceId={}", success, instanceId);
            return ResponseEntity.ok(success);
            
        } catch (Exception e) {
            log.error("[VmController] waitForInstanceStatus - failed instanceId={}", instanceId, e);
            throw e;
        }
    }
}
