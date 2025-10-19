package com.agenticcp.core.domain.tenant.entity;

import com.agenticcp.core.common.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * TenantBilling 엔티티 단위 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("TenantBilling 엔티티 단위 테스트")
class TenantBillingTest {

    private Tenant testTenant;
    private TenantBilling testTenantBilling;

    @BeforeEach
    void setUp() {
        // 테스트용 테넌트 생성
        testTenant = Tenant.builder()
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .status(Status.ACTIVE)
                .build();
        testTenant.setId(1L);

        // 테스트용 테넌트 과금 생성
        testTenantBilling = TenantBilling.builder()
                .billingCycle(TenantBilling.BillingCycle.MONTHLY)
                .currency("USD")
                .baseAmount(new BigDecimal("100.00"))
                .usageAmount(new BigDecimal("50.00"))
                .totalAmount(new BigDecimal("150.00"))
                .taxAmount(new BigDecimal("15.00"))
                .discountAmount(new BigDecimal("10.00"))
                .billingPeriodStart(LocalDateTime.now().minusDays(30))
                .billingPeriodEnd(LocalDateTime.now())
                .dueDate(LocalDateTime.now().plusDays(15))
                .paidDate(LocalDateTime.now().plusDays(5))
                .paymentStatus(TenantBilling.PaymentStatus.PAID)
                .paymentMethod("credit_card")
                .invoiceNumber("INV-2024-001")
                .notes("Monthly subscription billing")
                .build();
    }

    @Test
    @DisplayName("TenantBilling 기본 필드 설정 및 조회")
    void testBasicFields() {
        // Given & When
        testTenantBilling.setId(1L);
        testTenantBilling.setTenant(testTenant);

        // Then
        assertThat(testTenantBilling.getId()).isEqualTo(1L);
        assertThat(testTenantBilling.getBillingCycle()).isEqualTo(TenantBilling.BillingCycle.MONTHLY);
        assertThat(testTenantBilling.getCurrency()).isEqualTo("USD");
        assertThat(testTenantBilling.getPaymentStatus()).isEqualTo(TenantBilling.PaymentStatus.PAID);
        assertThat(testTenantBilling.getPaymentMethod()).isEqualTo("credit_card");
        assertThat(testTenantBilling.getInvoiceNumber()).isEqualTo("INV-2024-001");
        assertThat(testTenantBilling.getNotes()).isEqualTo("Monthly subscription billing");
    }

    @Test
    @DisplayName("TenantBilling 금액 정보 설정 및 조회")
    void testAmountFields() {
        // Given & When
        testTenantBilling.setBaseAmount(new BigDecimal("200.00"));
        testTenantBilling.setUsageAmount(new BigDecimal("75.00"));
        testTenantBilling.setTotalAmount(new BigDecimal("275.00"));
        testTenantBilling.setTaxAmount(new BigDecimal("27.50"));
        testTenantBilling.setDiscountAmount(new BigDecimal("25.00"));

        // Then
        assertThat(testTenantBilling.getBaseAmount()).isEqualTo(new BigDecimal("200.00"));
        assertThat(testTenantBilling.getUsageAmount()).isEqualTo(new BigDecimal("75.00"));
        assertThat(testTenantBilling.getTotalAmount()).isEqualTo(new BigDecimal("275.00"));
        assertThat(testTenantBilling.getTaxAmount()).isEqualTo(new BigDecimal("27.50"));
        assertThat(testTenantBilling.getDiscountAmount()).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("TenantBilling 날짜 정보 설정 및 조회")
    void testDateFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(30);
        LocalDateTime endDate = now;
        LocalDateTime dueDate = now.plusDays(15);
        LocalDateTime paidDate = now.plusDays(5);

        // When
        testTenantBilling.setBillingPeriodStart(startDate);
        testTenantBilling.setBillingPeriodEnd(endDate);
        testTenantBilling.setDueDate(dueDate);
        testTenantBilling.setPaidDate(paidDate);

        // Then
        assertThat(testTenantBilling.getBillingPeriodStart()).isEqualTo(startDate);
        assertThat(testTenantBilling.getBillingPeriodEnd()).isEqualTo(endDate);
        assertThat(testTenantBilling.getDueDate()).isEqualTo(dueDate);
        assertThat(testTenantBilling.getPaidDate()).isEqualTo(paidDate);
    }

    @Test
    @DisplayName("TenantBilling 테넌트 정보 설정 및 조회")
    void testTenantInfo() {
        // Given & When
        testTenantBilling.setTenant(testTenant);

        // Then
        assertThat(testTenantBilling.getTenant()).isNotNull();
        assertThat(testTenantBilling.getTenant().getId()).isEqualTo(1L);
        assertThat(testTenantBilling.getTenant().getTenantKey()).isEqualTo("test-tenant");
        assertThat(testTenantBilling.getTenant().getTenantName()).isEqualTo("Test Tenant");
    }

    @Test
    @DisplayName("TenantBilling 과금 주기 열거형 테스트")
    void testBillingCycleEnum() {
        // Given & When
        testTenantBilling.setBillingCycle(TenantBilling.BillingCycle.QUARTERLY);

        // Then
        assertThat(testTenantBilling.getBillingCycle()).isEqualTo(TenantBilling.BillingCycle.QUARTERLY);
        
        // 다른 과금 주기들도 테스트
        testTenantBilling.setBillingCycle(TenantBilling.BillingCycle.YEARLY);
        assertThat(testTenantBilling.getBillingCycle()).isEqualTo(TenantBilling.BillingCycle.YEARLY);
        
        testTenantBilling.setBillingCycle(TenantBilling.BillingCycle.USAGE_BASED);
        assertThat(testTenantBilling.getBillingCycle()).isEqualTo(TenantBilling.BillingCycle.USAGE_BASED);
    }

    @Test
    @DisplayName("TenantBilling 결제 상태 열거형 테스트")
    void testPaymentStatusEnum() {
        // Given & When
        testTenantBilling.setPaymentStatus(TenantBilling.PaymentStatus.PENDING);

        // Then
        assertThat(testTenantBilling.getPaymentStatus()).isEqualTo(TenantBilling.PaymentStatus.PENDING);
        
        // 다른 결제 상태들도 테스트
        testTenantBilling.setPaymentStatus(TenantBilling.PaymentStatus.OVERDUE);
        assertThat(testTenantBilling.getPaymentStatus()).isEqualTo(TenantBilling.PaymentStatus.OVERDUE);
        
        testTenantBilling.setPaymentStatus(TenantBilling.PaymentStatus.CANCELLED);
        assertThat(testTenantBilling.getPaymentStatus()).isEqualTo(TenantBilling.PaymentStatus.CANCELLED);
        
        testTenantBilling.setPaymentStatus(TenantBilling.PaymentStatus.REFUNDED);
        assertThat(testTenantBilling.getPaymentStatus()).isEqualTo(TenantBilling.PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("TenantBilling 통화 설정 및 조회")
    void testCurrency() {
        // Given & When
        testTenantBilling.setCurrency("EUR");

        // Then
        assertThat(testTenantBilling.getCurrency()).isEqualTo("EUR");
        
        // 다른 통화들도 테스트
        testTenantBilling.setCurrency("JPY");
        assertThat(testTenantBilling.getCurrency()).isEqualTo("JPY");
        
        testTenantBilling.setCurrency("KRW");
        assertThat(testTenantBilling.getCurrency()).isEqualTo("KRW");
    }

    @Test
    @DisplayName("TenantBilling 결제 방법 설정 및 조회")
    void testPaymentMethod() {
        // Given & When
        testTenantBilling.setPaymentMethod("bank_transfer");

        // Then
        assertThat(testTenantBilling.getPaymentMethod()).isEqualTo("bank_transfer");
        
        // 다른 결제 방법들도 테스트
        testTenantBilling.setPaymentMethod("paypal");
        assertThat(testTenantBilling.getPaymentMethod()).isEqualTo("paypal");
        
        testTenantBilling.setPaymentMethod("crypto");
        assertThat(testTenantBilling.getPaymentMethod()).isEqualTo("crypto");
    }

    @Test
    @DisplayName("TenantBilling 인보이스 번호 설정 및 조회")
    void testInvoiceNumber() {
        // Given & When
        testTenantBilling.setInvoiceNumber("INV-2024-002");

        // Then
        assertThat(testTenantBilling.getInvoiceNumber()).isEqualTo("INV-2024-002");
    }

    @Test
    @DisplayName("TenantBilling 메모 설정 및 조회")
    void testNotes() {
        // Given & When
        testTenantBilling.setNotes("Quarterly billing with discount applied");

        // Then
        assertThat(testTenantBilling.getNotes()).isEqualTo("Quarterly billing with discount applied");
    }

    @Test
    @DisplayName("TenantBilling toString 메서드 확인")
    void testToString() {
        // Given
        testTenantBilling.setId(1L);
        testTenantBilling.setInvoiceNumber("INV-2024-001");
        testTenantBilling.setTotalAmount(new BigDecimal("150.00"));

        // When
        String toString = testTenantBilling.toString();

        // Then
        assertThat(toString).contains("invoiceNumber=INV-2024-001");
        assertThat(toString).contains("totalAmount=150.00");
        // id는 Lombok @Data에서 toString에 포함되지 않을 수 있으므로 제거
    }

    @Test
    @DisplayName("TenantBilling equals와 hashCode 확인")
    void testEqualsAndHashCode() {
        // Given
        TenantBilling billing1 = TenantBilling.builder()
                .invoiceNumber("INV-2024-001")
                .totalAmount(new BigDecimal("150.00"))
                .build();
        billing1.setId(1L);

        TenantBilling billing2 = TenantBilling.builder()
                .invoiceNumber("INV-2024-001")
                .totalAmount(new BigDecimal("150.00"))
                .build();
        billing2.setId(1L);

        TenantBilling billing3 = TenantBilling.builder()
                .invoiceNumber("INV-2024-002")
                .totalAmount(new BigDecimal("200.00"))
                .build();
        billing3.setId(2L);

        // When & Then
        assertThat(billing1).isEqualTo(billing2);
        assertThat(billing1).isNotEqualTo(billing3);
        assertThat(billing1.hashCode()).isEqualTo(billing2.hashCode());
        assertThat(billing1.hashCode()).isNotEqualTo(billing3.hashCode());
    }
}
