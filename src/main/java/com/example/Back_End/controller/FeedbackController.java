package com.example.Back_End.controller;

import com.example.Back_End.entity.Feedback;
import com.example.Back_End.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** User gửi góp ý */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = body.get("email");
            String subject = body.get("subject");
            String content = body.get("content");
            String category = body.get("category");
            Feedback fb = feedbackService.create(email, subject, content, category);
            response.put("success", true);
            response.put("message", "Gửi góp ý thành công! Chúng tôi sẽ phản hồi sớm.");
            response.put("data", feedbackService.toMap(fb));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Không thể gửi góp ý. Vui lòng thử lại.");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** User xem góp ý của mình */
    @GetMapping("/mine")
    public ResponseEntity<?> mine(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<Map<String, Object>> data = feedbackService.listForUser(email.trim());
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    /** Admin: danh sách + thống kê */
    @GetMapping("/admin")
    public ResponseEntity<?> adminList(@RequestParam(required = false) String status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", feedbackService.listAll(status));
        response.put("stats", feedbackService.stats());
        return ResponseEntity.ok(response);
    }

    /** Admin: phản hồi (USER hoặc ALL) */
    @PostMapping("/admin/{id}/reply")
    public ResponseEntity<?> adminReply(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String reply = body.get("reply") != null ? String.valueOf(body.get("reply")) : null;
            String replyMode = body.get("replyMode") != null ? String.valueOf(body.get("replyMode")) : "USER";
            String status = body.get("status") != null ? String.valueOf(body.get("status")) : "RESOLVED";
            Boolean markSystem = null;
            if (body.get("isSystemIssue") != null) {
                markSystem = Boolean.parseBoolean(String.valueOf(body.get("isSystemIssue")));
            }
            Feedback fb = feedbackService.reply(id, reply, replyMode, status, markSystem);
            response.put("success", true);
            response.put("message", "ALL".equalsIgnoreCase(replyMode) || "BROADCAST".equalsIgnoreCase(replyMode)
                    ? "Đã phản hồi và gửi thông báo tới toàn bộ người dùng."
                    : "Đã phản hồi và gửi thông báo tới người dùng.");
            response.put("data", feedbackService.toMap(fb));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable root = e;
            while (root.getCause() != null) root = root.getCause();
            response.put("success", false);
            response.put("message", "Không thể gửi phản hồi: " + root.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /** Admin: cập nhật trạng thái */
    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Feedback fb = feedbackService.updateStatus(id, body.get("status"));
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công.");
            response.put("data", feedbackService.toMap(fb));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
