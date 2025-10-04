package com.agenticcp.core.common.util;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AuditActionGenerator {

    private AuditActionGenerator() {}

    private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("(?<=[a-z])(?=[A-Z])");

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
