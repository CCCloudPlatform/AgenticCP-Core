package com.agenticcp.core.domain.tenant.service;

import com.agenticcp.core.domain.tenant.entity.Tenant;
import com.agenticcp.core.domain.tenant.entity.TenantIsolation;
import com.agenticcp.core.domain.tenant.repository.TenantIsolationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TenantIsolationService 단위 테스트
 * 
 * <p>테넌트 격리 수준 관리 서비스의 핵심 기능을 검증합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantIsolationService 단위 테스트")
class TenantIsolationServiceTest {

    @Mock
    private TenantIsolationRepository tenantIsolationRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TenantIsolationService tenantIsolationService;

    private Tenant testTenant;
    private TenantIsolation testIsolation;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .tenantKey("test-tenant-001")
                .tenantName("Test Tenant")
                .build();
        testTenant.setId(1L);

        testIsolation = TenantIsolation.builder()
                .tenant(testTenant)
                .isolationLevel(TenantIsolation.IsolationLevel.SHARED)
                .build();
        testIsolation.setId(1L);
    }

    @Nested
    @DisplayName("getIsolationLevel 테스트")
    class GetIsolationLevelTest {

        @Test
        @DisplayName("격리 수준이 설정된 경우 → 해당 격리 수준 반환")
        void getIsolationLevel_격리수준설정됨_해당격리수준반환() {
            // Given
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.of(testIsolation));

            // When
            TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(testTenant);

            // Then
            assertThat(level).isEqualTo(TenantIsolation.IsolationLevel.SHARED);
            verify(tenantIsolationRepository).findByTenantAndIsDeletedFalse(testTenant);
        }

        @Test
        @DisplayName("격리 수준이 설정되지 않은 경우 → null 반환")
        void getIsolationLevel_격리수준설정안됨_null반환() {
            // Given
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.empty());

            // When
            TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(testTenant);

            // Then
            assertThat(level).isNull();
            verify(tenantIsolationRepository).findByTenantAndIsDeletedFalse(testTenant);
        }

        @Test
        @DisplayName("DEDICATED 격리 수준 조회 → DEDICATED 반환")
        void getIsolationLevel_DEDICATED격리수준_DEDICATED반환() {
            // Given
            testIsolation.setIsolationLevel(TenantIsolation.IsolationLevel.DEDICATED);
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.of(testIsolation));

            // When
            TenantIsolation.IsolationLevel level = tenantIsolationService.getIsolationLevel(testTenant);

            // Then
            assertThat(level).isEqualTo(TenantIsolation.IsolationLevel.DEDICATED);
        }
    }

    @Nested
    @DisplayName("getTenantIsolation 테스트")
    class GetTenantIsolationTest {

        @Test
        @DisplayName("격리 정보가 존재하는 경우 → Optional에 격리 정보 포함")
        void getTenantIsolation_격리정보존재_Optional에격리정보포함() {
            // Given
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.of(testIsolation));

            // When
            Optional<TenantIsolation> result = tenantIsolationService.getTenantIsolation(testTenant);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getIsolationLevel()).isEqualTo(TenantIsolation.IsolationLevel.SHARED);
            verify(tenantIsolationRepository).findByTenantAndIsDeletedFalse(testTenant);
        }

        @Test
        @DisplayName("격리 정보가 없는 경우 → Optional.empty() 반환")
        void getTenantIsolation_격리정보없음_OptionalEmpty반환() {
            // Given
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.empty());

            // When
            Optional<TenantIsolation> result = tenantIsolationService.getTenantIsolation(testTenant);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("setIsolationLevel 테스트")
    class SetIsolationLevelTest {

        @Test
        @DisplayName("새로운 격리 수준 설정 → 격리 정보 생성")
        void setIsolationLevel_새로운격리수준설정_격리정보생성() {
            // Given
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.empty());
            when(tenantIsolationRepository.save(any(TenantIsolation.class)))
                    .thenReturn(testIsolation);

            // When
            TenantIsolation result = tenantIsolationService.setIsolationLevel(
                    testTenant, TenantIsolation.IsolationLevel.SHARED);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getIsolationLevel()).isEqualTo(TenantIsolation.IsolationLevel.SHARED);
            verify(tenantIsolationRepository).findByTenantAndIsDeletedFalse(testTenant);
            verify(tenantIsolationRepository).save(any(TenantIsolation.class));
        }

        @Test
        @DisplayName("기존 격리 수준 업데이트 → 격리 수준 변경")
        void setIsolationLevel_기존격리수준업데이트_격리수준변경() {
            // Given
            testIsolation.setIsolationLevel(TenantIsolation.IsolationLevel.SHARED);
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.of(testIsolation));
            when(tenantIsolationRepository.save(testIsolation))
                    .thenReturn(testIsolation);

            // When
            TenantIsolation result = tenantIsolationService.setIsolationLevel(
                    testTenant, TenantIsolation.IsolationLevel.DEDICATED);

            // Then
            assertThat(result.getIsolationLevel()).isEqualTo(TenantIsolation.IsolationLevel.DEDICATED);
            verify(tenantIsolationRepository).findByTenantAndIsDeletedFalse(testTenant);
            verify(tenantIsolationRepository).save(testIsolation);
        }

        @Test
        @DisplayName("SHARED에서 DEDICATED로 변경 → 격리 수준 변경 성공")
        void setIsolationLevel_SHARED에서DEDICATED로변경_격리수준변경성공() {
            // Given
            testIsolation.setIsolationLevel(TenantIsolation.IsolationLevel.SHARED);
            when(tenantIsolationRepository.findByTenantAndIsDeletedFalse(testTenant))
                    .thenReturn(Optional.of(testIsolation));
            when(tenantIsolationRepository.save(testIsolation))
                    .thenReturn(testIsolation);

            // When
            TenantIsolation result = tenantIsolationService.setIsolationLevel(
                    testTenant, TenantIsolation.IsolationLevel.DEDICATED);

            // Then
            assertThat(result.getIsolationLevel()).isEqualTo(TenantIsolation.IsolationLevel.DEDICATED);
        }
    }

    @Nested
    @DisplayName("saveTenantIsolation 테스트")
    class SaveTenantIsolationTest {

        @Test
        @DisplayName("격리 정보 저장 → 저장된 격리 정보 반환")
        void saveTenantIsolation_격리정보저장_저장된격리정보반환() {
            // Given
            when(tenantIsolationRepository.save(testIsolation))
                    .thenReturn(testIsolation);

            // When
            TenantIsolation result = tenantIsolationService.saveTenantIsolation(testIsolation);

            // Then
            assertThat(result).isEqualTo(testIsolation);
            verify(tenantIsolationRepository).save(testIsolation);
        }
    }
}

