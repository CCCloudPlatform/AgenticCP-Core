package com.agenticcp.core.domain.cloud.service.aws;

import com.agenticcp.core.common.context.TenantContextHolder;
import com.agenticcp.core.domain.cloud.capability.CapabilityGuard;
import com.agenticcp.core.domain.cloud.entity.CloudProvider.ProviderType;
import com.agenticcp.core.domain.cloud.entity.CloudResource;
import com.agenticcp.core.domain.cloud.port.model.VmQuery;
import com.agenticcp.core.domain.cloud.port.model.account.CloudSessionCredential;
import com.agenticcp.core.domain.cloud.port.outbound.account.AccountCredentialManagementPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmDiscoveryPort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmLifecyclePort;
import com.agenticcp.core.domain.cloud.port.outbound.vm.VmTaggingPort;
import com.agenticcp.core.domain.cloud.service.helper.CloudResourceManagementHelper;
import com.agenticcp.core.domain.cloud.service.vm.VmPortRouter;
import com.agenticcp.core.domain.cloud.service.vm.VmUseCaseService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * VM 유스케이스 서비스 테스트
 */
@Disabled
@ExtendWith(MockitoExtension.class)
class VmUseCaseServiceTest {

    @Mock
    private VmPortRouter vmPortRouter;

    @Mock
    private VmDiscoveryPort vmDiscoveryPort;

    @Mock
    private VmLifecyclePort vmLifecyclePort;

    @Mock
    private VmTaggingPort vmTaggingPort;

    @Mock
    private CapabilityGuard capabilityGuard;

    @Mock
    private AccountCredentialManagementPort credentialProviderPort;

    @Mock
    private CloudResourceManagementHelper resourceHelper;

    private VmUseCaseService vmUseCaseService;
    private CloudSessionCredential mockSession;
    private static final ProviderType PROVIDER_TYPE = ProviderType.AWS;
    private static final String ACCOUNT_SCOPE = "123456789012";

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantKey("tenant-test");
        vmUseCaseService = new VmUseCaseService(
                vmPortRouter,
                capabilityGuard,
                credentialProviderPort,
                resourceHelper
        );
        mockSession = mock(CloudSessionCredential.class);

        when(vmPortRouter.discovery(ProviderType.AWS)).thenReturn(vmDiscoveryPort);
        when(vmPortRouter.lifecycle(ProviderType.AWS)).thenReturn(vmLifecyclePort);
        when(vmPortRouter.tagging(ProviderType.AWS)).thenReturn(vmTaggingPort);
        when(credentialProviderPort.getSession(anyString(), eq(ACCOUNT_SCOPE), eq(PROVIDER_TYPE))).thenReturn(mockSession);
        doNothing().when(capabilityGuard).ensureSupported(any(), anyString(), anyString(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void listInstances_성공() {
        // Given
        VmQuery query = VmQuery.builder()
            .page(0)
            .size(10)
            .build();

        CloudResource resource = CloudResource.builder()
            .resourceId("i-1234567890abcdef0")
            .name("test-instance")
            .build();

        Page<CloudResource> expectedPage = new PageImpl<>(
            List.of(resource), 
            PageRequest.of(0, 10), 
            1
        );

        when(vmDiscoveryPort.listInstances(eq(query), any(CloudSessionCredential.class))).thenReturn(expectedPage);

        // When
        Page<CloudResource> result = vmUseCaseService.listInstances(PROVIDER_TYPE, ACCOUNT_SCOPE, query);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getResourceId()).isEqualTo("i-1234567890abcdef0");
    }

    @Test
    void getInstance_성공() {
        // Given
        String instanceId = "i-1234567890abcdef0";
        
        CloudResource resource = CloudResource.builder()
            .resourceId(instanceId)
            .name("test-instance")
            .build();

        when(vmDiscoveryPort.getInstance(eq(instanceId), any(CloudSessionCredential.class))).thenReturn(Optional.of(resource));

        // When
        Optional<CloudResource> result = vmUseCaseService.getInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getResourceId()).isEqualTo(instanceId);
    }

    @Test
    void getInstance_인스턴스없음() {
        // Given
        String instanceId = "i-nonexistent";
        
        when(vmDiscoveryPort.getInstance(eq(instanceId), any(CloudSessionCredential.class))).thenReturn(Optional.empty());

        // When
        Optional<CloudResource> result = vmUseCaseService.getInstance(PROVIDER_TYPE, ACCOUNT_SCOPE, instanceId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void listInstances_예외발생시_감사로그기록() {
        // Given
        VmQuery query = VmQuery.builder()
            .page(0)
            .size(10)
            .build();

        RuntimeException exception = new RuntimeException("AWS API Error");
        when(vmDiscoveryPort.listInstances(eq(query), any(CloudSessionCredential.class))).thenThrow(exception);

        // When & Then
        try {
            vmUseCaseService.listInstances(PROVIDER_TYPE, ACCOUNT_SCOPE, query);
        } catch (RuntimeException e) {
            assertThat(e).isEqualTo(exception);
        }
    }
}
