package com.agenticcp.core.controller;

import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.Ec2CreateRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2DeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.Ec2Query;
import com.agenticcp.core.domain.cloud.port.model.Ec2UpdateRequest;
import com.agenticcp.core.domain.cloud.service.aws.Ec2UseCaseService;
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

import java.util.Map;
import java.util.Optional;

/**
 * EC2 인스턴스 관리를 위한 REST API 컨트롤러
 * 
 * AWS EC2 인스턴스의 생성, 조회, 수정, 삭제, 생명주기 관리 등의 기능을 제공합니다.
 * 핵사고날 아키텍처의 인터페이스 계층에 해당하며, 외부 클라이언트와의 통신을 담당합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ec2")
@RequiredArgsConstructor
@Tag(name = "EC2 Management", description = "EC2 인스턴스 관리 API")
public class Ec2Controller {

    private final Ec2UseCaseService ec2UseCaseService;

    // ==================== 인스턴스 조회 ====================

    /**
     * EC2 인스턴스 목록을 조회합니다.
     * 
     * @param query 조회 조건 (페이지, 필터 등)
     * @return CloudResource 페이지
     */
    @GetMapping("/instances")
    @Operation(summary = "EC2 인스턴스 목록 조회", description = "조건에 맞는 EC2 인스턴스 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Page<CloudResource>> listInstances(
            @Parameter(description = "조회 조건") Ec2Query query) {
        
        log.info("[Ec2Controller] listInstances - query={}", query);
        
        try {
            Page<CloudResource> result = ec2UseCaseService.listInstances(query);
            log.info("[Ec2Controller] listInstances - success count={}", result.getTotalElements());
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("[Ec2Controller] listInstances - failed", e);
            throw e;
        }
    }

    /**
     * 특정 EC2 인스턴스를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return CloudResource 또는 404
     */
    @GetMapping("/instances/{instanceId}")
    @Operation(summary = "EC2 인스턴스 상세 조회", description = "특정 EC2 인스턴스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<CloudResource> getInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] getInstance - instanceId={}", instanceId);
        
        try {
            Optional<CloudResource> result = ec2UseCaseService.getInstance(instanceId);
            
            if (result.isPresent()) {
                log.info("[Ec2Controller] getInstance - success instanceId={}", instanceId);
                return ResponseEntity.ok(result.get());
            } else {
                log.info("[Ec2Controller] getInstance - not found instanceId={}", instanceId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("[Ec2Controller] getInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 인스턴스 생성 ====================

    /**
     * 새로운 EC2 인스턴스를 생성합니다.
     * 
     * @param request 생성 요청 정보
     * @return 생성된 인스턴스 ID
     */
    @PostMapping("/instances")
    @Operation(summary = "EC2 인스턴스 생성", description = "새로운 EC2 인스턴스를 생성합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "생성 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> createInstance(
            @Parameter(description = "생성 요청 정보") @RequestBody Ec2CreateRequest request) {
        
        log.info("[Ec2Controller] createInstance - imageId={}, instanceType={}", 
                request.getImageId(), request.getInstanceType());
        
        try {
            String instanceId = ec2UseCaseService.createInstance(request);
            log.info("[Ec2Controller] createInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok(instanceId);
            
        } catch (Exception e) {
            log.error("[Ec2Controller] createInstance - failed", e);
            throw e;
        }
    }

    // ==================== 인스턴스 생명주기 관리 ====================

    /**
     * EC2 인스턴스를 시작합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/start")
    @Operation(summary = "EC2 인스턴스 시작", description = "중지된 EC2 인스턴스를 시작합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "시작 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> startInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] startInstance - instanceId={}", instanceId);
        
        try {
            ec2UseCaseService.startInstance(instanceId);
            log.info("[Ec2Controller] startInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] startInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 중지합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/stop")
    @Operation(summary = "EC2 인스턴스 중지", description = "실행 중인 EC2 인스턴스를 중지합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "중지 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> stopInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] stopInstance - instanceId={}", instanceId);
        
        try {
            ec2UseCaseService.stopInstance(instanceId);
            log.info("[Ec2Controller] stopInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] stopInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 재부팅합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/reboot")
    @Operation(summary = "EC2 인스턴스 재부팅", description = "실행 중인 EC2 인스턴스를 재부팅합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "재부팅 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> rebootInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] rebootInstance - instanceId={}", instanceId);
        
        try {
            ec2UseCaseService.rebootInstance(instanceId);
            log.info("[Ec2Controller] rebootInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] rebootInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 종료합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/terminate")
    @Operation(summary = "EC2 인스턴스 종료", description = "EC2 인스턴스를 종료합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "종료 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> terminateInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] terminateInstance - instanceId={}", instanceId);
        
        try {
            ec2UseCaseService.terminateInstance(instanceId);
            log.info("[Ec2Controller] terminateInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] terminateInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스를 삭제합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param request 삭제 요청 정보
     * @return 성공 응답
     */
    @DeleteMapping("/instances/{instanceId}")
    @Operation(summary = "EC2 인스턴스 삭제", description = "EC2 인스턴스를 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> deleteInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "삭제 요청 정보") @RequestBody(required = false) Ec2DeleteRequest request) {
        
        log.info("[Ec2Controller] deleteInstance - instanceId={}", instanceId);
        
        try {
            // 요청이 없으면 기본 삭제 요청 생성
            if (request == null) {
                request = Ec2DeleteRequest.basic(instanceId);
            }
            
            ec2UseCaseService.deleteInstance(request);
            log.info("[Ec2Controller] deleteInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] deleteInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 인스턴스 수정 ====================

    /**
     * EC2 인스턴스 정보를 수정합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param request 수정 요청 정보
     * @return 성공 응답
     */
    @PutMapping("/instances/{instanceId}")
    @Operation(summary = "EC2 인스턴스 수정", description = "EC2 인스턴스의 정보를 수정합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "수정 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> updateInstance(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "수정 요청 정보") @RequestBody Ec2UpdateRequest request) {
        
        log.info("[Ec2Controller] updateInstance - instanceId={}", instanceId);
        
        try {
            ec2UseCaseService.updateInstance(request);
            log.info("[Ec2Controller] updateInstance - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] updateInstance - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 태그 관리 ====================

    /**
     * EC2 인스턴스에 태그를 추가합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tags 추가할 태그
     * @return 성공 응답
     */
    @PostMapping("/instances/{instanceId}/tags")
    @Operation(summary = "EC2 인스턴스 태그 추가", description = "EC2 인스턴스에 태그를 추가합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "태그 추가 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> addTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "추가할 태그") @RequestBody Map<String, String> tags) {
        
        log.info("[Ec2Controller] addTags - instanceId={}, tags={}", instanceId, tags);
        
        try {
            ec2UseCaseService.addTags(instanceId, tags);
            log.info("[Ec2Controller] addTags - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] addTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스에서 태그를 제거합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param tagKeys 제거할 태그 키들
     * @return 성공 응답
     */
    @DeleteMapping("/instances/{instanceId}/tags")
    @Operation(summary = "EC2 인스턴스 태그 제거", description = "EC2 인스턴스에서 태그를 제거합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "태그 제거 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Void> removeTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId,
            @Parameter(description = "제거할 태그 키들") @RequestBody Map<String, String> tagKeys) {
        
        log.info("[Ec2Controller] removeTags - instanceId={}, tagKeys={}", instanceId, tagKeys.keySet());
        
        try {
            ec2UseCaseService.removeTags(instanceId, tagKeys);
            log.info("[Ec2Controller] removeTags - success instanceId={}", instanceId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            log.error("[Ec2Controller] removeTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스의 모든 태그를 조회합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 태그 맵
     */
    @GetMapping("/instances/{instanceId}/tags")
    @Operation(summary = "EC2 인스턴스 태그 조회", description = "EC2 인스턴스의 모든 태그를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<Map<String, String>> getTags(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] getTags - instanceId={}", instanceId);
        
        try {
            Map<String, String> tags = ec2UseCaseService.getTags(instanceId);
            log.info("[Ec2Controller] getTags - success instanceId={}, tagCount={}", instanceId, tags.size());
            return ResponseEntity.ok(tags);
            
        } catch (Exception e) {
            log.error("[Ec2Controller] getTags - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    // ==================== 상태 확인 ====================

    /**
     * EC2 인스턴스의 현재 상태를 확인합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @return 인스턴스 상태
     */
    @GetMapping("/instances/{instanceId}/status")
    @Operation(summary = "EC2 인스턴스 상태 확인", description = "EC2 인스턴스의 현재 상태를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "인스턴스 없음"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    public ResponseEntity<String> getInstanceStatus(
            @Parameter(description = "인스턴스 ID") @PathVariable String instanceId) {
        
        log.info("[Ec2Controller] getInstanceStatus - instanceId={}", instanceId);
        
        try {
            String status = ec2UseCaseService.getInstanceStatus(instanceId);
            log.info("[Ec2Controller] getInstanceStatus - success instanceId={}, status={}", instanceId, status);
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("[Ec2Controller] getInstanceStatus - failed instanceId={}", instanceId, e);
            throw e;
        }
    }

    /**
     * EC2 인스턴스가 특정 상태에 도달할 때까지 대기합니다.
     * 
     * @param instanceId 인스턴스 ID
     * @param targetStatus 목표 상태
     * @param timeoutSeconds 타임아웃 (초)
     * @return 대기 성공 여부
     */
    @PostMapping("/instances/{instanceId}/wait")
    @Operation(summary = "EC2 인스턴스 상태 대기", description = "EC2 인스턴스가 특정 상태에 도달할 때까지 대기합니다.")
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
        
        log.info("[Ec2Controller] waitForInstanceStatus - instanceId={}, targetStatus={}, timeout={}s", 
                instanceId, targetStatus, timeoutSeconds);
        
        try {
            boolean success = ec2UseCaseService.waitForInstanceStatus(instanceId, targetStatus, timeoutSeconds);
            log.info("[Ec2Controller] waitForInstanceStatus - success={} instanceId={}", success, instanceId);
            return ResponseEntity.ok(success);
            
        } catch (Exception e) {
            log.error("[Ec2Controller] waitForInstanceStatus - failed instanceId={}", instanceId, e);
            throw e;
        }
    }
}
