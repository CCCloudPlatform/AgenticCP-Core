package com.agenticcp.core.domain.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * 시간 조건 데이터 전송 객체
 * 
 * <p>정책의 시간 관련 조건을 정의합니다.</p>
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeConditions {
    
    /**
     * 허용 시간 범위 목록
     * 예: 09:00-18:00 (업무시간)
     */
    private List<TimeRange> allowedTimeRanges;
    
    /**
     * 금지 시간 범위 목록
     * 예: 22:00-06:00 (야간 시간)
     */
    private List<TimeRange> deniedTimeRanges;
    
    /**
     * 허용 요일 목록
     * MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
     */
    private List<String> allowedDaysOfWeek;
    
    /**
     * 금지 요일 목록
     */
    private List<String> deniedDaysOfWeek;
    
    /**
     * 허용 월 목록 (1-12)
     */
    private List<Integer> allowedMonths;
    
    /**
     * 금지 월 목록 (1-12)
     */
    private List<Integer> deniedMonths;
    
    /**
     * 허용 날짜 목록 (MM-DD 형식)
     */
    private List<String> allowedDates;
    
    /**
     * 금지 날짜 목록 (MM-DD 형식)
     */
    private List<String> deniedDates;
    
    /**
     * 시간대 설정
     */
    private String timeZone;
    
    /**
     * 시간 범위 데이터 전송 객체
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeRange {
        private LocalTime startTime;
        private LocalTime endTime;
        private String description;
        
        /**
         * 시간 범위가 유효한지 확인
         * 
         * @return 유효하면 true, 그렇지 않으면 false
         */
        public boolean isValid() {
            return startTime != null && endTime != null && !startTime.isAfter(endTime);
        }
        
        /**
         * 특정 시간이 범위에 포함되는지 확인
         * 
         * @param time 확인할 시간
         * @return 포함되면 true, 그렇지 않으면 false
         */
        public boolean contains(LocalTime time) {
            if (!isValid() || time == null) {
                return false;
            }
            
            // 자정을 넘나드는 경우 처리
            if (startTime.isAfter(endTime)) {
                return !time.isBefore(startTime) || !time.isAfter(endTime);
            } else {
                return !time.isBefore(startTime) && !time.isAfter(endTime);
            }
        }
    }
    
    /**
     * 현재 시간이 허용 시간 범위에 포함되는지 확인
     * 
     * @return 포함되면 true, 그렇지 않으면 false
     */
    public boolean isCurrentTimeAllowed() {
        LocalTime now = LocalTime.now();
        
        // 금지 시간 범위 확인
        if (deniedTimeRanges != null) {
            for (TimeRange range : deniedTimeRanges) {
                if (range.contains(now)) {
                    return false;
                }
            }
        }
        
        // 허용 시간 범위 확인
        if (allowedTimeRanges != null && !allowedTimeRanges.isEmpty()) {
            for (TimeRange range : allowedTimeRanges) {
                if (range.contains(now)) {
                    return true;
                }
            }
            return false; // 허용 범위가 있는데 현재 시간이 포함되지 않음
        }
        
        return true; // 허용 범위가 없으면 기본적으로 허용
    }
    
    /**
     * 현재 요일이 허용되는지 확인
     * 
     * @return 허용되면 true, 그렇지 않으면 false
     */
    public boolean isCurrentDayAllowed() {
        String currentDay = java.time.DayOfWeek.from(java.time.LocalDate.now()).name();
        
        // 금지 요일 확인
        if (deniedDaysOfWeek != null && deniedDaysOfWeek.contains(currentDay)) {
            return false;
        }
        
        // 허용 요일 확인
        if (allowedDaysOfWeek != null && !allowedDaysOfWeek.isEmpty()) {
            return allowedDaysOfWeek.contains(currentDay);
        }
        
        return true; // 허용 요일이 없으면 기본적으로 허용
    }
}
