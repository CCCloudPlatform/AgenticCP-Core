package com.agenticcp.core.domain.cloud.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Azure Subscription ID Validator 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@DisplayName("Azure Subscription ID Validator 테스트")
class AzureSubscriptionIdValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Data
    @AllArgsConstructor
    static class TestDto {
        @ValidAzureSubscriptionId
        private String subscriptionId;
    }
    
    @Data
    @AllArgsConstructor
    static class CaseSensitiveTestDto {
        @ValidAzureSubscriptionId(caseSensitive = true)
        private String subscriptionId;
    }
    
    @Data
    @AllArgsConstructor
    static class FlexibleFormatTestDto {
        @ValidAzureSubscriptionId(strictFormat = false)
        private String subscriptionId;
    }
    
    @Test
    @DisplayName("올바른 Azure Subscription ID (UUID 형식)는 검증을 통과한다")
    void validAzureSubscriptionId() {
        // given
        TestDto dto = new TestDto("12345678-1234-1234-1234-123456789012");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("소문자 UUID는 검증을 통과한다")
    void validAzureSubscriptionId_Lowercase() {
        // given
        TestDto dto = new TestDto("abcdef12-3456-7890-abcd-ef1234567890");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("대문자 UUID는 검증을 통과한다")
    void validAzureSubscriptionId_Uppercase() {
        // given
        TestDto dto = new TestDto("ABCDEF12-3456-7890-ABCD-EF1234567890");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("대소문자 혼합 UUID는 검증을 통과한다")
    void validAzureSubscriptionId_Mixed() {
        // given
        TestDto dto = new TestDto("AbCdEf12-3456-7890-aBcD-eF1234567890");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("하이픈이 없는 UUID는 검증 실패")
    void invalidAzureSubscriptionId_NoHyphens() {
        // given
        TestDto dto = new TestDto("12345678123412341234123456789012");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("올바른 Azure Subscription ID 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("잘못된 하이픈 위치의 UUID는 검증 실패")
    void invalidAzureSubscriptionId_WrongHyphenPosition() {
        // given
        TestDto dto = new TestDto("123456-78-1234-1234-1234-123456789012");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("너무 짧은 UUID는 검증 실패")
    void invalidAzureSubscriptionId_TooShort() {
        // given
        TestDto dto = new TestDto("12345678-1234-1234-1234-12345678901");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("잘못된 문자 포함 UUID는 검증 실패")
    void invalidAzureSubscriptionId_InvalidChars() {
        // given
        TestDto dto = new TestDto("12345678-1234-1234-1234-12345678901Z");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("null 값은 검증을 통과한다 (선택적 필드)")
    void nullValue_ShouldPass() {
        // given
        TestDto dto = new TestDto(null);
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("대소문자 구분 설정 - 소문자만 허용")
    void caseSensitive_LowercaseOnly() {
        // given
        CaseSensitiveTestDto dto = new CaseSensitiveTestDto("abcdef12-3456-7890-abcd-ef1234567890");
        
        // when
        Set<ConstraintViolation<CaseSensitiveTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("대소문자 구분 설정 - 대문자 포함 시 실패")
    void caseSensitive_UppercaseFails() {
        // given
        CaseSensitiveTestDto dto = new CaseSensitiveTestDto("ABCDEF12-3456-7890-ABCD-EF1234567890");
        
        // when
        Set<ConstraintViolation<CaseSensitiveTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("유연한 형식 설정 - 하이픈 없는 UUID도 허용")
    void flexibleFormat_NoHyphensAllowed() {
        // given
        FlexibleFormatTestDto dto = new FlexibleFormatTestDto("12345678123412341234123456789012");
        
        // when
        Set<ConstraintViolation<FlexibleFormatTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("유연한 형식 설정 - 하이픈 있는 UUID도 허용")
    void flexibleFormat_WithHyphensAllowed() {
        // given
        FlexibleFormatTestDto dto = new FlexibleFormatTestDto("12345678-1234-1234-1234-123456789012");
        
        // when
        Set<ConstraintViolation<FlexibleFormatTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("유연한 형식 설정 - 잘못된 길이는 여전히 실패")
    void flexibleFormat_WrongLengthFails() {
        // given
        FlexibleFormatTestDto dto = new FlexibleFormatTestDto("1234567812341234123412345678901");
        
        // when
        Set<ConstraintViolation<FlexibleFormatTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
}

