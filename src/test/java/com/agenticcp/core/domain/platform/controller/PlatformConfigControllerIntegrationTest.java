package com.agenticcp.core.domain.platform.controller;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PlatformConfigController 통합 테스트
 *
 * @author AgenticCP Team
 * @since 2025-09-26
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("PlatformConfigController 통합 테스트")
@org.junit.jupiter.api.Disabled("develop 브랜치에 없는 테스트")
class PlatformConfigControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PlatformConfigRepository platformConfigRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // 테스트 데이터 정리는 @Transactional이 자동으로 처리
    }

    @Test
    @DisplayName("유효한 설정 생성 성공")
    void shouldCreateValidConfig() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.config.key")
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .description("Test configuration")
                .isEncrypted(false)
                .isSystem(false)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configKey").value("test.config.key"))
                .andExpect(jsonPath("$.data.configValue").value("test value"))
                .andExpect(jsonPath("$.data.configType").value("STRING"));
    }

    @Test
    @DisplayName("잘못된 설정 키로 인한 생성 실패")
    void shouldFailToCreateConfigWithInvalidKey() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("123invalid.key") // 숫자로 시작하는 잘못된 키
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6002"));
    }

    @Test
    @DisplayName("빈 문자열 값으로 인한 생성 실패")
    void shouldFailToCreateConfigWithEmptyValue() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.config.key")
                .configValue("") // 빈 문자열
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6006"));
    }

    @Test
    @DisplayName("잘못된 JSON 형식으로 인한 생성 실패")
    void shouldFailToCreateConfigWithInvalidJson() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.json.key")
                .configValue("{ invalid json }") // 잘못된 JSON
                .configType(PlatformConfig.ConfigType.JSON)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6009"));
    }

    @Test
    @DisplayName("잘못된 불린 값으로 인한 생성 실패")
    void shouldFailToCreateConfigWithInvalidBoolean() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.boolean.key")
                .configValue("yes") // 잘못된 불린 값
                .configType(PlatformConfig.ConfigType.BOOLEAN)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6008"));
    }

    @Test
    @DisplayName("잘못된 숫자 형식으로 인한 생성 실패")
    void shouldFailToCreateConfigWithInvalidNumber() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.number.key")
                .configValue("not.a.number") // 잘못된 숫자
                .configType(PlatformConfig.ConfigType.NUMBER)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6007"));
    }

    @Test
    @DisplayName("중복 키로 인한 생성 실패")
    void shouldFailToCreateConfigWithDuplicateKey() throws Exception {
        // Given
        PlatformConfig existingConfig = PlatformConfig.builder()
                .configKey("duplicate.key")
                .configValue("existing value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();
        platformConfigRepository.save(existingConfig);

        PlatformConfig newConfig = PlatformConfig.builder()
                .configKey("duplicate.key") // 중복 키
                .configValue("new value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        // When & Then
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6012"));
    }

    @Test
    @DisplayName("시스템 설정 수정 실패")
    void shouldFailToUpdateSystemConfig() throws Exception {
        // Given
        PlatformConfig systemConfig = PlatformConfig.builder()
                .configKey("system.config.key")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(true)
                .build();
        platformConfigRepository.save(systemConfig);

        PlatformConfig updatedConfig = PlatformConfig.builder()
                .configValue("updated value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        // When & Then
        mockMvc.perform(put("/api/platform/configs/system.config.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedConfig)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6014"));
    }

    @Test
    @DisplayName("시스템 설정 삭제 실패")
    void shouldFailToDeleteSystemConfig() throws Exception {
        // Given
        PlatformConfig systemConfig = PlatformConfig.builder()
                .configKey("system.config.key")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(true)
                .build();
        platformConfigRepository.save(systemConfig);

        // When & Then
        mockMvc.perform(delete("/api/platform/configs/system.config.key"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("PLATFORM_6013"));
    }

    @Test
    @DisplayName("존재하지 않는 설정 조회 실패")
    void shouldFailToGetNonExistentConfig() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/platform/configs/non.existent.key"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("유효한 설정 조회 성공")
    void shouldGetValidConfig() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("test.config.key")
                .configValue("test value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();
        platformConfigRepository.save(config);

        // When & Then
        mockMvc.perform(get("/api/platform/configs/test.config.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.configKey").value("test.config.key"))
                .andExpect(jsonPath("$.data.configValue").value("test value"));
    }

    @Test
    @DisplayName("ENCRYPTED 저장 시 암호문 저장 및 isEncrypted=true 확인")
    void shouldEncryptOnSaveForEncryptedType() throws Exception {
        // Given
        String plaintext = "super-secret-token";
        PlatformConfig config = PlatformConfig.builder()
                .configKey("secret.api.token")
                .configValue(plaintext)
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .description("Secret token")
                .isSystem(false)
                .build();

        // When
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // Then: 저장된 값이 평문이 아니고, isEncrypted=true
        PlatformConfig saved = platformConfigRepository.findByConfigKey("secret.api.token").orElseThrow();
        assert saved.getIsEncrypted() != null && saved.getIsEncrypted();
        assert saved.getConfigValue() != null && !saved.getConfigValue().equals(plaintext);
        // 대략적 Base64 형태 및 IV 포함 길이 확인 (12바이트 IV + 태그 포함 암호문)
        byte[] decoded = java.util.Base64.getDecoder().decode(saved.getConfigValue());
        assert decoded.length > 12;
    }

    @Test
    @DisplayName("ENCRYPTED 조회 기본값은 마스킹되어야 함(showSecret 미지정/false)")
    void shouldMaskEncryptedValueByDefaultOnRead() throws Exception {
        // Given: ENCRYPTED 타입을 먼저 저장하여 암호문 상태가 되도록 함
        PlatformConfig config = PlatformConfig.builder()
                .configKey("masked.secret.key")
                .configValue("plain-secret")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isSystem(false)
                .build();
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated());

        // When & Then: showSecret 미지정 → 기본 false, 마스킹("***")
        mockMvc.perform(get("/api/platform/configs/masked.secret.key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("***"));

        // When & Then: showSecret=false 명시
        mockMvc.perform(get("/api/platform/configs/masked.secret.key").param("showSecret", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("***"));
    }

    @Test
    @DisplayName("관리자 권한 시 showSecret=true로 평문 조회 가능")
    void shouldAllowShowSecretForAdmin() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("admin.secret.key")
                .configValue("admin-secret-value")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isSystem(false)
                .build();
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated());

        // 관리자 권한 설정
        var auth = new UsernamePasswordAuthenticationToken(
                "adminUser",
                "N/A",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When & Then: showSecret=true 시 평문 반환
        mockMvc.perform(get("/api/platform/configs/admin.secret.key")
                        .param("showSecret", "true")
                        .header("X-Reason", "integration-test")
                        .header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.configValue").value("admin-secret-value"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Pragma", org.hamcrest.Matchers.containsString("no-cache")));
    }

    @Test
    @DisplayName("비관리자 showSecret=true 접근은 403 반환")
    void shouldDenyShowSecretForNonAdmin() throws Exception {
        // Given
        PlatformConfig config = PlatformConfig.builder()
                .configKey("user.secret.key")
                .configValue("user-secret-value")
                .configType(PlatformConfig.ConfigType.ENCRYPTED)
                .isSystem(false)
                .build();
        mockMvc.perform(post("/api/platform/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isCreated());

        // 일반 사용자 권한 설정
        var auth = new UsernamePasswordAuthenticationToken(
                "normalUser",
                "N/A",
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // When & Then: 403
        mockMvc.perform(get("/api/platform/configs/user.secret.key").param("showSecret", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }
}
