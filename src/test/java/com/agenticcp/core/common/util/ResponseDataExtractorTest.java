package com.agenticcp.core.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ResponseDataExtractor 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@DisplayName("응답 데이터 추출기 테스트")
class ResponseDataExtractorTest {

    @Test
    @DisplayName("null 반환값은 빈 맵을 반환한다")
    void extract_shouldReturnEmptyMap_whenResultIsNull() {
        // when
        Object result = ResponseDataExtractor.extract(null);
        
        // then
        assertThat(result).isInstanceOf(Map.class);
        assertThat((Map<?, ?>) result).isEmpty();
    }

    @Test
    @DisplayName("ResponseEntity를 정상적으로 추출한다")
    void extract_shouldExtractResponseEntity_whenValidResponseEntity() {
        // given
        String responseBody = "test response";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(responseBody);
        
        // when
        Object result = ResponseDataExtractor.extract(responseEntity);
        
        // then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertThat(resultMap).containsEntry("responseBody", responseBody);
        assertThat(resultMap).containsEntry("statusCode", 200);
    }

    @Test
    @DisplayName("ResponseEntity의 body가 null인 경우를 처리한다")
    void extract_shouldHandleNullBody_whenResponseEntityBodyIsNull() {
        // given
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        
        // when
        Object result = ResponseDataExtractor.extract(responseEntity);
        
        // then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertThat(resultMap).doesNotContainKey("responseBody");
        assertThat(resultMap).containsEntry("statusCode", 204);
    }

    @Test
    @DisplayName("ResponseEntity의 Map body를 펼쳐서 정상적으로 추출한다")
    void extract_shouldExtractAndFlattenMapBody_whenResponseEntityHasMapBody() {
        // given
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("id", 1L);
        responseBody.put("name", "test");

        ResponseEntity<Map<String, Object>> responseEntity = ResponseEntity.ok(responseBody);

        // when
        Map<String, Object> resultMap = ResponseDataExtractor.extract(responseEntity);

        // then
        assertThat(resultMap).containsEntry("id", 1L);
        assertThat(resultMap).containsEntry("name", "test");
        assertThat(resultMap).containsEntry("statusCode", 200);
        assertThat(resultMap).doesNotContainKey("responseBody");
    }

    @Test
    @DisplayName("ResponseEntity의 에러 상태코드를 정상적으로 추출한다")
    void extract_shouldExtractErrorStatusCode_whenResponseEntityHasErrorStatus() {
        // given
        String errorMessage = "Not Found";
        ResponseEntity<String> responseEntity = ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        
        // when
        Object result = ResponseDataExtractor.extract(responseEntity);
        
        // then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertThat(resultMap).containsEntry("responseBody", errorMessage);
        assertThat(resultMap).containsEntry("statusCode", 404);
    }

    @Test
    @DisplayName("일반 객체는 'result' 키로 감싸진 Map으로 반환된다")
    void extract_shouldReturnOriginal_whenNotResponseEntity() {
        // given
        String simpleObject = "simple response";

        // when
        Map<String, Object> result = ResponseDataExtractor.extract(simpleObject);

        // then
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("result", simpleObject);

        assertThat(result).isEqualTo(expectedMap);
    }

    @Test
    @DisplayName("Map 객체는 원본을 반환한다")
    void extract_shouldReturnOriginal_whenMapObject() {
        // given
        Map<String, Object> mapObject = new HashMap<>();
        mapObject.put("key", "value");
        
        // when
        Object result = ResponseDataExtractor.extract(mapObject);
        
        // then
        assertThat(result).isEqualTo(mapObject);
        assertThat(result).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("복잡한 객체는 'result' 키로 감싸진 Map으로 반환된다")
    void extract_shouldReturnWrappedMap_whenComplexObject() {
        // given
        TestObject testObject = new TestObject(1L, "test");

        // when
        Map<String, Object> resultMap = ResponseDataExtractor.extract(testObject);

        // then
        Map<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("result", testObject);

        assertThat(resultMap).isEqualTo(expectedMap);
        assertThat(resultMap.get("result")).isEqualTo(testObject);
        assertThat(resultMap.get("result")).isInstanceOf(TestObject.class);
    }

    @Test
    @DisplayName("ResponseEntity의 커스텀 상태코드를 정상적으로 추출한다")
    void extract_shouldExtractCustomStatusCode_whenResponseEntityHasCustomStatus() {
        // given
        ResponseEntity<String> responseEntity = ResponseEntity.status(418).body("I'm a teapot");
        
        // when
        Object result = ResponseDataExtractor.extract(responseEntity);
        
        // then
        assertThat(result).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        
        assertThat(resultMap).containsEntry("responseBody", "I'm a teapot");
        assertThat(resultMap).containsEntry("statusCode", 418);
    }

    // 테스트용 클래스
    private static class TestObject {
        private final Long id;
        private final String name;
        
        public TestObject(Long id, String name) {
            this.id = id;
            this.name = name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            TestObject that = (TestObject) obj;
            return id.equals(that.id) && name.equals(that.name);
        }
        
        @Override
        public int hashCode() {
            return id.hashCode() + name.hashCode();
        }
    }
}
