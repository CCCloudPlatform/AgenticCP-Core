package com.agenticcp.core.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("application", "AgenticCP-Core");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "READY");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
