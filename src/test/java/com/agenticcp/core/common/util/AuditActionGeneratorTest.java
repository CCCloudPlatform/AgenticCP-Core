package com.agenticcp.core.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AuditActionGenerator 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("감사 액션 생성기 테스트")
class AuditActionGeneratorTest {

    @Test
    @DisplayName("메서드명이 null이면 null을 반환한다")
    void generateActionName_shouldReturnNull_whenMethodNameIsNull() {
        // when
        String result = AuditActionGenerator.generateActionName(null);
        
        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("메서드명이 빈 문자열이면 null을 반환한다")
    void generateActionName_shouldReturnNull_whenMethodNameIsEmpty() {
        // when
        String result = AuditActionGenerator.generateActionName("");
        
        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("메서드명이 공백이면 null을 반환한다")
    void generateActionName_shouldReturnNull_whenMethodNameIsBlank() {
        // when
        String result = AuditActionGenerator.generateActionName("   ");
        
        // then
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "get, GET",
        "post, POST", 
        "delete, DELETE",
        "update, UPDATE",
        "find, FIND"
    })
    @DisplayName("단일 단어 메서드명은 대문자로 변환한다")
    void generateActionName_shouldConvertToUpperCase_whenSingleWord(String input, String expected) {
        // when
        String result = AuditActionGenerator.generateActionName(input);
        
        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "getUser, GET_USER",
        "createUser, CREATE_USER",
        "updateUserProfile, UPDATE_USER_PROFILE",
        "deleteUserAccount, DELETE_USER_ACCOUNT",
        "findUserById, FIND_USER_BY_ID"
    })
    @DisplayName("카멜케이스 메서드명을 정상적으로 변환한다")
    void generateActionName_shouldConvertCamelCase_whenValidMethodName(String input, String expected) {
        // when
        String result = AuditActionGenerator.generateActionName(input);
        
        // then
        assertThat(result).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "getUserById, GET_USER_BY_ID",
        "createUserProfile, CREATE_USER_PROFILE",
        "updateUserAccountInfo, UPDATE_USER_ACCOUNT_INFO",
        "deleteUserSessionData, DELETE_USER_SESSION_DATA"
    })
    @DisplayName("복잡한 카멜케이스 메서드명을 정상적으로 변환한다")
    void generateActionName_shouldConvertComplexCamelCase_whenValidMethodName(String input, String expected) {
        // when
        String result = AuditActionGenerator.generateActionName(input);
        
        // then
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("대문자로 시작하는 메서드명을 정상 처리한다")
    void generateActionName_shouldHandleUpperCaseStart_whenMethodNameStartsWithUpperCase() {
        // when
        String result = AuditActionGenerator.generateActionName("GetUser");
        
        // then
        assertThat(result).isEqualTo("GET_USER");
    }

    @Test
    @DisplayName("숫자가 포함된 메서드명을 정상 처리한다")
    void generateActionName_shouldHandleNumbers_whenMethodNameContainsNumbers() {
        // when
        String result = AuditActionGenerator.generateActionName("getUser2FA");
        
        // then
        assertThat(result).isEqualTo("GET_USER2FA");
    }

    @Test
    @DisplayName("연속된 대문자를 정상 처리한다")
    void generateActionName_shouldHandleConsecutiveUpperCase_whenMethodNameHasConsecutiveUpperCase() {
        // when
        String result = AuditActionGenerator.generateActionName("getXMLData");
        
        // then
        assertThat(result).isEqualTo("GET_XMLDATA");
    }
}
