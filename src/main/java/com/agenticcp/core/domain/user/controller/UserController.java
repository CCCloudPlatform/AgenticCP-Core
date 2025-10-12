package com.agenticcp.core.domain.user.controller;

import com.agenticcp.core.common.dto.ApiResponse;
import com.agenticcp.core.domain.user.entity.User;
import com.agenticcp.core.domain.user.service.UserService;
import com.agenticcp.core.common.enums.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "모든 사용자 조회")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/active")
    @Operation(summary = "활성 사용자 조회")
    public ResponseEntity<ApiResponse<List<User>>> getActiveUsers() {
        List<User> users = userService.getActiveUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{username}")
    @Operation(summary = "특정 사용자 조회")
    public ResponseEntity<ApiResponse<User>> getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "사용자 검색")
    public ResponseEntity<ApiResponse<List<User>>> searchUsers(@RequestParam String keyword) {
        List<User> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "역할별 사용자 조회")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRole(@PathVariable UserRole role) {
        List<User> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/inactive")
    @Operation(summary = "비활성 사용자 조회")
    public ResponseEntity<ApiResponse<List<User>>> getInactiveUsers(
            @RequestParam(defaultValue = "30") int daysSinceLastLogin) {
        List<User> users = userService.getInactiveUsers(daysSinceLastLogin);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/locked")
    @Operation(summary = "잠긴 사용자 조회")
    public ResponseEntity<ApiResponse<List<User>>> getLockedUsers(
            @RequestParam(defaultValue = "5") int maxFailedAttempts) {
        List<User> users = userService.getLockedUsers(maxFailedAttempts);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping
    @Operation(summary = "사용자 생성")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "사용자가 생성되었습니다."));
    }

    @PutMapping("/{username}")
    @Operation(summary = "사용자 수정")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable String username, 
            @RequestBody User user) {
        User updatedUser = userService.updateUser(username, user);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "사용자가 수정되었습니다."));
    }

    @PatchMapping("/{username}/password")
    @Operation(summary = "비밀번호 변경")
    public ResponseEntity<ApiResponse<User>> changePassword(
            @PathVariable String username, 
            @RequestParam String newPassword) {
        User updatedUser = userService.changePassword(username, newPassword);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "비밀번호가 변경되었습니다."));
    }

    @PatchMapping("/{username}/unlock")
    @Operation(summary = "사용자 잠금 해제")
    public ResponseEntity<ApiResponse<User>> unlockUser(@PathVariable String username) {
        User unlockedUser = userService.unlockUser(username);
        return ResponseEntity.ok(ApiResponse.success(unlockedUser, "사용자 잠금이 해제되었습니다."));
    }

    @PatchMapping("/{username}/suspend")
    @Operation(summary = "사용자 일시정지")
    public ResponseEntity<ApiResponse<User>> suspendUser(@PathVariable String username) {
        User suspendedUser = userService.suspendUser(username);
        return ResponseEntity.ok(ApiResponse.success(suspendedUser, "사용자가 일시정지되었습니다."));
    }

    @PatchMapping("/{username}/activate")
    @Operation(summary = "사용자 활성화")
    public ResponseEntity<ApiResponse<User>> activateUser(@PathVariable String username) {
        User activatedUser = userService.activateUser(username);
        return ResponseEntity.ok(ApiResponse.success(activatedUser, "사용자가 활성화되었습니다."));
    }

    @DeleteMapping("/{username}")
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String username) {
        userService.deleteUser(username);
        return ResponseEntity.ok(ApiResponse.success(null, "사용자가 삭제되었습니다."));
    }
}
