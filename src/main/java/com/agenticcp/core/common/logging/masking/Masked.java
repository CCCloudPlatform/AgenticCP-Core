package com.agenticcp.core.common.logging.masking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 민감 정보 마스킹을 표시하는 필드 애노테이션입니다.
 * 
 * @author AgenticCP Team
 * @since 2025-10-01
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
