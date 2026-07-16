package com.example.Back_End.controller;

import com.example.Back_End.entity.PremiumRegistration;
import com.example.Back_End.entity.User;
import com.example.Back_End.repository.PremiumRegistrationRepository;
import com.example.Back_End.repository.UserRepository;
import com.example.Back_End.service.EmailService;
import com.example.Back_End.service.R2StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/premium")
public class PremiumController {

    private final PremiumRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final R2StorageService r2StorageService;

    private static final String ADMIN_EMAIL = "tannguyen.4420@gmail.com";

    public PremiumController(PremiumRegistrationRepository registrationRepository, UserRepository userRepository, EmailService emailService, R2StorageService r2StorageService) {
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.r2StorageService = r2StorageService;
    }

    private Map<String, Object> toMap(PremiumRegistration r) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", r.getId());
        map.put("userEmail", r.getUserEmail());
        map.put("userName", r.getUserName());
        map.put("planName", r.getPlanName());
        map.put("price", r.getPrice());
        map.put("status", r.getStatus().name());
        map.put("receiptImage", r.getReceiptImage());
        map.put("receiptFile", r.getReceiptImage());
        map.put("registeredAt", r.getRegisteredAt() != null ? r.getRegisteredAt().toString() : null);
        map.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
        return map;
    }

    @PostMapping(value = "/register", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<?> register(
            @RequestParam("email") String email,
            @RequestParam("userName") String userName,
            @RequestParam("planName") String planName,
            @RequestParam("price") String price,
            @RequestParam(value = "receipt", required = false) MultipartFile receiptFile
    ) {
        Map<String, Object> response = new HashMap<>();

        if (email == null || email.isBlank() || planName == null || planName.isBlank()) {
            response.put("success", false);
            response.put("message", "Thiếu thông tin đăng ký.");
            return ResponseEntity.badRequest().body(response);
        }

        String receiptUrl = null;
        if (receiptFile != null && !receiptFile.isEmpty()) {
            try {
                receiptUrl = r2StorageService.uploadFile(receiptFile, "receipts");
            } catch (Exception e) {
                System.out.println("[PremiumController] upload receipt FAILED: " + e.getMessage());
            }
        }

        PremiumRegistration registration = new PremiumRegistration();
        registration.setUserEmail(email.trim());
        registration.setUserName(userName != null ? userName : email.trim());
        registration.setPlanName(planName.trim());
        registration.setPrice(price != null ? price : "");
        registration.setStatus(PremiumRegistration.Status.PENDING);
        registration.setReceiptImage(receiptUrl);
        registrationRepository.save(registration);

        String subject = "[AudioStory] Yêu cầu đăng ký " + planName + " - " + email;
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String normalizedPlan = planName != null ? planName.toLowerCase(java.util.Locale.ROOT) : "";
        StringBuilder text = new StringBuilder();
        text.append("Kính gửi Admin,\n\n")
            .append("Có yêu cầu đăng ký gói Premium mới:\n")
            .append("- Tên người dùng: ").append(registration.getUserName()).append("\n")
            .append("- Email: ").append(email).append("\n")
            .append("- Gói đăng ký: ").append(planName).append("\n")
            .append("- Giá: ").append(price != null ? price : "N/A").append("\n")
            .append("- Thời gian đăng ký: ").append(now).append("\n")
            .append("- Trạng thái: Chờ xác nhận\n")
            .append("- Ảnh giao dịch: ").append(receiptUrl != null ? receiptUrl : "Không đính kèm").append("\n\n");

        if (normalizedPlan.contains("vip")) {
            text.append("Mô tả quyền lợi gói VIP:\n")
                .append("- Tất cả quyền lợi của gói Premium\n")
                .append("- Nghe tập mới sớm hơn (có thông báo qua email khi có truyện mới, vui lòng kiểm tra mail chính chủ)\n")
                .append("- Tạo danh sách phát cá nhân không giới hạn số lượng audio\n\n");
        } else {
            text.append("Mô tả quyền lợi gói Premium:\n")
                .append("- Không quảng cáo khi nghe audio\n")
                .append("- Tạo danh sách phát cá nhân (các audio trong danh sách sẽ tự động phát tiếp sau khi phát hết mỗi audio, tối đa 3 audio)\n\n");
        }

        text.append("Nếu có vấn đề, vui lòng liên hệ: tannguyen.4420@gmail.com\n\n")
            .append("Vui lòng kiểm tra và xác nhận thanh toán trong hệ thống.");

        try {
            emailService.sendHtmlEmail(ADMIN_EMAIL, subject, text.toString().replace("\n", "<br>"));
        } catch (Exception e) {
            System.out.println("[PremiumController] send email FAILED: " + e.getMessage());
        }

        response.put("success", true);
        response.put("message", "Đăng ký thành công. Chúng tôi sẽ xác nhận trong 5-7 giờ.");
        response.put("data", Map.of(
                "id", registration.getId(),
                "planName", registration.getPlanName(),
                "price", registration.getPrice(),
                "status", registration.getStatus().name(),
                "receiptImage", receiptUrl,
                "registeredAt", registration.getRegisteredAt() != null ? registration.getRegisteredAt().toString() : null
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/registrations")
    public ResponseEntity<?> getRegistrations(@RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<PremiumRegistration> list = registrationRepository.findByUserEmailOrderByRegisteredAtDesc(email.trim());
        List<Map<String, Object>> data = new ArrayList<>();
        for (PremiumRegistration r : list) {
            data.add(toMap(r));
        }
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/registrations")
    public ResponseEntity<?> getAllRegistrations() {
        Map<String, Object> response = new HashMap<>();
        List<PremiumRegistration> list = registrationRepository.findByOrderByRegisteredAtDesc();
        List<Map<String, Object>> data = new ArrayList<>();
        for (PremiumRegistration r : list) {
            data.add(toMap(r));
        }
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/registrations/{id}")
    public ResponseEntity<?> deleteRegistration(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        PremiumRegistration registration = registrationRepository.findById(id).orElse(null);
        if (registration == null) {
            response.put("success", false);
            response.put("message", "Đăng ký không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        registrationRepository.delete(registration);
        response.put("success", true);
        response.put("message", "Đã xóa đăng ký.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/registrations/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        PremiumRegistration registration = registrationRepository.findById(id).orElse(null);
        if (registration == null) {
            response.put("success", false);
            response.put("message", "Đăng ký không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        String newStatus = body.get("status");
        String reason = body.get("reason");
        if (newStatus == null || newStatus.isBlank()) {
            response.put("success", false);
            response.put("message", "Thiếu trạng thái.");
            return ResponseEntity.badRequest().body(response);
        }
        try {
            PremiumRegistration.Status oldStatus = registration.getStatus();
            PremiumRegistration.Status targetStatus = PremiumRegistration.Status.valueOf(newStatus.trim().toUpperCase());
            registration.setStatus(targetStatus);
            registrationRepository.save(registration);

            if (targetStatus == PremiumRegistration.Status.COMPLETED && oldStatus != PremiumRegistration.Status.COMPLETED) {
                userRepository.findByEmail(registration.getUserEmail()).ifPresent(user -> {
                    if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                        user.setPlanType("FREE");
                        user.setPlanExpiresAt(null);
                    } else {
                        String planType = "PREMIUM";
                        if (registration.getPlanName() != null && registration.getPlanName().contains("VIP")) {
                            planType = "VIP";
                        }
                        user.setPlanType(planType);
                        user.setPlanExpiresAt(LocalDate.now().plusDays(30));
                    }
                    userRepository.save(user);
                });

                try {
                    emailService.sendPremiumConfirmedEmail(
                            registration.getUserEmail(),
                            registration.getUserName(),
                            registration.getPlanName(),
                            registration.getPrice()
                    );
                } catch (Exception e) {
                    System.out.println("[PremiumController] send confirmed email FAILED: " + e.getMessage());
                }
            } else if (targetStatus == PremiumRegistration.Status.CANCELLED && oldStatus != PremiumRegistration.Status.CANCELLED) {
                userRepository.findByEmail(registration.getUserEmail()).ifPresent(user -> {
                    if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                        user.setPlanType("FREE");
                        user.setPlanExpiresAt(null);
                        userRepository.save(user);
                    }
                });

                try {
                    emailService.sendPremiumRejectedEmail(
                            registration.getUserEmail(),
                            registration.getUserName(),
                            registration.getPlanName(),
                            reason != null ? reason : ""
                    );
                } catch (Exception e) {
                    System.out.println("[PremiumController] send rejected email FAILED: " + e.getMessage());
                }
            }

            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công.");
            response.put("data", Map.of(
                    "id", registration.getId(),
                    "status", registration.getStatus().name(),
                    "updatedAt", registration.getUpdatedAt() != null ? registration.getUpdatedAt().toString() : null
            ));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", "Trạng thái không hợp lệ. Chỉ chấp nhận: PENDING, COMPLETED, CANCELLED");
            return ResponseEntity.badRequest().body(response);
        }
    }
}
