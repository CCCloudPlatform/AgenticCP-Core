package com.agenticcp.core.domain.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimeConditions DTO 테스트
 * 
 * @author AgenticCP Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@DisplayName("TimeConditions DTO 테스트")
class TimeConditionsTest {
    
    @Nested
    @DisplayName("TimeRange 테스트")
    class TimeRangeTest {
        
        @Test
        @DisplayName("유효한 시간 범위")
        void isValid_ValidRange_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .description("업무시간")
                    .build();
            
            // When
            boolean result = range.isValid();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("시작 시간이 종료 시간보다 늦은 경우 (자정을 넘나드는 경우) - 유효함")
        void isValid_StartTimeAfterEndTime_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(18, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();
            
            // When
            boolean result = range.isValid();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("null 시작 시간")
        void isValid_NullStartTime_ReturnsFalse() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(null)
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            // When
            boolean result = range.isValid();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("null 종료 시간")
        void isValid_NullEndTime_ReturnsFalse() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(null)
                    .build();
            
            // When
            boolean result = range.isValid();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("시작 시간과 종료 시간이 같은 경우 - 무효")
        void isValid_SameStartAndEndTime_ReturnsFalse() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(9, 0))
                    .build();
            
            // When
            boolean result = range.isValid();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("시간이 범위 내에 포함됨")
        void contains_TimeWithinRange_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            LocalTime time = LocalTime.of(12, 30);
            
            // When
            boolean result = range.contains(time);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("시간이 범위 밖에 있음")
        void contains_TimeOutsideRange_ReturnsFalse() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            LocalTime time = LocalTime.of(20, 0);
            
            // When
            boolean result = range.contains(time);
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("시간이 시작 시간과 동일")
        void contains_TimeEqualsStartTime_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            LocalTime time = LocalTime.of(9, 0);
            
            // When
            boolean result = range.contains(time);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("시간이 종료 시간과 동일")
        void contains_TimeEqualsEndTime_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            LocalTime time = LocalTime.of(18, 0);
            
            // When
            boolean result = range.contains(time);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("자정을 넘나드는 시간 범위 - 범위 내")
        void contains_MidnightCrossing_WithinRange_ReturnsTrue() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(22, 0))
                    .endTime(LocalTime.of(6, 0))
                    .build();
            
            LocalTime time = LocalTime.of(23, 30);
            
            // When
            boolean result = range.contains(time);
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("null 시간 확인")
        void contains_NullTime_ReturnsFalse() {
            // Given
            TimeConditions.TimeRange range = TimeConditions.TimeRange.builder()
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(18, 0))
                    .build();
            
            // When
            boolean result = range.contains(null);
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("시간 조건 평가 테스트")
    class TimeConditionEvaluationTest {
        
        @Test
        @DisplayName("허용 시간 범위가 없으면 항상 허용")
        void isCurrentTimeAllowed_NoAllowedRanges_ReturnsTrue() {
            // Given
            TimeConditions conditions = TimeConditions.builder().build();
            
            // When
            boolean result = conditions.isCurrentTimeAllowed();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("금지 시간 범위에 포함되면 거부")
        void isCurrentTimeAllowed_InDeniedRange_ReturnsFalse() {
            // Given - 현재 시간을 포함하는 매우 넓은 금지 범위
            TimeConditions conditions = TimeConditions.builder()
                    .deniedTimeRanges(List.of(
                            TimeConditions.TimeRange.builder()
                                    .startTime(LocalTime.MIN)
                                    .endTime(LocalTime.MAX)
                                    .build()
                    ))
                    .build();
            
            // When
            boolean result = conditions.isCurrentTimeAllowed();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("허용 요일이 없으면 항상 허용")
        void isCurrentDayAllowed_NoAllowedDays_ReturnsTrue() {
            // Given
            TimeConditions conditions = TimeConditions.builder().build();
            
            // When
            boolean result = conditions.isCurrentDayAllowed();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("현재 요일이 금지 요일에 포함되면 거부")
        void isCurrentDayAllowed_InDeniedDays_ReturnsFalse() {
            // Given
            String currentDay = java.time.DayOfWeek.from(java.time.LocalDate.now()).name();
            TimeConditions conditions = TimeConditions.builder()
                    .deniedDaysOfWeek(List.of(currentDay))
                    .build();
            
            // When
            boolean result = conditions.isCurrentDayAllowed();
            
            // Then
            assertThat(result).isFalse();
        }
        
        @Test
        @DisplayName("현재 요일이 허용 요일에 포함되면 허용")
        void isCurrentDayAllowed_InAllowedDays_ReturnsTrue() {
            // Given
            String currentDay = java.time.DayOfWeek.from(java.time.LocalDate.now()).name();
            TimeConditions conditions = TimeConditions.builder()
                    .allowedDaysOfWeek(List.of(currentDay))
                    .build();
            
            // When
            boolean result = conditions.isCurrentDayAllowed();
            
            // Then
            assertThat(result).isTrue();
        }
        
        @Test
        @DisplayName("현재 요일이 허용 요일에 포함되지 않으면 거부")
        void isCurrentDayAllowed_NotInAllowedDays_ReturnsFalse() {
            // Given
            String currentDay = java.time.DayOfWeek.from(java.time.LocalDate.now()).name();
            List<String> allowedDays = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
            List<String> otherDays = allowedDays.stream()
                    .filter(day -> !day.equals(currentDay))
                    .toList();
            
            TimeConditions conditions = TimeConditions.builder()
                    .allowedDaysOfWeek(otherDays)
                    .build();
            
            // When
            boolean result = conditions.isCurrentDayAllowed();
            
            // Then
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {
        
        @Test
        @DisplayName("모든 필드로 객체 생성")
        void builder_AllFields_CreatesCompleteObject() {
            // Given
            List<TimeConditions.TimeRange> allowedRanges = List.of(
                    TimeConditions.TimeRange.builder()
                            .startTime(LocalTime.of(9, 0))
                            .endTime(LocalTime.of(18, 0))
                            .description("업무시간")
                            .build()
            );
            
            List<TimeConditions.TimeRange> deniedRanges = List.of(
                    TimeConditions.TimeRange.builder()
                            .startTime(LocalTime.of(22, 0))
                            .endTime(LocalTime.of(6, 0))
                            .description("야간시간")
                            .build()
            );
            
            List<String> allowedDays = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY");
            List<String> deniedDays = List.of("SATURDAY", "SUNDAY");
            List<Integer> allowedMonths = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
            List<Integer> deniedMonths = List.of(12);
            List<String> allowedDates = List.of("01-01", "12-25");
            List<String> deniedDates = List.of("05-05");
            
            // When
            TimeConditions conditions = TimeConditions.builder()
                    .allowedTimeRanges(allowedRanges)
                    .deniedTimeRanges(deniedRanges)
                    .allowedDaysOfWeek(allowedDays)
                    .deniedDaysOfWeek(deniedDays)
                    .allowedMonths(allowedMonths)
                    .deniedMonths(deniedMonths)
                    .allowedDates(allowedDates)
                    .deniedDates(deniedDates)
                    .timeZone("Asia/Seoul")
                    .build();
            
            // Then
            assertThat(conditions.getAllowedTimeRanges()).isEqualTo(allowedRanges);
            assertThat(conditions.getDeniedTimeRanges()).isEqualTo(deniedRanges);
            assertThat(conditions.getAllowedDaysOfWeek()).isEqualTo(allowedDays);
            assertThat(conditions.getDeniedDaysOfWeek()).isEqualTo(deniedDays);
            assertThat(conditions.getAllowedMonths()).isEqualTo(allowedMonths);
            assertThat(conditions.getDeniedMonths()).isEqualTo(deniedMonths);
            assertThat(conditions.getAllowedDates()).isEqualTo(allowedDates);
            assertThat(conditions.getDeniedDates()).isEqualTo(deniedDates);
            assertThat(conditions.getTimeZone()).isEqualTo("Asia/Seoul");
        }
    }
}

