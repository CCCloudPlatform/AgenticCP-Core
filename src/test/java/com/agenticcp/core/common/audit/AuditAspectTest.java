package com.agenticcp.core.common.audit;

import com.agenticcp.core.common.dto.audit.AuditContextDto;
import com.agenticcp.core.common.enums.AuditResourceType;
import com.agenticcp.core.common.enums.AuditSeverity;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AuditAspect} 단위 테스트.
 *
 * 감사 애노테이션이 선언된 메서드가 호출될 때 {@link AuditService}로 위임되는지 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditAspect 단위 테스트")
class AuditAspectTest {

    @Mock
    private AuditService auditService;

    private TestAuditedComponent proxy;

    @BeforeEach
    void setUp() {
        AuditAspect auditAspect = new AuditAspect(auditService);
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(new TestAuditedComponent());
        proxyFactory.addAspect(auditAspect);
        proxy = proxyFactory.getProxy();
    }

    @Test
    @DisplayName("메서드 레벨 감사 애노테이션 적용 시 AuditService 호출")
    void auditedMethod_WithAuditRequired_DelegatesToAuditService() throws Throwable {
        // Given
        when(auditService.audit(any(ProceedingJoinPoint.class), any(AuditContextDto.class)))
            .thenAnswer(invocation -> {
                ProceedingJoinPoint joinPoint = invocation.getArgument(0);
                return joinPoint.proceed();
            });

        // When
        String result = proxy.auditedOperation("tenant-1");

        // Then
        assertThat(result).isEqualTo("audited:tenant-1");

        ArgumentCaptor<AuditContextDto> contextCaptor = ArgumentCaptor.forClass(AuditContextDto.class);
        verify(auditService).audit(any(ProceedingJoinPoint.class), contextCaptor.capture());

        AuditContextDto capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.action()).isEqualTo("TEST_ACTION");
        assertThat(capturedContext.includeRequestData()).isTrue();
        assertThat(capturedContext.includeResponseData()).isFalse();
    }

    static class TestAuditedComponent {

        @AuditRequired(
            action = "TEST_ACTION",
            description = "테스트 감사",
            resourceType = AuditResourceType.CONFIG,
            severity = AuditSeverity.INFO,
            includeRequestData = true,
            includeResponseData = false
        )
        public String auditedOperation(String tenantId) {
            return "audited:" + tenantId;
        }

        public String nonAuditedOperation() {
            return "plain";
        }
    }
}

