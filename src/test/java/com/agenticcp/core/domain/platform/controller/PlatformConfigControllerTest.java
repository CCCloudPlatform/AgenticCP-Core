package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.dto.PlatformConfigDtos;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PlatformConfigController 통합 테스트
 * - REST API 엔드포인트 테스트
 * - HTTP 상태 코드 검증
 * - 요청/응답 JSON 검증
 */
@WebMvcTest(PlatformConfigController.class)
class PlatformConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlatformConfigService platformConfigService;

    @Autowired
    private ObjectMapper objectMapper;

    private PlatformConfig testConfig;
    private PlatformConfigDtos.CreateRequest createRequest;
    private PlatformConfigDtos.UpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        testConfig = PlatformConfig.builder()
                .configKey("test.key")
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Test configuration")
                .isSystem(false)
                .isEncrypted(false)
                .build();

        createRequest = new PlatformConfigDtos.CreateRequest();
        createRequest.setKey("test.key");
        createRequest.setType(PlatformConfig.ConfigType.STRING);
        createRequest.setValue("test value");
        createRequest.setIsSystem(false);
        createRequest.setDescription("Test configuration");

        updateRequest = new PlatformConfigDtos.UpdateRequest();
        updateRequest.setType(PlatformConfig.ConfigType.STRING);
        updateRequest.setValue("updated value");
        updateRequest.setDescription("Updated description");
    }

    @Test
    @DisplayName("전체 설정 조회 - 성공")
    void getAllConfigs_Success() throws Exception {
        // Given
        List<PlatformConfig> configs = Arrays.asList(testConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs, PageRequest.of(0, 10), 1);
        
        when(platformConfigService.getAllConfigs(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/platform/configs")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].configKey").value("test.key"))
                .andExpect(jsonPath("$.data.content[0].configValue").value("test value"));

        verify(platformConfigService).getAllConfigs(any());
    }

    @Test
    @DisplayName("특정 설정 조회 - 성공 (마스킹)")
    void getConfigByKey_Success_WithMasking() throws Exception {
        // Given
        PlatformConfig encryptedConfig = PlatformConfig.builder()
                .configKey("secret.key")
                .configValue("secret_value")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isEncrypted(true)
                .build();

        when(platformConfigService.getConfigByKey("secret.key")).thenReturn(Optional.of(encryptedConfig));

        // When & Then
        mockMvc.perform(get("/api/platform/configs/secret.key")
                .param("showSecret", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("secret.key"))
                .andExpect(jsonPath("$.data.value").value("******"))
                .andExpect(jsonPath("$.data.type").value("ENCRYPTED"));

        verify(platformConfigService).getConfigByKey("secret.key");
    }

    @Test
    @DisplayName("특정 설정 조회 - 성공 (평문 노출)")
    void getConfigByKey_Success_ShowSecret() throws Exception {
        // Given
        when(platformConfigService.getConfigByKey("test.key")).thenReturn(Optional.of(testConfig));

        // When & Then
        mockMvc.perform(get("/api/platform/configs/test.key")
                .param("showSecret", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.key").value("test.key"))
                .andExpect(jsonPath("$.data.value").value("test value"));

        verify(platformConfigService).getConfigByKey("test.key");
    }

    @Test
    @DisplayName("특정 설정 조회 - 존재하지 않는 키")
    void getConfigByKey_NotFound() throws Exception {
        // Given
        when(platformConfigService.getConfigByKey("nonexistent.key")).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/platform/configs/nonexistent.key"))
                .andExpect(status().isNotFound());

        verify(platformConfigService).getConfigByKey("nonexistent.key");
    }

    @Test
    @DisplayName("타입별 설정 조회 - 성공")
    void getConfigsByType_Success() throws Exception {
        // Given
        List<PlatformConfig> configs = Arrays.asList(testConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs, PageRequest.of(0, 10), 1);
        
        when(platformConfigService.getConfigsByType(eq(PlatformConfig.ConfigType.STRING), any()))
                .thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/platform/configs/type/STRING")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].configType").value("STRING"));

        verify(platformConfigService).getConfigsByType(eq(PlatformConfig.ConfigType.STRING), any());
    }

    @Test
    @DisplayName("시스템 설정 조회 - 성공")
    void getSystemConfigs_Success() throws Exception {
        // Given
        PlatformConfig systemConfig = PlatformConfig.builder()
                .configKey("system.key")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(true)
                .build();

        List<PlatformConfig> configs = Arrays.asList(systemConfig);
        Page<PlatformConfig> page = new PageImpl<>(configs, PageRequest.of(0, 10), 1);
        
        when(platformConfigService.getSystemConfigs(any())).thenReturn(page);

        // When & Then
        mockMvc.perform(get("/api/platform/configs/system")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].isSystem").value(true));

        verify(platformConfigService).getSystemConfigs(any());
    }

    @Test
    @DisplayName("설정 생성 - 성공")
    void createConfig_Success() throws Exception {
        // Given
        when(platformConfigService.createConfig(any(PlatformConfig.class))).thenReturn(testConfig);

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("플랫폼 설정이 생성되었습니다."))
                .andExpect(jsonPath("$.data.key").value("test.key"));

        verify(platformConfigService).createConfig(any(PlatformConfig.class));
    }

    @Test
    @DisplayName("설정 생성 - 유효성 검증 실패")
    void createConfig_ValidationError() throws Exception {
        // Given
        createRequest.setKey("ab"); // 너무 짧은 키
        createRequest.setValue(""); // 빈 값

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest());

        verify(platformConfigService, never()).createConfig(any());
    }

    @Test
    @DisplayName("설정 수정 - 성공")
    void updateConfig_Success() throws Exception {
        // Given
        when(platformConfigService.updateConfig(eq("test.key"), any(PlatformConfig.class)))
                .thenReturn(testConfig);

        // When & Then
        mockMvc.perform(put("/api/platform/configs/test.key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("플랫폼 설정이 수정되었습니다."))
                .andExpect(jsonPath("$.data.key").value("test.key"));

        verify(platformConfigService).updateConfig(eq("test.key"), any(PlatformConfig.class));
    }

    @Test
    @DisplayName("설정 수정 - 유효성 검증 실패")
    void updateConfig_ValidationError() throws Exception {
        // Given
        updateRequest.setValue("not-a-number");
        updateRequest.setType(PlatformConfig.ConfigType.NUMBER);

        // When & Then
        mockMvc.perform(put("/api/platform/configs/test.key")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());

        verify(platformConfigService, never()).updateConfig(anyString(), any());
    }

    @Test
    @DisplayName("설정 삭제 - 성공")
    void deleteConfig_Success() throws Exception {
        // Given
        doNothing().when(platformConfigService).deleteConfig("test.key");

        // When & Then
        mockMvc.perform(delete("/api/platform/configs/test.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("플랫폼 설정이 삭제되었습니다."));

        verify(platformConfigService).deleteConfig("test.key");
    }

    @Test
    @DisplayName("설정 삭제 - 시스템 설정 삭제 금지")
    void deleteConfig_SystemConfigNotAllowed() throws Exception {
        // Given
        doThrow(new com.agenticcp.core.common.exception.ValidationException("configKey", "system config cannot be deleted"))
                .when(platformConfigService).deleteConfig("system.key");

        // When & Then
        mockMvc.perform(delete("/api/platform/configs/system.key"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verify(platformConfigService).deleteConfig("system.key");
    }

    @Test
    @DisplayName("잘못된 JSON 요청")
    void invalidJsonRequest() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Content-Type이 없는 요청")
    void missingContentType() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isUnsupportedMediaType());
    }
}
