package com.agenticcp.core.domain.security.service;

import com.agenticcp.core.domain.security.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 정책 JSON 파싱 서비스
 * 
 * <p>정책의 JSON 데이터를 파싱하여 DTO 객체로 변환하는 서비스입니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyJsonParser {
    
    private final ObjectMapper objectMapper;
    
    /**
     * 정책 규칙 JSON을 PolicyRules 객체로 파싱
     * 
     * @param rulesJson 정책 규칙 JSON
     * @return PolicyRules 객체
     */
    public PolicyRules parsePolicyRules(String rulesJson) {
        if (rulesJson == null || rulesJson.trim().isEmpty()) {
            log.debug("정책 규칙 JSON이 비어있습니다");
            return PolicyRules.builder().build();
        }
        
        try {
            PolicyRules rules = objectMapper.readValue(rulesJson, PolicyRules.class);
            log.debug("정책 규칙 파싱 완료: ruleCount={}", rules.getRuleCount());
            return rules;
        } catch (JsonProcessingException e) {
            log.error("정책 규칙 JSON 파싱 실패: {}", e.getMessage(), e);
            return PolicyRules.builder().build();
        }
    }
    
    /**
     * 정책 조건 JSON을 PolicyConditions 객체로 파싱
     * 
     * @param conditionsJson 정책 조건 JSON
     * @return PolicyConditions 객체
     */
    public PolicyConditions parsePolicyConditions(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
            log.debug("정책 조건 JSON이 비어있습니다");
            return PolicyConditions.builder().build();
        }
        
        try {
            PolicyConditions conditions = objectMapper.readValue(conditionsJson, PolicyConditions.class);
            log.debug("정책 조건 파싱 완료: conditionCount={}", conditions.getActiveConditionCount());
            return conditions;
        } catch (JsonProcessingException e) {
            log.error("정책 조건 JSON 파싱 실패: {}", e.getMessage(), e);
            return PolicyConditions.builder().build();
        }
    }
    
    /**
     * 정책 액션 JSON을 PolicyAction 목록으로 파싱
     * 
     * @param actionsJson 정책 액션 JSON
     * @return PolicyAction 목록
     */
    public List<PolicyAction> parsePolicyActions(String actionsJson) {
        if (actionsJson == null || actionsJson.trim().isEmpty()) {
            log.debug("정책 액션 JSON이 비어있습니다");
            return List.of();
        }
        
        try {
            List<PolicyAction> actions = objectMapper.readValue(actionsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, PolicyAction.class));
            log.debug("정책 액션 파싱 완료: actionCount={}", actions.size());
            return actions;
        } catch (JsonProcessingException e) {
            log.error("정책 액션 JSON 파싱 실패: {}", e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * PolicyRules 객체를 JSON으로 변환
     * 
     * @param rules PolicyRules 객체
     * @return JSON 문자열
     */
    public String toJson(PolicyRules rules) {
        if (rules == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (JsonProcessingException e) {
            log.error("PolicyRules JSON 변환 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * PolicyConditions 객체를 JSON으로 변환
     * 
     * @param conditions PolicyConditions 객체
     * @return JSON 문자열
     */
    public String toJson(PolicyConditions conditions) {
        if (conditions == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(conditions);
        } catch (JsonProcessingException e) {
            log.error("PolicyConditions JSON 변환 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * PolicyAction 목록을 JSON으로 변환
     * 
     * @param actions PolicyAction 목록
     * @return JSON 문자열
     */
    public String toJson(List<PolicyAction> actions) {
        if (actions == null) {
            return null;
        }
        
        try {
            return objectMapper.writeValueAsString(actions);
        } catch (JsonProcessingException e) {
            log.error("PolicyAction 목록 JSON 변환 실패: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * JSON 유효성 검증
     * 
     * @param json JSON 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        
        try {
            objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            log.debug("JSON 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 정책 규칙 JSON 유효성 검증
     * 
     * @param rulesJson 정책 규칙 JSON
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidPolicyRulesJson(String rulesJson) {
        if (!isValidJson(rulesJson)) {
            return false;
        }
        
        try {
            PolicyRules rules = objectMapper.readValue(rulesJson, PolicyRules.class);
            return rules != null;
        } catch (JsonProcessingException e) {
            log.debug("정책 규칙 JSON 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 정책 조건 JSON 유효성 검증
     * 
     * @param conditionsJson 정책 조건 JSON
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidPolicyConditionsJson(String conditionsJson) {
        if (!isValidJson(conditionsJson)) {
            return false;
        }
        
        try {
            PolicyConditions conditions = objectMapper.readValue(conditionsJson, PolicyConditions.class);
            return conditions != null;
        } catch (JsonProcessingException e) {
            log.debug("정책 조건 JSON 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 정책 액션 JSON 유효성 검증
     * 
     * @param actionsJson 정책 액션 JSON
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean isValidPolicyActionsJson(String actionsJson) {
        if (!isValidJson(actionsJson)) {
            return false;
        }
        
        try {
            List<PolicyAction> actions = objectMapper.readValue(actionsJson, 
                objectMapper.getTypeFactory().constructCollectionType(List.class, PolicyAction.class));
            return actions != null;
        } catch (JsonProcessingException e) {
            log.debug("정책 액션 JSON 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}
