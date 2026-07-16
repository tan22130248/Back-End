package com.example.Back_End.controller;

import com.example.Back_End.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<Map<String, Object>> data = notificationService.getNotificationsForUser(email.trim());
        long unreadCount = notificationService.countUnread(email.trim());
        response.put("success", true);
        response.put("data", data);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        notificationService.markAsRead(id);
        response.put("success", true);
        response.put("message", "Đã đánh dấu đã đọc.");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        notificationService.markAllAsRead(email.trim());
        response.put("success", true);
        response.put("message", "Đã đánh dấu tất cả đã đọc.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/settings")
    public ResponseEntity<?> getSettings(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        Map<String, Object> settings = notificationService.getNotificationSettings(email.trim());
        response.put("success", true);
        response.put("data", settings);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestParam String email, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        Object enabledObj = body.get("enabled");
        boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : false;
        Map<String, Object> result = notificationService.updateNotificationSettings(email.trim(), enabled);
        response.putAll(result);
        return ResponseEntity.ok(response);
    }
}
