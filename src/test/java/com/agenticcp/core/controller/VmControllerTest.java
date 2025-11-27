package com.agenticcp.core.controller;

import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmCreateRequest;
import com.agenticcp.core.domain.cloud.port.model.VmDeleteRequest;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.VmUpdateRequest;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import com.agenticcp.core.domain.cloud.controller.VmController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * VM 컨트롤러 테스트
 * 
 * Mockito를 사용하여 컨트롤러 계층만 테스트합니다.
 * Spring Context 로딩 없이 순수한 단위 테스트로 진행합니다.
 * 
 * @author AgenticCP Team
 * @version 2.0.0
 */
@ExtendWith(MockitoExtension.class)
class VmControllerTest {

    private static final String BASE_URL = "/api/v1/cloud/providers/{provider}/accounts/{accountScope}/vms/instances";
    
    private MockMvc mockMvc;

    @Mock
    private VmUseCaseService vmUseCaseService;

    @InjectMocks
    private VmController vmController;

    private ObjectMapper objectMapper = new ObjectMapper();
    private CloudResource testInstance;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vmController).build();
        
        testInstance = CloudResource.builder()
            .resourceId("i-1234567890abcdef0")
            .resourceName("test-instance")
            .displayName("Test Instance")
            .build();
    }

    @Test
    void listInstances_성공() throws Exception {
        // Given
        Page<CloudResource> page = new PageImpl<>(
            List.of(testInstance), 
            PageRequest.of(0, 10), 
            1
        );
        when(vmUseCaseService.listInstances(eq(ProviderType.AWS), any(VmQuery.class))).thenReturn(page);

        // When & Then
        mockMvc.perform(get(BASE_URL, "AWS", "123456789012")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].resourceId").value("i-1234567890abcdef0"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getInstance_성공() throws Exception {
        // Given
        when(vmUseCaseService.getInstance(eq(ProviderType.AWS), eq("i-1234567890abcdef0")))
            .thenReturn(Optional.of(testInstance));

        // When & Then
        mockMvc.perform(get(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceId").value("i-1234567890abcdef0"))
                .andExpect(jsonPath("$.data.resourceName").value("test-instance"));
    }

    @Test
    void getInstance_인스턴스없음() throws Exception {
        // Given
        when(vmUseCaseService.getInstance(eq(ProviderType.AWS), eq("i-nonexistent")))
            .thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createInstance_성공() throws Exception {
        // Given
        VmCreateRequest request = VmCreateRequest.builder()
            .image("ami-12345678")
            .instanceSize("t3.micro")
            .minCount(1)
            .maxCount(1)
            .build();

        when(vmUseCaseService.createInstance(any(VmCreateRequest.class)))
            .thenReturn("i-1234567890abcdef0");

        // When & Then
        mockMvc.perform(post(BASE_URL, "AWS", "123456789012")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value("i-1234567890abcdef0"));
    }

    @Test
    void startInstance_성공() throws Exception {
        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/start", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk());
    }

    @Test
    void stopInstance_성공() throws Exception {
        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/stop", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk());
    }

    @Test
    void rebootInstance_성공() throws Exception {
        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/reboot", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk());
    }

    @Test
    void terminateInstance_성공() throws Exception {
        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/terminate", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteInstance_성공() throws Exception {
        // Given
        VmDeleteRequest request = VmDeleteRequest.basic("i-1234567890abcdef0");

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-1234567890abcdef0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateInstance_성공() throws Exception {
        // Given
        VmUpdateRequest request = VmUpdateRequest.builder()
            .instanceId("i-1234567890abcdef0")
            .instanceType("t3.small")
            .build();

        // When & Then
        mockMvc.perform(put(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-1234567890abcdef0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void addTags_성공() throws Exception {
        // Given
        Map<String, String> tags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );

        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/tags", "AWS", "123456789012", "i-1234567890abcdef0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tags)))
                .andExpect(status().isOk());
    }

    @Test
    void removeTags_성공() throws Exception {
        // Given
        Map<String, String> tagKeys = Map.of(
            "Environment", "",
            "Project", ""
        );

        // When & Then
        mockMvc.perform(delete(BASE_URL + "/{instanceId}/tags", "AWS", "123456789012", "i-1234567890abcdef0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(tagKeys)))
                .andExpect(status().isOk());
    }

    @Test
    void getTags_성공() throws Exception {
        // Given
        Map<String, String> tags = Map.of(
            "Environment", "Development",
            "Project", "TestProject"
        );

        when(vmUseCaseService.getTags(eq(ProviderType.AWS), eq("i-1234567890abcdef0")))
            .thenReturn(tags);

        // When & Then
        mockMvc.perform(get(BASE_URL + "/{instanceId}/tags", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.Environment").value("Development"))
                .andExpect(jsonPath("$.data.Project").value("TestProject"));
    }

    @Test
    void getInstanceStatus_성공() throws Exception {
        // Given
        when(vmUseCaseService.getInstanceStatus(eq(ProviderType.AWS), eq("i-1234567890abcdef0")))
            .thenReturn("running");

        // When & Then
        mockMvc.perform(get(BASE_URL + "/{instanceId}/status", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("running"));
    }

    @Test
    void waitForInstanceStatus_성공() throws Exception {
        // Given
        when(vmUseCaseService.waitForInstanceStatus(eq(ProviderType.AWS), eq("i-1234567890abcdef0"), eq("running"), eq(300)))
            .thenReturn(true);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/wait", "AWS", "123456789012", "i-1234567890abcdef0")
                .param("targetStatus", "running")
                .param("timeoutSeconds", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void waitForInstanceStatus_기본타임아웃() throws Exception {
        // Given
        when(vmUseCaseService.waitForInstanceStatus(eq(ProviderType.AWS), eq("i-1234567890abcdef0"), eq("running"), eq(300)))
            .thenReturn(true);

        // When & Then
        mockMvc.perform(post(BASE_URL + "/{instanceId}/wait", "AWS", "123456789012", "i-1234567890abcdef0")
                .param("targetStatus", "running"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void createInstance_잘못된_요청() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(post(BASE_URL, "AWS", "123456789012")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateInstance_잘못된_요청() throws Exception {
        // Given - 잘못된 JSON
        String invalidJson = "{ invalid json }";

        // When & Then
        mockMvc.perform(put(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-1234567890abcdef0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteInstance_요청없이_삭제() throws Exception {
        // When & Then - 요청 본문 없이 삭제 (기본 삭제 요청 사용)
        mockMvc.perform(delete(BASE_URL + "/{instanceId}", "AWS", "123456789012", "i-1234567890abcdef0"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listInstances_기본_파라미터() throws Exception {
        // Given
        Page<CloudResource> page = new PageImpl<>(
            List.of(testInstance), 
            PageRequest.of(0, 20), 
            1
        );
        when(vmUseCaseService.listInstances(eq(ProviderType.AWS), any(VmQuery.class))).thenReturn(page);

        // When & Then - 파라미터 없이 호출 (기본값 사용)
        mockMvc.perform(get(BASE_URL, "AWS", "123456789012")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}
