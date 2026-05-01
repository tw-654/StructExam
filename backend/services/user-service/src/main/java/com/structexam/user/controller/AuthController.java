package com.structexam.user.controller;

import com.structexam.common.dto.*;
import com.structexam.common.entity.User;
import com.structexam.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.success("Registration successful", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("X-User-Id") Long userId) {
        userService.logout(userId);
        return ApiResponse.success("Logout successful", null);
    }

    @GetMapping("/userinfo")
    public ApiResponse<User> getUserInfo(@RequestHeader("X-User-Id") Long userId) {
        User user = userService.getUserById(userId);
        return ApiResponse.success(user);
    }

    @PutMapping("/password")
    public ApiResponse<Void> updatePassword(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam String oldPassword,
            @RequestParam String newPassword) {
        userService.updatePassword(userId, oldPassword, newPassword);
        return ApiResponse.success("Password updated successfully", null);
    }
}
