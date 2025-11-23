package com.agenticcp.core.common.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 감사 액션명을 생성하는 유틸리티입니다.
 * {@link com.agenticcp.core.common.audit.AuditAspect}에서 컨트롤러 메서드명을 기반으로
 * 표준화된 액션 문자열(VERB_NOUN)로 변환할 때 사용합니다.
 *
 * 예) <em>getTenantPolicies</em> → <em>GET_TENANT_POLICIES</em>
 *
 * @author AgenticCP Team
 * @since 2025-10-01
 * @version 1.0.0
 */
public final class AuditActionGenerator {

    private AuditActionGenerator() {}

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?<=[a-z])(?=[A-Z])");

    /**
     * 메서드명을 분석해 감사 액션 문자열을 생성합니다.
     * 단어가 하나뿐이면 해당 단어를 대문자로 반환하고,
     * 그 이상이면 첫 단어를 동사(VERB), 나머지를 명사 블록(NOUN_PART)으로 조합합니다.
     *
     * @param methodName 감사 대상 메서드명
     * @return 표준화된 액션명, 입력이 비거나 null이면 {@code null}
     */
    public static String generateActionName(String methodName) {
        if (methodName == null || methodName.trim().isEmpty()) {
            return null;
        }

        String[] words = CAMEL_CASE_PATTERN.split(methodName);

        if (words.length < 2) {
            return methodName.toUpperCase();
        }

        String verb = words[0].toUpperCase();
        String nounPart = Arrays.stream(words)
                .skip(1)
                .map(String::toUpperCase)
                .collect(Collectors.joining("_"));

        return verb + "_" + nounPart;
    }
}
