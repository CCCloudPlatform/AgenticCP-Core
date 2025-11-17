package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.platform.entity.PlatformConfig;
import com.agenticcp.core.domain.platform.service.PlatformConfigService;
import com.agenticcp.core.domain.tenant.dto.EffectiveConfigResponse;
import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantConfig;
import com.agenticcp.core.domain.tenant.entity.TenantTypeConfig;
import com.agenticcp.core.domain.tenant.repository.TenantConfigRepository;
import com.agenticcp.core.domain.tenant.repository.TenantTypeConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 테넌트 설정 상속 서비스 테스트
 */
@ExtendWith(MockitoExtension.class)
class TenantConfigInheritanceServiceTest {

    @Mock
    private PlatformConfigService platformConfigService;

    @Mock
    private TenantService tenantService;

    @Mock
    private TenantConfigRepository tenantConfigRepository;

    @Mock
    private TenantTypeConfigRepository tenantTypeConfigRepository;

    @InjectMocks
    private TenantConfigInheritanceService tenantConfigInheritanceService;

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
    void getEffectiveConfigurations_플랫폼설정만있는경우() {
        // Given
        PlatformConfig platformConfig = PlatformConfig.builder()
                .configKey("max_file_size")
                .configValue("100")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .description("Maximum file size in MB")
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(platformConfigService.getAllConfigs()).thenReturn(List.of(platformConfig));
        when(tenantTypeConfigRepository.findByTenantTypeAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE))
                .thenReturn(List.of());
        when(tenantConfigRepository.findByTenantAndIsDeletedFalse(testTenant))
                .thenReturn(List.of());

        // When
        EffectiveConfigResponse response = tenantConfigInheritanceService.getEffectiveConfigurations(tenantKey);

        // Then
        assertThat(response.getTenantKey()).isEqualTo(tenantKey);
        assertThat(response.getConfigurations()).hasSize(1);
        assertThat(response.getConfigurations().get("max_file_size")).isNotNull();
        assertThat(response.getConfigurations().get("max_file_size").getValue()).isEqualTo(100L);
        assertThat(response.getConfigurations().get("max_file_size").getSource())
                .isEqualTo(EffectiveConfigResponse.ConfigSource.PLATFORM);
    }

    @Test
    void getEffectiveConfigurations_테넌트타입설정오버라이드() {
        // Given
        PlatformConfig platformConfig = PlatformConfig.builder()
                .configKey("max_users")
                .configValue("50")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .description("Default max users")
                .build();

        TenantTypeConfig typeConfig = TenantTypeConfig.builder()
                .tenantType(Tenant.TenantType.ENTERPRISE)
                .configKey("max_users")
                .configValue("1000")
                .configType(TenantTypeConfig.ConfigType.NUMBER)
                .description("Enterprise max users")
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(platformConfigService.getAllConfigs()).thenReturn(List.of(platformConfig));
        when(tenantTypeConfigRepository.findByTenantTypeAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE))
                .thenReturn(List.of(typeConfig));
        when(tenantConfigRepository.findByTenantAndIsDeletedFalse(testTenant))
                .thenReturn(List.of());

        // When
        EffectiveConfigResponse response = tenantConfigInheritanceService.getEffectiveConfigurations(tenantKey);

        // Then
        assertThat(response.getConfigurations().get("max_users").getValue()).isEqualTo(1000L);
        assertThat(response.getConfigurations().get("max_users").getSource())
                .isEqualTo(EffectiveConfigResponse.ConfigSource.TENANT_TYPE);
    }

    @Test
    void getEffectiveConfigurations_테넌트설정최종오버라이드() {
        // Given
        PlatformConfig platformConfig = PlatformConfig.builder()
                .configKey("max_users")
                .configValue("50")
                .configType(PlatformConfig.ConfigType.NUMBER)
                .build();

        TenantTypeConfig typeConfig = TenantTypeConfig.builder()
                .tenantType(Tenant.TenantType.ENTERPRISE)
                .configKey("max_users")
                .configValue("1000")
                .configType(TenantTypeConfig.ConfigType.NUMBER)
                .build();

        TenantConfig tenantConfig = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("max_users")
                .configValue("2000")
                .configType(TenantConfig.ConfigType.NUMBER)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(platformConfigService.getAllConfigs()).thenReturn(List.of(platformConfig));
        when(tenantTypeConfigRepository.findByTenantTypeAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE))
                .thenReturn(List.of(typeConfig));
        when(tenantConfigRepository.findByTenantAndIsDeletedFalse(testTenant))
                .thenReturn(List.of(tenantConfig));

        // When
        EffectiveConfigResponse response = tenantConfigInheritanceService.getEffectiveConfigurations(tenantKey);

        // Then
        assertThat(response.getConfigurations().get("max_users").getValue()).isEqualTo(2000L);
        assertThat(response.getConfigurations().get("max_users").getSource())
                .isEqualTo(EffectiveConfigResponse.ConfigSource.TENANT);
    }

    @Test
    void getEffectiveConfiguration_개별설정조회() {
        // Given
        TenantConfig tenantConfig = TenantConfig.builder()
                .tenant(testTenant)
                .configKey("custom_setting")
                .configValue("custom_value")
                .configType(TenantConfig.ConfigType.STRING)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "custom_setting"))
                .thenReturn(Optional.of(tenantConfig));

        // When
        Object value = tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, "custom_setting");

        // Then
        assertThat(value).isEqualTo("custom_value");
    }

    @Test
    void getEffectiveConfiguration_타입설정조회() {
        // Given
        TenantTypeConfig typeConfig = TenantTypeConfig.builder()
                .tenantType(Tenant.TenantType.ENTERPRISE)
                .configKey("type_setting")
                .configValue("type_value")
                .configType(TenantTypeConfig.ConfigType.STRING)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "type_setting"))
                .thenReturn(Optional.empty());
        when(tenantTypeConfigRepository.findByTenantTypeAndConfigKeyAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE, "type_setting"))
                .thenReturn(Optional.of(typeConfig));

        // When
        Object value = tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, "type_setting");

        // Then
        assertThat(value).isEqualTo("type_value");
    }

    @Test
    void getEffectiveConfiguration_플랫폼설정조회() {
        // Given
        PlatformConfig platformConfig = PlatformConfig.builder()
                .configKey("platform_setting")
                .configValue("platform_value")
                .configType(PlatformConfig.ConfigType.STRING)
                .build();

        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "platform_setting"))
                .thenReturn(Optional.empty());
        when(tenantTypeConfigRepository.findByTenantTypeAndConfigKeyAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE, "platform_setting"))
                .thenReturn(Optional.empty());
        when(platformConfigService.getConfigByKey("platform_setting"))
                .thenReturn(Optional.of(platformConfig));

        // When
        Object value = tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, "platform_setting");

        // Then
        assertThat(value).isEqualTo("platform_value");
    }

    @Test
    void getEffectiveConfiguration_설정이없는경우() {
        // Given
        when(tenantService.getTenantByKeyOrThrow(tenantKey)).thenReturn(testTenant);
        when(tenantConfigRepository.findByTenantAndConfigKeyAndIsDeletedFalse(testTenant, "non_existent"))
                .thenReturn(Optional.empty());
        when(tenantTypeConfigRepository.findByTenantTypeAndConfigKeyAndIsDeletedFalse(Tenant.TenantType.ENTERPRISE, "non_existent"))
                .thenReturn(Optional.empty());
        when(platformConfigService.getConfigByKey("non_existent"))
                .thenReturn(Optional.empty());

        // When
        Object value = tenantConfigInheritanceService.getEffectiveConfiguration(tenantKey, "non_existent");

        // Then
        assertThat(value).isNull();
    }
}



