package com.agenticcp.core.common.context;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 감사 로그 변경 추적 컨텍스트
 * 
 * ThreadLocal을 사용하여 변경 전 값을 저장합니다.
 * AOP가 자동으로 이 값을 읽어서 감사 로그에 포함시킵니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Slf4j
public class AuditChangeContext {

    private static final ThreadLocal<ChangeData> changeContext = new ThreadLocal<>();

    /**
     * 변경 전 값 설정
     */
    public static void setOldValue(Map<String, Object> oldValue) {
        ChangeData data = getOrCreate();
        data.oldValue = oldValue;
        changeContext.set(data);
    }

    /**
     * 변경 대상 리소스 ID 설정
     */
    public static void setTargetResourceId(String targetResourceId) {
        ChangeData data = getOrCreate();
        data.targetResourceId = targetResourceId;
        changeContext.set(data);
    }

    /**
     * 변경 데이터 설정 (한번에)
     */
    public static void setChangeData(Map<String, Object> oldValue, String targetResourceId) {
        ChangeData data = new ChangeData();
        data.oldValue = oldValue;
        data.targetResourceId = targetResourceId;
        changeContext.set(data);
    }

    /**
     * 변경 전 값 조회
     */
    public static Map<String, Object> getOldValue() {
        ChangeData data = changeContext.get();
        return data != null ? data.oldValue : null;
    }

    /**
     * 변경 대상 리소스 ID 조회
     */
    public static String getTargetResourceId() {
        ChangeData data = changeContext.get();
        return data != null ? data.targetResourceId : null;
    }

    /**
     * 컨텍스트 정리
     */
    public static void clear() {
        changeContext.remove();
    }

    private static ChangeData getOrCreate() {
        ChangeData data = changeContext.get();
        if (data == null) {
            data = new ChangeData();
        }
        return data;
    }

    private static class ChangeData {
        Map<String, Object> oldValue;
        String targetResourceId;
    }
}

