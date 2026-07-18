package com.example.Back_End.service;

import com.example.Back_End.entity.Feedback;
import com.example.Back_End.entity.User;
import com.example.Back_End.repository.FeedbackRepository;
import com.example.Back_End.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class FeedbackService {

    private static final Set<String> CATEGORIES = Set.of(
            "BUG", "FEATURE", "ACCOUNT", "PAYMENT", "CONTENT", "OTHER"
    );

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    public Feedback create(String email, String subject, String content, String category) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Bạn cần đăng nhập để gửi góp ý.");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tiêu đề.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung góp ý.");
        }
        String cat = normalizeCategory(category);
        User user = userRepository.findByEmail(email.trim()).orElse(null);

        Feedback fb = new Feedback();
        fb.setUserEmail(email.trim());
        fb.setUserName(user != null ? user.getFullName() : null);
        fb.setSubject(subject.trim());
        fb.setContent(content.trim());
        fb.setCategory(cat);
        fb.setStatus("OPEN");
        fb.setSystemIssue(false);
        fb.setBroadcastSent(false);
        return feedbackRepository.save(fb);
    }

    public List<Map<String, Object>> listForUser(String email) {
        List<Feedback> list = feedbackRepository.findByUserEmailOrderByCreatedAtDesc(email);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Feedback fb : list) {
            result.add(toMap(fb));
        }
        return result;
    }

    public List<Map<String, Object>> listAll(String statusFilter) {
        List<Feedback> list;
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
            list = feedbackRepository.findByStatusOrderByCreatedAtDesc(statusFilter.trim().toUpperCase(Locale.ROOT));
        } else {
            list = feedbackRepository.findAllByOrderByCreatedAtDesc();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Feedback fb : list) {
            result.add(toMap(fb));
        }
        return result;
    }

    public Map<String, Object> stats() {
        Map<String, Object> s = new HashMap<>();
        s.put("total", feedbackRepository.count());
        s.put("open", feedbackRepository.countByStatus("OPEN"));
        s.put("inProgress", feedbackRepository.countByStatus("IN_PROGRESS"));
        s.put("resolved", feedbackRepository.countByStatus("RESOLVED"));
        s.put("closed", feedbackRepository.countByStatus("CLOSED"));
        return s;
    }

    /**
     * Admin phản hồi.
     * replyMode: USER = chỉ người gửi; ALL = broadcast toàn hệ thống (vấn đề hệ thống).
     */
    public Feedback reply(Long id, String reply, String replyMode, String status, Boolean markSystemIssue) {
        Feedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy góp ý."));
        if (reply == null || reply.isBlank()) {
            throw new IllegalArgumentException("Nội dung phản hồi không được để trống.");
        }

        String mode = replyMode != null ? replyMode.trim().toUpperCase(Locale.ROOT) : "USER";
        boolean broadcast = "ALL".equals(mode) || "BROADCAST".equals(mode);

        fb.setAdminReply(reply.trim());
        fb.setRepliedAt(LocalDateTime.now());
        if (markSystemIssue != null) {
            fb.setSystemIssue(markSystemIssue);
        } else if (broadcast) {
            fb.setSystemIssue(true);
        }

        String nextStatus = status != null && !status.isBlank()
                ? status.trim().toUpperCase(Locale.ROOT)
                : "RESOLVED";
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(nextStatus)) {
            nextStatus = "RESOLVED";
        }
        fb.setStatus(nextStatus);

        String notifTitle = "Phản hồi góp ý: " + fb.getSubject();
        String notifMessage = reply.trim();

        if (broadcast) {
            int count = notificationService.notifyAllUsers(notifTitle, notifMessage);
            fb.setBroadcastSent(true);
            fb.setSystemIssue(true);
            System.out.println("[FeedbackService] broadcast notifications: " + count);
        } else {
            notificationService.notifyUser(fb.getUserEmail(), notifTitle, notifMessage);
            fb.setBroadcastSent(false);
        }

        return feedbackRepository.save(fb);
    }

    public Feedback updateStatus(Long id, String status) {
        Feedback fb = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy góp ý."));
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        String next = status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED").contains(next)) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ.");
        }
        fb.setStatus(next);
        return feedbackRepository.save(fb);
    }

    public Map<String, Object> toMap(Feedback fb) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", fb.getId());
        m.put("userEmail", fb.getUserEmail());
        m.put("userName", fb.getUserName());
        m.put("category", fb.getCategory());
        m.put("subject", fb.getSubject());
        m.put("content", fb.getContent());
        m.put("status", fb.getStatus());
        m.put("adminReply", fb.getAdminReply());
        m.put("repliedAt", fb.getRepliedAt() != null ? fb.getRepliedAt().toString() : null);
        m.put("isSystemIssue", fb.isSystemIssue());
        m.put("broadcastSent", fb.isBroadcastSent());
        m.put("createdAt", fb.getCreatedAt() != null ? fb.getCreatedAt().toString() : null);
        m.put("updatedAt", fb.getUpdatedAt() != null ? fb.getUpdatedAt().toString() : null);
        return m;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return "OTHER";
        String c = category.trim().toUpperCase(Locale.ROOT);
        return CATEGORIES.contains(c) ? c : "OTHER";
    }
}
