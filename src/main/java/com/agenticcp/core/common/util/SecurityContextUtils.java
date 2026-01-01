package com.agenticcp.core.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext 유틸리티
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 */
public class SecurityContextUtils {

    /**
     * 현재 인증된 사용자명 조회
     * 
     * @return 사용자명, 없으면 null
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * 현재 인증된 사용자명 조회 (null 체크 포함)
     * 
     * @return 사용자명
     * @throws IllegalStateException 인증 정보가 없는 경우
     */
    public static String getCurrentUsernameOrThrow() {
        String username = getCurrentUsername();
        if (username == null) {
            throw new IllegalStateException("현재 인증된 사용자 정보가 없습니다");
        }
        return username;
    }
}

