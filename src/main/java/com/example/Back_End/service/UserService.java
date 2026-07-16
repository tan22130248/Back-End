package com.example.Back_End.service;

import com.example.Back_End.entity.User;
import com.example.Back_End.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final R2StorageService r2StorageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, R2StorageService r2StorageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.r2StorageService = r2StorageService;
    }

    public Map<String, Object> getProfile(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return null;

        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("fullName", user.getFullName());
        data.put("phone", user.getPhone());
        data.put("birthday", user.getBirthday() != null ? user.getBirthday().toString() : null);
        data.put("gender", user.getGender());
        data.put("avatar", user.getAvatar());
        data.put("planType", user.getPlanType() != null ? user.getPlanType() : "FREE");
        data.put("role", user.getRole());
        return data;
    }

    public boolean updateProfile(String email, Map<String, String> updates) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        if (updates.containsKey("fullName")) {
            String fullName = updates.get("fullName");
            if (fullName != null && !fullName.isBlank()) {
                user.setFullName(fullName.trim());
            }
        }

        if (updates.containsKey("phone")) {
            String phone = updates.get("phone");
            user.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        }

        if (updates.containsKey("birthday")) {
            String birthday = updates.get("birthday");
            if (birthday != null && !birthday.isBlank()) {
                user.setBirthday(LocalDate.parse(birthday));
            } else {
                user.setBirthday(null);
            }
        }

        if (updates.containsKey("gender")) {
            String gender = updates.get("gender");
            user.setGender(gender != null && !gender.isBlank() ? gender.trim() : null);
        }

        userRepository.save(user);
        return true;
    }

    public String uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không hợp lệ.");
        }

        long maxSize = 5 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Kích thước ảnh vượt quá 5MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận định dạng JPG/PNG.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) throw new IllegalArgumentException("Người dùng không tồn tại.");

        String avatarUrl = r2StorageService.uploadFile(file, "avatars");
        user.setAvatar(avatarUrl);
        userRepository.save(user);

        return avatarUrl;
    }

    public boolean changePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }

        if (newPassword == null || newPassword.length() <= 6 || !newPassword.matches(".*[A-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Mật khẩu mới phải từ 7 ký tự, có ít nhất 1 chữ hoa và 1 số.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
