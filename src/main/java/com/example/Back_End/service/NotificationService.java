package com.example.Back_End.service;

import com.example.Back_End.entity.Notification;
import com.example.Back_End.entity.User;
import com.example.Back_End.repository.NotificationRepository;
import com.example.Back_End.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public void notifyNewAudio(String title, String author, String genre, String duration, String audioUrl, String coverImageUrl, Long audioId) {
        List<User> allUsers = userRepository.findAll();
        String now = LocalDateTime.now().format(FORMATTER);
        String message = "Audio mới: \"" + title + "\" đã được đăng lúc " + now;

        List<Notification> notifications = new ArrayList<>();
        for (User user : allUsers) {
            Notification notification = new Notification();
            notification.setUserEmail(user.getEmail());
            notification.setAudioId(audioId);
            notification.setAudioTitle(title);
            notification.setAudioAuthor(author);
            notification.setAudioGenre(genre);
            notification.setAudioDuration(duration);
            notification.setAudioUrl(audioUrl);
            notification.setCoverImageUrl(coverImageUrl);
            notification.setMessage(message);
            notification.setRead(false);
            notifications.add(notification);
        }
        notificationRepository.saveAll(notifications);

        for (User user : allUsers) {
            if ("VIP".equalsIgnoreCase(user.getPlanType()) && user.isNotificationEnabled()) {
                sendNewAudioEmail(user.getEmail(), user.getFullName(), title, author, genre, duration, audioUrl, coverImageUrl, audioId);
            }
        }
    }

    private void sendNewAudioEmail(String to, String userName, String title, String author, String genre, String duration, String audioUrl, String coverImageUrl, Long audioId) {
        String subject = "[AudioStory] Audio mới: " + title;
        String coverHtml = coverImageUrl != null && !coverImageUrl.isBlank()
                ? "<img src=\"" + coverImageUrl + "\" alt=\"cover\" style=\"width:120px;border-radius:12px;margin-bottom:12px;\" />"
                : "";
        String html = "<h2>Kính gửi " + (userName != null ? userName : "bạn") + ",</h2>"
            + "<p>Có audio mới vừa được đăng trên <b>AudioStory</b>:</p>"
            + "<div style=\"background:#121126;border:1px solid rgba(255,255,255,0.08);border-radius:16px;padding:16px;max-width:320px;\">"
            + coverHtml
            + "<p style=\"margin:0;font-size:16px;font-weight:700;color:#fff;\">" + title + "</p>"
            + "<p style=\"margin:4px 0 0;font-size:12px;color:rgba(255,255,255,0.6);\">Tác giả: " + author + "</p>"
            + "<p style=\"margin:2px 0 0;font-size:12px;color:rgba(255,255,255,0.6);\">Thể loại: " + genre + " | Thời lượng: " + duration + "</p>"
            + "</div>"
            + "<p style=\"margin-top:12px;\"><a href=\"" + audioUrl + "\" style=\"color:#7c3aed;text-decoration:none;font-weight:600;\">Nghe ngay trên AudioStory</a></p>"
            + "<p>Trân trọng,<br/>Đội ngũ AudioStory</p>";
        try {
            emailService.sendHtmlEmail(to, subject, html);
        } catch (Exception e) {
            System.out.println("[NotificationService] send email FAILED: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getNotificationsForUser(String email) {
        List<Notification> list = notificationRepository.findByUserEmailOrderByCreatedAtDesc(email);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", n.getId());
            map.put("audioId", n.getAudioId());
            map.put("audioTitle", n.getAudioTitle());
            map.put("audioAuthor", n.getAudioAuthor());
            map.put("audioGenre", n.getAudioGenre());
            map.put("audioDuration", n.getAudioDuration());
            map.put("audioUrl", n.getAudioUrl());
            map.put("coverImageUrl", n.getCoverImageUrl());
            map.put("message", n.getMessage());
            map.put("isRead", n.isRead());
            map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            result.add(map);
        }
        return result;
    }

    public long countUnread(String email) {
        return notificationRepository.countByUserEmailAndRead(email, false);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    public void markAllAsRead(String email) {
        List<Notification> list = notificationRepository.findByUserEmailAndReadOrderByCreatedAtDesc(email, false);
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }

    public Map<String, Object> getNotificationSettings(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        Map<String, Object> settings = new HashMap<>();
        if (user != null) {
            settings.put("notificationEnabled", user.isNotificationEnabled());
            settings.put("planType", user.getPlanType());
        } else {
            settings.put("notificationEnabled", false);
            settings.put("planType", "FREE");
        }
        return settings;
    }

    public Map<String, Object> updateNotificationSettings(String email, boolean enabled) {
        User user = userRepository.findByEmail(email).orElse(null);
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("message", "Không tìm thấy người dùng.");
            return response;
        }
        if (!"VIP".equalsIgnoreCase(user.getPlanType())) {
            response.put("success", false);
            response.put("message", "Chỉ tài khoản VIP mới có thể bật thông báo email.");
            return response;
        }
        user.setNotificationEnabled(enabled);
        userRepository.save(user);
        response.put("success", true);
        response.put("message", enabled ? "Đã bật thông báo email." : "Đã tắt thông báo email.");
        response.put("notificationEnabled", enabled);
        return response;
    }
}
