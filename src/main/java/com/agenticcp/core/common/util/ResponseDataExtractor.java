package com.agenticcp.core.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 응답 데이터 추출 유틸리티 클래스
 * 
 * Controller의 반환 객체에서 실제 로깅할 데이터를 추출하는 유틸리티입니다.
 * Spring Framework의 특정 클래스들을 처리하는 책임을 담당합니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
public final class ResponseDataExtractor {

    private ResponseDataExtractor() {
        throw new UnsupportedOperationException("Utility class");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extract(Object result) {
        if (result == null) {
            return new HashMap<>();
        }

        if (result instanceof ResponseEntity<?> responseEntity) {
            Map<String, Object> responseData = new HashMap<>();
            Object body = responseEntity.getBody();
            if (body != null) {
                if (body instanceof Map) {

                    responseData.putAll((Map<String, Object>) body);
                } else {
                    responseData.put("responseBody", body);
                }
            }
            responseData.put("statusCode", responseEntity.getStatusCode().value());
            return responseData;
        }

        if (result instanceof Map) {
            // 이 메서드의 맥락상 Controller가 반환하는 Map은
            // JSON으로 변환 가능한 Map<String, Object> 형태임을 신뢰한
            return new HashMap<>((Map<String, Object>) result);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("result", result);
        return responseData;
    }
}
