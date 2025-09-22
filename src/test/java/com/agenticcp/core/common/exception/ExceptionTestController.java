package com.agenticcp.core.common.exception;

import com.agenticcp.core.common.enums.CommonErrorCode;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class ExceptionTestController {

    @GetMapping("/business-exception")
    public String throwBusinessException() {
        throw new BusinessException(CommonErrorCode.BAD_REQUEST);
    }

    @GetMapping("/data-access-exception")
    public String throwDataAccessException() {
        throw new DataAccessException("Database error") {};
    }

    @GetMapping("/authorization-exception")
    public String throwAuthorizationException() {
        throw new AuthorizationException(1L, "testResource", "read");
    }

    @GetMapping("/resource-not-found")
    public String throwResourceNotFoundException() {
        throw new ResourceNotFoundException("User", "id", "123");
    }

    @PostMapping("/validation-error")
    public String validationError(@Valid @RequestBody TestRequest request) {
        return "success";
    }

    @PostMapping("/method-not-allowed")
    public String methodNotAllowed() {
        return "success";
    }

    @GetMapping("/type-mismatch/{id}")
    public String typeMismatch(@PathVariable @Min(1) Long id) {
        return "success";
    }

    @GetMapping("/missing-param")
    public String missingParam(@RequestParam @NotBlank String requiredParam) {
        return "success";
    }

    @PostMapping("/malformed-json")
    public String malformedJson(@Valid @RequestBody TestRequest request) {
        return "success";
    }

    @GetMapping("/unexpected-exception")
    public String throwUnexpectedException() {
        throw new RuntimeException("서버 내부 오류가 발생했습니다.");
    }

     public record TestRequest(
         @NotNull(message = "COMMON_400") String name,
         @NotBlank(message = "COMMON_400") String email
     ) {}
}
