package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.dto.BaseErrorCode;
import lombok.Getter;
/**
 * 애플리케이션의 비즈니스 로직 처리 과정에서 발생하는 예외의 최상위 클래스입니다.
 * <p>
 * 비즈니스 예외는 예측 가능한 예외 상황을 나타내며, 개발자의 코딩 실수나
 * 시스템 장애(예: NPE, 데이터베이스 연결 실패)와는 구분됩니다.
 * 이 예외는 {@code GlobalExceptionHandler}에 의해 클라이언트에게 적절한
 * HTTP 상태 코드와 에러 메시지를 담은 {@code ApiResponse}로 변환됩니다.
 * </p>
 * <p>
 * 새로운 커스텀 비즈니스 예외를 정의할 때는 이 클래스를 상속받아야 합니다.
 * </p>
 *
 * <pre>
 * // [사용 예시 - Service Layer]
 * public void updateUser(UserUpdateDto dto) {
 * if (isNicknameDuplicate(dto.getNickname())) {
 * // 예측 가능한 비즈니스 규칙 위반 시 BusinessException 또는 그 하위 예외를 던짐
 * throw new BusinessException(UserErrorCode.DUPLICATE_NICKNAME);
 * }
 * // ...
 * }
 * </pre>
 *
 * @see BaseErrorCode
 * @see GlobalExceptionHandler
 * @author hyobinyang
 * @since 2025.09.22
 */
@Getter
public class BusinessException extends RuntimeException {

    private final BaseErrorCode errorCode;

    public BusinessException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(BaseErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }
}
