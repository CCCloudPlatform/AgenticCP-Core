package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.common.exception.ResourceNotFoundException;
import com.agenticcp.core.domain.tenant.dto.TenantConfigRequest;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.repository.TenantConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 테넌트 설정 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class TenantConfigServiceTest {

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantConfigService tenantConfigService;

    private Tenant testTenant;
    private String tenantKey = "test-tenant";

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey(tenantKey)
                .tenantName("Test Tenant")
                .tenantType(Tenant.TenantType.ENTERPRISE)
                .build();
    }

    @Test
    void getTenantConfigurations_성공() {
        // Given
        TenantConfig config1 = TenantConfig.builder()
                .configKey("setting1")
                .configValue("value1")
                .configType(TenantConfig.ConfigType.STRING)
                .build();

        TenantConfig config2 = TenantConfig.builder()
                .configKey("setting2")
                .configValue("value2")
                .configType(TenantConfig.ConfigType.NUMBER)
                .build();

        when(tenantConfigRepository.findByTenantKey(tenantKey))
                .thenReturn(List.of(config1, config2));

        // When
        List<TenantConfig> result = tenantConfigService.getTenantConfigurations(tenantKey);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getConfigKey()).isEqualTo("setting1");
        assertThat(result.get(1).getConfigKey()).isEqualTo("setting2");
    }

    @Test
    void getTenantConfiguration_존재하는설정() {
        // Given
        TenantConfig config = TenantConfig.builder()
                .configKey("setting1")
                .configValue("value1")
                .configType(TenantConfig.ConfigType.STRING)
                .build();

        when(tenantConfigRepository.findByTenantKeyAndConfigKey(tenantKey, "setting1"))
                .thenReturn(Optional.of(config));

        // When
        Optional<TenantConfig> result = tenantConfigService.getTenantConfiguration(tenantKey, "setting1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getConfigKey()).isEqualTo("setting1");
    }

    @Test
    void getTenantConfiguration_존재하지않는설정() {
        // Given
        when(tenantConfigRepository.findByTenantKeyAndConfigKey(tenantKey, "non_existent"))
                .thenReturn(Optional.empty());

        // When
        Optional<TenantConfig> result = tenantConfigService.getTenantConfiguration(tenantKey, "non_existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void saveTenantConfiguration_새설정생성() {
        // Given
        TenantConfigRequest request = TenantConfigRequest.builder()
                .configKey("new_setting")
                .configValue("new_value")
                .configType(TenantConfig.ConfigType.STRING)
                .description("New setting")
                .isEncrypted(false)
                .build();

        TenantConfig savedConfig = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("new_setting")
                .configValue("new_value")
                .configType(TenantConfig.ConfigType.STRING)
                .description("New setting")
                .isEncrypted(false)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "new_setting"))
                .thenReturn(Optional.empty());
        when(tenantConfigRepository.save(any(TenantConfig.class))).thenReturn(savedConfig);

        // When
        TenantConfig result = tenantConfigService.saveTenantConfiguration(tenantKey, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigKey()).isEqualTo("new_setting");
        assertThat(result.getConfigValue()).isEqualTo("new_value");
    }

    @Test
    void saveTenantConfiguration_기존설정수정() {
        // Given
        TenantConfigRequest request = TenantConfigRequest.builder()
                .configKey("existing_setting")
                .configValue("updated_value")
                .configType(TenantConfig.ConfigType.STRING)
                .description("Updated setting")
                .isEncrypted(false)
                .build();

        TenantConfig existingConfig = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("existing_setting")
                .configValue("old_value")
                .configType(TenantConfig.ConfigType.STRING)
                .description("Old setting")
                .isEncrypted(false)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "existing_setting"))
                .thenReturn(Optional.of(existingConfig));
        when(tenantConfigRepository.save(existingConfig)).thenReturn(existingConfig);

        // When
        TenantConfig result = tenantConfigService.saveTenantConfiguration(tenantKey, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConfigValue()).isEqualTo("updated_value");
        assertThat(result.getDescription()).isEqualTo("Updated setting");
    }

    @Test
    void deleteTenantConfiguration_성공() {
        // Given
        TenantConfig config = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("setting_to_delete")
                .configValue("value")
                .configType(TenantConfig.ConfigType.STRING)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "setting_to_delete"))
                .thenReturn(Optional.of(config));
        when(tenantConfigRepository.save(config)).thenReturn(config);

        // When
        tenantConfigService.deleteTenantConfiguration(tenantKey, "setting_to_delete");

        // Then
        assertThat(config.getIsDeleted()).isTrue();
    }

    @Test
    void deleteTenantConfiguration_존재하지않는설정() {
        // Given
        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "non_existent"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tenantConfigService.deleteTenantConfiguration(tenantKey, "non_existent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteAllTenantConfigurations_성공() {
        // Given
        TenantConfig config1 = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("setting1")
                .configValue("value1")
                .configType(TenantConfig.ConfigType.STRING)
                .build();

        TenantConfig config2 = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("setting2")
                .configValue("value2")
                .configType(TenantConfig.ConfigType.NUMBER)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndIsDeletedFalse(testTenant))
                .thenReturn(List.of(config1, config2));
        when(tenantConfigRepository.saveAll(List.of(config1, config2)))
                .thenReturn(List.of(config1, config2));

        // When
        tenantConfigService.deleteAllTenantConfigurations(tenantKey);

        // Then
        assertThat(config1.getIsDeleted()).isTrue();
        assertThat(config2.getIsDeleted()).isTrue();
    }
}
