package com.example.Back_End.controller;

import com.example.Back_End.service.R2StorageService;
import com.example.Back_End.service.UserService;
import com.example.Back_End.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    private String getEmailFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.extractEmail(token);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        String email = getEmailFromToken(request);
        if (email == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        Map<String, Object> data = userService.getProfile(email);
        if (data == null) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String email = getEmailFromToken(request);
        if (email == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        if (body == null || body.isEmpty()) {
            response.put("success", false);
            response.put("message", "Không có dữ liệu cập nhật.");
            return ResponseEntity.badRequest().body(response);
        }

        boolean updated = userService.updateProfile(email, body);
        if (!updated) {
            response.put("success", false);
            response.put("message", "Cập nhật thất bại.");
            return ResponseEntity.badRequest().body(response);
        }

        Map<String, Object> data = userService.getProfile(email);
        response.put("success", true);
        response.put("message", "Cập nhật thông tin thành công.");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/upload-avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        String email = getEmailFromToken(request);
        if (email == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String avatarUrl = userService.uploadAvatar(email, file);
            response.put("success", true);
            response.put("message", "Upload ảnh đại diện thành công.");
            response.put("data", Map.of("avatar", avatarUrl));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String email = getEmailFromToken(request);
        if (email == null) {
            response.put("success", false);
            response.put("message", "Bạn cần đăng nhập.");
            return ResponseEntity.status(401).body(response);
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập đầy đủ mật khẩu hiện tại và mật khẩu mới.");
            return ResponseEntity.badRequest().body(response);
        }

        if (currentPassword.equals(newPassword)) {
            response.put("success", false);
            response.put("message", "Mật khẩu mới phải khác mật khẩu hiện tại.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            boolean changed = userService.changePassword(email, currentPassword, newPassword);
            if (!changed) {
                response.put("success", false);
                response.put("message", "Mật khẩu hiện tại không đúng.");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("message", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
