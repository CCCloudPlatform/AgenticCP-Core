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
 * AWS Account ID Validator 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@DisplayName("AWS Account ID Validator 테스트")
class AwsAccountIdValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Data
    @AllArgsConstructor
    static class TestDto {
        @ValidAwsAccountId
        private String accountId;
    }
    
    @Data
    @AllArgsConstructor
    static class CustomLengthTestDto {
        @ValidAwsAccountId(length = 10)
        private String accountId;
    }
    
    @Data
    @AllArgsConstructor
    static class NonNumericTestDto {
        @ValidAwsAccountId(length = 8, numericOnly = false)
        private String accountId;
    }
    
    @Test
    @DisplayName("올바른 AWS Account ID (12자리 숫자)는 검증을 통과한다")
    void validAwsAccountId() {
        // given
        TestDto dto = new TestDto("123456789012");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("11자리 숫자는 검증 실패")
    void invalidAwsAccountId_TooShort() {
        // given
        TestDto dto = new TestDto("12345678901");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("올바른 AWS Account ID 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("13자리 숫자는 검증 실패")
    void invalidAwsAccountId_TooLong() {
        // given
        TestDto dto = new TestDto("1234567890123");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("문자가 포함된 경우 검증 실패")
    void invalidAwsAccountId_WithLetters() {
        // given
        TestDto dto = new TestDto("12345678901a");
        
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
    @DisplayName("빈 문자열은 검증을 통과한다 (선택적 필드)")
    void emptyString_ShouldPass() {
        // given
        TestDto dto = new TestDto("");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 올바른 길이의 Account ID는 검증을 통과한다")
    void customLength_ValidAccountId() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("1234567890");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 잘못된 길이의 Account ID는 검증 실패")
    void customLength_InvalidLength() {
        // given (11자리)
        CustomLengthTestDto dto = new CustomLengthTestDto("12345678901");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("비숫자 허용 설정 - 문자 포함 Account ID는 검증을 통과한다")
    void nonNumeric_ValidAccountId() {
        // given
        NonNumericTestDto dto = new NonNumericTestDto("abc12345");
        
        // when
        Set<ConstraintViolation<NonNumericTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("비숫자 허용 설정 - 특수문자 포함 Account ID는 검증을 통과한다")
    void nonNumeric_WithSpecialChars() {
        // given
        NonNumericTestDto dto = new NonNumericTestDto("test-123");
        
        // when
        Set<ConstraintViolation<NonNumericTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("비숫자 허용 설정 - 잘못된 길이는 여전히 검증 실패")
    void nonNumeric_InvalidLength() {
        // given (9자리)
        NonNumericTestDto dto = new NonNumericTestDto("test12345");
        
        // when
        Set<ConstraintViolation<NonNumericTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
}

