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
 * IAM Role ARN Validator 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@DisplayName("IAM Role ARN Validator 테스트")
class IamRoleArnValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Data
    @AllArgsConstructor
    static class TestDto {
        @ValidIamRoleArn
        private String roleArn;
    }
    
    @Data
    @AllArgsConstructor
    static class CustomLengthTestDto {
        @ValidIamRoleArn(minRoleNameLength = 3, maxRoleNameLength = 16)
        private String roleArn;
    }
    
    @Test
    @DisplayName("올바른 IAM Role ARN은 검증을 통과한다")
    void validIamRoleArn() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::123456789012:role/MyRole");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("올바른 IAM Role ARN (경로 포함)은 검증을 통과한다")
    void validIamRoleArn_WithPath() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::123456789012:role/path/to/MyRole");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("올바른 IAM Role ARN (하이픈 포함 이름)은 검증을 통과한다")
    void validIamRoleArn_WithHyphens() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::123456789012:role/My-Test-Role-123");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("잘못된 ARN 형식 (arn 접두사 없음)은 검증 실패")
    void invalidIamRoleArn_NoArnPrefix() {
        // given
        TestDto dto = new TestDto("aws:iam::123456789012:role/MyRole");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("올바른 AWS IAM Role ARN 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("잘못된 서비스 (iam이 아님)는 검증 실패")
    void invalidIamRoleArn_WrongService() {
        // given
        TestDto dto = new TestDto("arn:aws:s3::123456789012:role/MyRole");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("잘못된 계정 ID (12자리가 아님)는 검증 실패")
    void invalidIamRoleArn_InvalidAccountId() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::12345:role/MyRole");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("잘못된 리소스 타입 (role이 아님)은 검증 실패")
    void invalidIamRoleArn_WrongResourceType() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::123456789012:user/MyUser");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("Role 이름이 없으면 검증 실패")
    void invalidIamRoleArn_NoRoleName() {
        // given
        TestDto dto = new TestDto("arn:aws:iam::123456789012:role/");
        
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
    @DisplayName("커스텀 길이 설정 - 올바른 길이의 Role 이름은 검증을 통과한다")
    void customLength_ValidRoleName() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("arn:aws:iam::123456789012:role/MyRole");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 너무 짧은 Role 이름은 검증 실패")
    void customLength_RoleNameTooShort() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("arn:aws:iam::123456789012:role/AB");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 너무 긴 Role 이름은 검증 실패")
    void customLength_RoleNameTooLong() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("arn:aws:iam::123456789012:role/VeryLongRoleName123");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 경계값 테스트 (최소 길이)")
    void customLength_MinimumLength() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("arn:aws:iam::123456789012:role/ABC");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 경계값 테스트 (최대 길이)")
    void customLength_MaximumLength() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("arn:aws:iam::123456789012:role/ABCDEFGHIJKLMNOP");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
}

