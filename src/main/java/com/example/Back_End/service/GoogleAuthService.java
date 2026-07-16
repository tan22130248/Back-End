package com.example.Back_End.service;

import com.example.Back_End.entity.User;
import com.example.Back_End.repository.UserRepository;
import com.example.Back_End.util.JwtUtil;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final String googleClientId;

    public GoogleAuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            @Value("${google.client-id:}") String googleClientId
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.googleClientId = googleClientId;
    }

    public Map<String, Object> loginWithIdToken(String idTokenString) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình GOOGLE_CLIENT_ID trên server.");
        }
        if (idTokenString == null || idTokenString.isBlank()) {
            throw new IllegalArgumentException("Thiếu Google ID token.");
        }

        GoogleIdToken.Payload payload = verifyIdToken(idTokenString);
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");
        String googleSub = payload.getSubject();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account không có email.");
        }
        if (emailVerified == null || !emailVerified) {
            throw new IllegalArgumentException("Email Google chưa được xác minh.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        // Tài khoản Google mới: role USER, gói FREE
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFullName(name != null && !name.isBlank() ? name.trim() : email.split("@")[0]);
            user.setPassword(passwordEncoder.encode(generateRandomPassword()));
            user.setRole("USER");
            user.setPlanType("FREE");
            user.setPlanExpiresAt(null);
            user.setAuthProvider("GOOGLE");
            user.setGoogleSub(googleSub);
            if (picture != null && !picture.isBlank()) {
                user.setAvatar(truncate(picture, 1000));
            }
            userRepository.save(user);
        } else {
            boolean dirty = false;
            if (user.getAuthProvider() == null || user.getAuthProvider().isBlank() || "LOCAL".equalsIgnoreCase(user.getAuthProvider())) {
                user.setAuthProvider("GOOGLE");
                dirty = true;
            }
            if ((user.getGoogleSub() == null || user.getGoogleSub().isBlank()) && googleSub != null) {
                user.setGoogleSub(googleSub);
                dirty = true;
            }
            if ((user.getAvatar() == null || user.getAvatar().isBlank()) && picture != null && !picture.isBlank()) {
                user.setAvatar(truncate(picture, 1000));
                dirty = true;
            }
            if (user.getPlanExpiresAt() != null && user.getPlanExpiresAt().isBefore(java.time.LocalDate.now())) {
                user.setPlanType("FREE");
                user.setPlanExpiresAt(null);
                dirty = true;
            }
            if (dirty) {
                userRepository.save(user);
            }
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("fullName", user.getFullName());
        data.put("email", user.getEmail());
        data.put("role", user.getRole());
        data.put("planType", user.getPlanType() != null ? user.getPlanType() : "FREE");
        data.put("avatar", user.getAvatar());
        return data;
    }

    private GoogleIdToken.Payload verifyIdToken(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Google ID token không hợp lệ.");
            }
            return idToken.getPayload();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Không thể xác thực Google token: " + e.getMessage());
        }
    }

    private String generateRandomPassword() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return "Ggl!" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
