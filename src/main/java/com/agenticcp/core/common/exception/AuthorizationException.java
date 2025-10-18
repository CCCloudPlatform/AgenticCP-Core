package com.agenticcp.core.common.exception;


import com.agenticcp.core.common.enums.CommonErrorCode;

/**
 * 인증은 되었지만 특정 리소스에 대한 접근 권한이 없을 때 발생하는 예외입니다.
 * <p>
 * 이 예외는 주로 서비스 계층에서 현재 사용자가 요청된 작업을 수행할 수 있는
 * 권한(예: 역할, 소유권)이 있는지 확인할 때 사용됩니다.
 * {@code GlobalExceptionHandler}에 의해 HTTP 403 Forbidden 상태 코드로 변환됩니다.
 * </p>
 * <p>
 * 이는 '누구인지 확인이 안 됨'(Authentication, 401 Unauthorized)과는 다른,
 * '누구인지는 알지만 권한이 없음'(Authorization, 403 Forbidden)을 의미합니다.
 * </p>
 *
 * <pre>
 * // [사용 예시 - Service Layer]
 * public void deletePost(Long userId, Long postId) {
 * Post post = postRepository.findById(postId)
 * .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
 *
 * if (!post.getOwnerId().equals(userId)) {
 * // 게시물의 소유자가 아니므로 권한 없음 예외를 던짐
 * throw new AuthorizationException(userId, "Post", "delete");
 * }
 *
 * postRepository.delete(post);
 * }
 * </pre>
 *
 * @see BusinessException
 * @see GlobalExceptionHandler
 * @author AgenticCP Team
 * @since 2025.09.22
 */
public class AuthorizationException extends BusinessException{

    public AuthorizationException() {
        super(CommonErrorCode.FORBIDDEN);
    }

    public AuthorizationException(Long userId, String resource, String action) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): '%s' 리소스에 대한 '%s' 권한이 없습니다.",
                        userId, resource, action));
    }

    public AuthorizationException(Long userId, String requiredRole) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 할당된 '%s' 역할이 없습니다.",
                        userId, requiredRole));
    }

    public AuthorizationException(Long userId) {
        super(CommonErrorCode.FORBIDDEN,
                String.format("User(ID: %d): 접근 권한이 없습니다.", userId));
    }
}
