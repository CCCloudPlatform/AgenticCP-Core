package com.agenticcp.core.common.logging.masking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 민감 정보 마스킹 처리 애노테이션
 * 
 * 이 애노테이션이 적용된 필드는 감사 로그 저장 시 자동으로 마스킹 처리됩니다.
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {
    /**
     * 마스킹 처리 유형
     * @return MaskingType
     */
    MaskingType type() default MaskingType.DEFAULT;
}
