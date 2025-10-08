package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.springframework.web.bind.annotation.*;

/**
 * 감사 로깅 테스트용 컨트롤러
 */
@RestController
@RequestMapping("/test/audit")
@AuditController(
    resourceType = AuditResourceType.USER,
    defaultSeverity = AuditSeverity.MEDIUM,
    defaultIncludeRequestData = true,
    defaultIncludeResponseData = false,
    targetHttpMethods = {"POST", "PUT", "PATCH", "DELETE"},
    excludeMethods = {"getUserInfo"}
)
public class TestAuditController {

    /**
     * 클래스 레벨 애노테이션으로 자동 감사 로깅 적용 (POST)
     */
    @PostMapping("/create")
    public String createUser(@RequestBody String userData) {
        return "User created: " + userData;
    }

    /**
     * 클래스 레벨 애노테이션으로 자동 감사 로깅 적용 (PUT)
     */
    @PutMapping("/update/{id}")
    public String updateUser(@PathVariable String id, @RequestBody String userData) {
        return "User updated: " + id + " with " + userData;
    }

    /**
     * 클래스 레벨 애노테이션으로 자동 감사 로깅 적용 (DELETE)
     */
    @DeleteMapping("/delete/{id}")
    public String deleteUser(@PathVariable String id) {
        return "User deleted: " + id;
    }

    /**
     * GET 메서드는 targetHttpMethods에 없어서 감사 로깅 안됨
     */
    @GetMapping("/get/{id}")
    public String getUser(@PathVariable String id) {
        return "User: " + id;
    }

    /**
     * excludeMethods에 포함되어 감사 로깅 안됨
     */
    @PostMapping("/getUserInfo")
    public String getUserInfo(@RequestBody String request) {
        return "User info: " + request;
    }

    /**
     * 메서드 레벨 애노테이션으로 개별 감사 로깅 적용
     */
    @PostMapping("/custom")
    @AuditRequired(
        action = "CUSTOM_USER_ACTION",
        resourceType = AuditResourceType.USER,
        description = "Custom user action for testing",
        includeRequestData = true,
        includeResponseData = true,
        severity = AuditSeverity.HIGH
    )
    public String customAction(@RequestBody String data) {
        return "Custom action result: " + data;
    }

    /**
     * 메서드 레벨 애노테이션이 클래스 레벨보다 우선
     */
    @PostMapping("/override")
    @AuditRequired(
        action = "OVERRIDE_ACTION",
        resourceType = AuditResourceType.CONFIG,
        description = "This overrides class-level settings",
        includeRequestData = false,
        includeResponseData = true,
        severity = AuditSeverity.CRITICAL
    )
    public String overrideAction(@RequestBody String data) {
        return "Override action result: " + data;
    }
}
