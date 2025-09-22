package com.agenticcp.core.domain.platform.integration;

import com.agenticcp.core.AgenticCpCoreApplication;
import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.platform.dto.PlatformConfigDtos;
import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.repository.PlatformConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlatformConfig 통합 테스트
 * - 실제 데이터베이스와의 연동 테스트
 * - 전체 플로우 검증
 * - 트랜잭션 처리 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PlatformConfigIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    
    private PlatformConfigRepository platformConfigRepository;

    private String baseUrl;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/platform/configs";
        // 데이터 초기화는 각 테스트에서 개별적으로 처리
    }

    @Test
    @DisplayName("전체 플로우 테스트 - 생성 → 조회 → 수정 → 삭제")
    void fullWorkflowTest() throws Exception {
        // 1. 설정 생성
        PlatformConfigDtos.CreateRequest createRequest = new PlatformConfigDtos.CreateRequest();
        createRequest.setKey("integration.test");
        createRequest.setType(PlatformConfig.ConfigType.STRING);
        createRequest.setValue("initial value");
        createRequest.setIsSystem(false);
        createRequest.setDescription("Integration test config");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlatformConfigDtos.CreateRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(baseUrl, createEntity, ApiResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(createResponse.getBody()).isSuccess()).isTrue();

        // 2. 생성된 설정 조회
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(baseUrl + "/integration.test", ApiResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getResponse.getBody()).isSuccess()).isTrue();

        // 3. 설정 수정
        PlatformConfigDtos.UpdateRequest updateRequest = new PlatformConfigDtos.UpdateRequest();
        updateRequest.setType(PlatformConfig.ConfigType.STRING);
        updateRequest.setValue("updated value");
        updateRequest.setDescription("Updated description");

        HttpEntity<PlatformConfigDtos.UpdateRequest> updateEntity = new HttpEntity<>(updateRequest, headers);
        ResponseEntity<ApiResponse> updateResponse = restTemplate.exchange(
                baseUrl + "/integration.test", HttpMethod.PUT, updateEntity, ApiResponse.class);
        assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(updateResponse.getBody()).isSuccess()).isTrue();

        // 4. 수정된 설정 조회
        ResponseEntity<ApiResponse> getUpdatedResponse = restTemplate.getForEntity(baseUrl + "/integration.test", ApiResponse.class);
        assertThat(getUpdatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 5. 설정 삭제
        ResponseEntity<ApiResponse> deleteResponse = restTemplate.exchange(
                baseUrl + "/integration.test", HttpMethod.DELETE, HttpEntity.EMPTY, ApiResponse.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(deleteResponse.getBody()).isSuccess()).isTrue();

        // 6. 삭제된 설정 조회 (소프트 삭제이므로 여전히 존재)
        ResponseEntity<ApiResponse> getDeletedResponse = restTemplate.getForEntity(baseUrl + "/integration.test", ApiResponse.class);
        assertThat(getDeletedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("ENCRYPTED 타입 설정 테스트")
    void encryptedConfigTest() throws Exception {
        // 1. ENCRYPTED 타입 설정 생성
        PlatformConfigDtos.CreateRequest createRequest = new PlatformConfigDtos.CreateRequest();
        createRequest.setKey("secret.config");
        createRequest.setType(PlatformConfig.ConfigType.ENCRYPTED);
        createRequest.setValue("secret_value_123");
        createRequest.setIsSystem(false);
        createRequest.setDescription("Secret configuration");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlatformConfigDtos.CreateRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(baseUrl, createEntity, ApiResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(createResponse.getBody()).isSuccess()).isTrue();

        // 2. 마스킹된 값으로 조회 (기본)
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(baseUrl + "/secret.config", ApiResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getResponse.getBody()).isSuccess()).isTrue();

        // 3. 평문으로 조회 (showSecret=true)
        ResponseEntity<ApiResponse> getSecretResponse = restTemplate.getForEntity(
                baseUrl + "/secret.config?showSecret=true", ApiResponse.class);
        assertThat(getSecretResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getSecretResponse.getBody()).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("JSON 타입 설정 테스트")
    void jsonConfigTest() throws Exception {
        // 1. JSON 타입 설정 생성
        PlatformConfigDtos.CreateRequest createRequest = new PlatformConfigDtos.CreateRequest();
        createRequest.setKey("json.config");
        createRequest.setType(PlatformConfig.ConfigType.JSON);
        createRequest.setValue("{\"key\": \"value\", \"number\": 123, \"boolean\": true}");
        createRequest.setIsSystem(false);
        createRequest.setDescription("JSON configuration");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlatformConfigDtos.CreateRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(baseUrl, createEntity, ApiResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(createResponse.getBody()).isSuccess()).isTrue();

        // 2. 생성된 JSON 설정 조회
        ResponseEntity<ApiResponse> getResponse = restTemplate.getForEntity(baseUrl + "/json.config", ApiResponse.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(getResponse.getBody()).isSuccess()).isTrue();
    }

    @Test
    @DisplayName("시스템 설정 삭제 금지 테스트")
    void systemConfigDeleteTest() throws Exception {
        // 1. 시스템 설정 생성
        PlatformConfig systemConfig = PlatformConfig.builder()
                .configKey("system.config")
                .configValue("system value")
                .configType(PlatformConfig.ConfigType.STRING)
                .isSystem(true)
                .description("System configuration")
                .build();
        platformConfigRepository.save(systemConfig);

        // 2. 시스템 설정 삭제 시도 (실패해야 함)
        ResponseEntity<ApiResponse> deleteResponse = restTemplate.exchange(
                baseUrl + "/system.config", HttpMethod.DELETE, HttpEntity.EMPTY, ApiResponse.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(Objects.requireNonNull(deleteResponse.getBody()).isSuccess()).isFalse();
    }

    @Test
    @DisplayName("잘못된 타입 값 검증 테스트")
    void invalidTypeValueTest() throws Exception {
        // 1. NUMBER 타입에 잘못된 값
        PlatformConfigDtos.CreateRequest createRequest = new PlatformConfigDtos.CreateRequest();
        createRequest.setKey("invalid.number");
        createRequest.setType(PlatformConfig.ConfigType.NUMBER);
        createRequest.setValue("not-a-number");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<PlatformConfigDtos.CreateRequest> createEntity = new HttpEntity<>(createRequest, headers);

        ResponseEntity<ApiResponse> createResponse = restTemplate.postForEntity(baseUrl, createEntity, ApiResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(Objects.requireNonNull(createResponse.getBody()).isSuccess()).isFalse();
    }
}
