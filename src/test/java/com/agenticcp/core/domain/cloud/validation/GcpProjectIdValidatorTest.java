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
 * GCP Project ID Validator 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2025-10-24
 */
@DisplayName("GCP Project ID Validator 테스트")
class GcpProjectIdValidatorTest {
    
    private Validator validator;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }
    
    @Data
    @AllArgsConstructor
    static class TestDto {
        @ValidGcpProjectId
        private String projectId;
    }
    
    @Data
    @AllArgsConstructor
    static class CustomLengthTestDto {
        @ValidGcpProjectId(minLength = 8, maxLength = 20)
        private String projectId;
    }
    
    @Test
    @DisplayName("올바른 GCP Project ID는 검증을 통과한다")
    void validGcpProjectId() {
        // given
        TestDto dto = new TestDto("my-gcp-project");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("올바른 GCP Project ID (숫자 포함)는 검증을 통과한다")
    void validGcpProjectId_WithNumbers() {
        // given
        TestDto dto = new TestDto("test-project-123");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("6자 Project ID는 검증을 통과한다")
    void validGcpProjectId_MinLength() {
        // given
        TestDto dto = new TestDto("test12");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("30자 Project ID는 검증을 통과한다")
    void validGcpProjectId_MaxLength() {
        // given
        TestDto dto = new TestDto("abcdefghijklmnopqrstuvwxyz12");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("5자 이하 Project ID는 검증 실패")
    void invalidGcpProjectId_TooShort() {
        // given
        TestDto dto = new TestDto("test1");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .contains("올바른 GCP Project ID 형식이 아닙니다");
    }
    
    @Test
    @DisplayName("31자 이상 Project ID는 검증 실패")
    void invalidGcpProjectId_TooLong() {
        // given (31자: a + bcdefghijklmnopqrstuvwxyz1234 = 1 + 30 = 31)
        TestDto dto = new TestDto("abcdefghijklmnopqrstuvwxyz12345");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("대문자 포함 Project ID는 검증 실패")
    void invalidGcpProjectId_WithUpperCase() {
        // given
        TestDto dto = new TestDto("My-GCP-Project");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("숫자로 시작하는 Project ID는 검증 실패")
    void invalidGcpProjectId_StartsWithNumber() {
        // given
        TestDto dto = new TestDto("1test-project");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("하이픈으로 끝나는 Project ID는 검증 실패")
    void invalidGcpProjectId_EndsWithHyphen() {
        // given
        TestDto dto = new TestDto("test-project-");
        
        // when
        Set<ConstraintViolation<TestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("특수문자 포함 Project ID는 검증 실패")
    void invalidGcpProjectId_WithSpecialChars() {
        // given
        TestDto dto = new TestDto("test_project");
        
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
    @DisplayName("커스텀 길이 설정 - 올바른 길이의 Project ID는 검증을 통과한다")
    void customLength_ValidProjectId() {
        // given
        CustomLengthTestDto dto = new CustomLengthTestDto("my-project-123");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 너무 짧은 Project ID는 검증 실패")
    void customLength_ProjectIdTooShort() {
        // given (7자)
        CustomLengthTestDto dto = new CustomLengthTestDto("test123");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 너무 긴 Project ID는 검증 실패")
    void customLength_ProjectIdTooLong() {
        // given (21자)
        CustomLengthTestDto dto = new CustomLengthTestDto("very-long-project-name-123");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).hasSize(1);
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 경계값 테스트 (최소 길이)")
    void customLength_MinimumLength() {
        // given (8자)
        CustomLengthTestDto dto = new CustomLengthTestDto("test1234");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
    
    @Test
    @DisplayName("커스텀 길이 설정 - 경계값 테스트 (최대 길이)")
    void customLength_MaximumLength() {
        // given (20자)
        CustomLengthTestDto dto = new CustomLengthTestDto("very-long-project-12");
        
        // when
        Set<ConstraintViolation<CustomLengthTestDto>> violations = validator.validate(dto);
        
        // then
        assertThat(violations).isEmpty();
    }
}

