package com.example.Back_End.controller;

import com.example.Back_End.dto.RegisterRequest;
import com.example.Back_End.entity.User;
import com.example.Back_End.repository.UserRepository;
import com.example.Back_End.service.EmailService;
import com.example.Back_End.service.GoogleAuthService;
import com.example.Back_End.service.OtpService;
import com.example.Back_End.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final OtpService otpService;
    private final GoogleAuthService googleAuthService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            EmailService emailService,
            OtpService otpService,
            GoogleAuthService googleAuthService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.emailService = emailService;
        this.otpService = otpService;
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.existsByEmail(request.getEmail())) {
            response.put("success", false);
            response.put("message", "Email đã được sử dụng. Vui lòng chọn email khác.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");

        userRepository.save(user);

        response.put("success", true);
        response.put("message", "Đăng ký thành công!");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String idToken = body != null ? body.get("idToken") : null;
            Map<String, Object> data = googleAuthService.loginWithIdToken(idToken);
            response.put("success", true);
            response.put("message", "Đăng nhập Google thành công.");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IllegalStateException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Đăng nhập Google thất bại. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();

        String email = body.get("email");
        String password = body.get("password");

        if (email == null || password == null) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập email và mật khẩu.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "Email hoặc mật khẩu không đúng.");
            return ResponseEntity.badRequest().body(response);
        }

        if (user.getPlanExpiresAt() != null && user.getPlanExpiresAt().isBefore(java.time.LocalDate.now())) {
            user.setPlanType("FREE");
            user.setPlanExpiresAt(null);
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("planType", user.getPlanType() != null ? user.getPlanType() : "FREE");

        response.put("success", true);
        response.put("message", "Đăng nhập thành công.");
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        System.out.println("[AuthController] forgot-password called, body=" + body);
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            System.out.println("[AuthController] forgot-password: email is blank");
            response.put("success", false);
            response.put("message", "Vui lòng nhập email.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            System.out.println("[AuthController] forgot-password: user not found for email=" + email);
            response.put("success", false);
            response.put("message", "Email không tồn tại trong hệ thống.");
            return ResponseEntity.badRequest().body(response);
        }

        System.out.println("[AuthController] forgot-password: user found, generating OTP");
        String otp = otpService.generateOtp(email);
        try {
            System.out.println("[AuthController] forgot-password: sending email to " + email);
            emailService.sendOtp(email, otp);
        } catch (Exception e) {
            System.out.println("[AuthController] forgot-password: email send FAILED, " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Không thể gửi email. Vui lòng thử lại sau.");
            return ResponseEntity.internalServerError().body(response);
        }

        System.out.println("[AuthController] forgot-password: success");
        response.put("success", true);
        response.put("message", "Mã OTP đã được gửi đến email của bạn.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, String> body) {
        System.out.println("[AuthController] verify-otp called, body=" + body);
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null || otp.isBlank()) {
            response.put("success", false);
            response.put("message", "Thiếu thông tin xác thực.");
            return ResponseEntity.badRequest().body(response);
        }

        if (otpService.verifyOtp(email, otp)) {
            response.put("success", true);
            response.put("message", "Xác nhận OTP thành công.");
            return ResponseEntity.ok(response);
        }

        response.put("success", false);
        response.put("message", "Mã OTP không hợp lệ hoặc đã hết hạn.");
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        System.out.println("[AuthController] reset-password called, body keys=" + (body != null ? body.keySet() : "null"));
        Map<String, Object> response = new HashMap<>();
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || otp == null || newPassword == null || newPassword.isBlank()) {
            response.put("success", false);
            response.put("message", "Thiếu thông tin.");
            return ResponseEntity.badRequest().body(response);
        }

        if (!otpService.verifyOtp(email, otp)) {
            response.put("success", false);
            response.put("message", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword.length() <= 6 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*\\d.*")) {
            response.put("success", false);
            response.put("message", "Mật khẩu mới phải từ 7 ký tự, có ít nhất 1 chữ hoa và 1 số.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        otpService.removeOtp(email);

        response.put("success", true);
        response.put("message", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            Map<String, Object> u = new HashMap<>();
            u.put("id", user.getId());
            u.put("name", user.getFullName());
            u.put("email", user.getEmail());
            u.put("role", user.getRole());
            u.put("planType", user.getPlanType() != null ? user.getPlanType() : "FREE");
            u.put("planExpiresAt", user.getPlanExpiresAt() != null ? user.getPlanExpiresAt().toString() : null);
            u.put("status", "Hoạt động");
            u.put("phone", "");
            u.put("gender", "");
            u.put("birthday", "");
            u.put("avatar", "");
            users.add(u);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", users);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        User user = userRepository.findByEmail(email.trim()).orElse(null);
        if (user == null) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("role", user.getRole());
        data.put("planType", user.getPlanType() != null ? user.getPlanType() : "FREE");
        data.put("planExpiresAt", user.getPlanExpiresAt() != null ? user.getPlanExpiresAt().toString() : null);
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
