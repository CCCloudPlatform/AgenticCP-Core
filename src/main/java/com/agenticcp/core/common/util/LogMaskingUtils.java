package com.agenticcp.core.common.util;

/**
 * 로그 마스킹 유틸리티
 * 민감한 문자열(tenantKey, permissionKey 등)을 로그에 출력할 때 일부만 노출합니다.
 */
public final class LogMaskingUtils {

	private LogMaskingUtils() {}

	/**
	 * 일반 문자열 마스킹. 앞/뒤 일부만 노출하고 가운데를 *로 마스킹합니다.
	 * 예) abcdefg -> ab***fg (revealStart=2, revealEnd=2)
	 */
	public static String mask(String value, int revealStart, int revealEnd) {
		if (value == null || value.isEmpty()) {
			return "";
		}
		int length = value.length();
		if (revealStart + revealEnd >= length) {
			return repeat('*', Math.max(3, length));
		}
		String start = value.substring(0, Math.max(0, revealStart));
		String end = value.substring(length - Math.max(0, revealEnd));
		int maskLen = Math.max(3, length - start.length() - end.length());
		return start + repeat('*', maskLen) + end;
	}

	public static String maskTenantKey(String tenantKey) {
		return mask(tenantKey, 2, 2);
	}

	public static String maskPermissionKey(String permissionKey) {
		return mask(permissionKey, 2, 2);
	}

	public static String maskUserIdentifier(String userIdOrEmail) {
		return mask(userIdOrEmail, 2, 2);
	}

	public static String maskNullable(Object value, int revealStart, int revealEnd) {
		return value == null ? "" : mask(String.valueOf(value), revealStart, revealEnd);
	}

	/**
	 * IP 주소 마스킹
	 * IPv4: a.b.c.d -> a.b.c.***
	 * IPv6: a:b:c:d:e:f:g:h -> a:b:c:d:****
	 */
	public static String maskIpAddress(String ip) {
		if (ip == null || ip.isEmpty()) {
			return ip;
		}
		
		if (ip.contains(".")) { // IPv4
			String[] parts = ip.split("\\.");
			if (parts.length == 4) {
				return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
			}
		} else if (ip.contains(":")) { // IPv6
			String[] parts = ip.split(":");
			if (parts.length >= 4) {
				return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3] + ":****";
			}
		}
		
		return ip;
	}

	/**
	 * User-Agent 마스킹
	 * 길이가 maxPrefixLen 이하면 원문, 그 이상이면 prefix + "..."
	 */
	public static String previewUserAgent(String userAgent, int maxPrefixLen) {
		if (userAgent == null || userAgent.length() <= maxPrefixLen) {
			return userAgent;
		}
		return userAgent.substring(0, maxPrefixLen) + "...";
	}

	private static String repeat(char c, int count) {
		StringBuilder sb = new StringBuilder(Math.max(0, count));
		for (int i = 0; i < count; i++) {
			sb.append(c);
		}
		return sb.toString();
	}
}


