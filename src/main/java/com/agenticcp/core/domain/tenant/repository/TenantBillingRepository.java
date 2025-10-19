package com.agenticcp.core.domain.tenant.repository;

import com.agenticcp.core.common.repository.TenantAwareRepository;
import com.agenticcp.core.domain.tenant.entity.TenantBilling;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 테넌트 과금 Repository
 * 테넌트별 과금 정보를 관리합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Repository
public interface TenantBillingRepository extends TenantAwareRepository<TenantBilling, Long> {

    /**
     * 결제 상태별 과금 정보 조회
     * 
     * @param paymentStatus 결제 상태
     * @return 과금 정보 목록
     */
    List<TenantBilling> findByPaymentStatus(TenantBilling.PaymentStatus paymentStatus);

    /**
     * 과금 주기별 과금 정보 조회
     * 
     * @param billingCycle 과금 주기
     * @return 과금 정보 목록
     */
    List<TenantBilling> findByBillingCycle(TenantBilling.BillingCycle billingCycle);

    /**
     * 특정 기간의 과금 정보 조회
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 과금 정보 목록
     */
    @Query("SELECT tb FROM TenantBilling tb WHERE tb.billingPeriodStart >= :startDate AND tb.billingPeriodEnd <= :endDate")
    List<TenantBilling> findByBillingPeriod(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * 연체된 과금 정보 조회
     * 
     * @param currentDate 현재 날짜
     * @return 연체된 과금 정보 목록
     */
    @Query("SELECT tb FROM TenantBilling tb WHERE tb.dueDate < :currentDate AND tb.paymentStatus = 'PENDING'")
    List<TenantBilling> findOverdueBilling(@Param("currentDate") LocalDateTime currentDate);

    /**
     * 특정 금액 이상의 과금 정보 조회
     * 
     * @param minAmount 최소 금액
     * @return 과금 정보 목록
     */
    @Query("SELECT tb FROM TenantBilling tb WHERE tb.totalAmount >= :minAmount")
    List<TenantBilling> findByTotalAmountGreaterThanEqual(@Param("minAmount") BigDecimal minAmount);

    /**
     * 인보이스 번호로 과금 정보 조회
     * 
     * @param invoiceNumber 인보이스 번호
     * @return 과금 정보
     */
    Optional<TenantBilling> findByInvoiceNumber(String invoiceNumber);

    /**
     * 결제 방법별 과금 정보 조회
     * 
     * @param paymentMethod 결제 방법
     * @return 과금 정보 목록
     */
    List<TenantBilling> findByPaymentMethod(String paymentMethod);

    /**
     * 통화별 과금 정보 조회
     * 
     * @param currency 통화
     * @return 과금 정보 목록
     */
    List<TenantBilling> findByCurrency(String currency);

    /**
     * 특정 기간의 총 과금 금액 조회
     * 
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 총 과금 금액
     */
    @Query("SELECT SUM(tb.totalAmount) FROM TenantBilling tb WHERE tb.billingPeriodStart >= :startDate AND tb.billingPeriodEnd <= :endDate")
    BigDecimal getTotalBillingAmount(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);

    /**
     * 결제 상태별 과금 금액 합계 조회
     * 
     * @param paymentStatus 결제 상태
     * @return 과금 금액 합계
     */
    @Query("SELECT SUM(tb.totalAmount) FROM TenantBilling tb WHERE tb.paymentStatus = :paymentStatus")
    BigDecimal getTotalAmountByPaymentStatus(@Param("paymentStatus") TenantBilling.PaymentStatus paymentStatus);
}
