package com.example.Back_End.controller;

import com.example.Back_End.entity.ListeningHistory;
import com.example.Back_End.repository.ListeningHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/history")
public class ListeningHistoryController {

    private final ListeningHistoryRepository listeningHistoryRepository;

    public ListeningHistoryController(ListeningHistoryRepository listeningHistoryRepository) {
        this.listeningHistoryRepository = listeningHistoryRepository;
    }

    @GetMapping
    public ResponseEntity<?> getHistory(@RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<ListeningHistory> history = listeningHistoryRepository.findByUserEmailOrderByListenedAtDesc(email.trim());
        List<Map<String, Object>> data = new ArrayList<>();
        for (ListeningHistory h : history) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("audioId", h.getAudioId());
            map.put("title", h.getAudioTitle());
            map.put("author", h.getAudioAuthor());
            map.put("genre", h.getAudioGenre());
            map.put("duration", h.getAudioDuration());
            map.put("audioUrl", h.getAudioUrl());
            map.put("coverImageUrl", h.getCoverImageUrl());
            map.put("listenedAt", h.getListenedAt());
            map.put("progress", h.getProgress());
            data.add(map);
        }
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping
    public ResponseEntity<?> addHistory(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = (String) body.get("email");
            Long audioId = body.get("audioId") != null ? Long.parseLong(body.get("audioId").toString()) : null;
            String title = (String) body.get("title");
            String author = (String) body.get("author");
            String genre = (String) body.get("genre");
            String duration = (String) body.get("duration");
            String audioUrl = (String) body.get("audioUrl");
            String coverImageUrl = (String) body.get("coverImageUrl");
            Integer progress = body.get("progress") != null ? Integer.parseInt(body.get("progress").toString()) : 0;

            if (email == null || email.isBlank() || audioId == null || title == null || audioUrl == null) {
                response.put("success", false);
                response.put("message", "Thiếu thông tin bắt buộc.");
                return ResponseEntity.badRequest().body(response);
            }

            listeningHistoryRepository.deleteByUserEmailAndAudioId(email.trim(), audioId);

            ListeningHistory history = new ListeningHistory();
            history.setUserEmail(email.trim());
            history.setAudioId(audioId);
            history.setAudioTitle(title);
            history.setAudioAuthor(author != null ? author : "");
            history.setAudioGenre(genre != null ? genre : "");
            history.setAudioDuration(duration != null ? duration : "0:00");
            history.setAudioUrl(audioUrl);
            history.setCoverImageUrl(coverImageUrl);
            history.setProgress(progress);
            listeningHistoryRepository.save(history);

            response.put("success", true);
            response.put("message", "Đã lưu lịch sử nghe.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lưu lịch sử thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/progress")
    public ResponseEntity<?> updateProgress(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String email = (String) body.get("email");
            Long audioId = body.get("audioId") != null ? Long.parseLong(body.get("audioId").toString()) : null;
            Integer progress = body.get("progress") != null ? Integer.parseInt(body.get("progress").toString()) : 0;

            if (email == null || email.isBlank() || audioId == null) {
                response.put("success", false);
                response.put("message", "Thiếu thông tin bắt buộc.");
                return ResponseEntity.badRequest().body(response);
            }

            Optional<ListeningHistory> historyOpt = listeningHistoryRepository.findByUserEmailOrderByListenedAtDesc(email.trim()).stream()
                .filter(h -> h.getAudioId().equals(audioId))
                .findFirst();

            if (historyOpt.isPresent()) {
                ListeningHistory history = historyOpt.get();
                history.setProgress(Math.max(0, Math.min(100, progress)));
                listeningHistoryRepository.save(history);
            }

            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Cập nhật tiến trình thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHistory(@PathVariable Long id, @RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        Optional<ListeningHistory> historyOpt = listeningHistoryRepository.findById(id);
        if (historyOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Lịch sử không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        ListeningHistory history = historyOpt.get();
        if (!history.getUserEmail().equalsIgnoreCase(email.trim())) {
            response.put("success", false);
            response.put("message", "Không có quyền xóa lịch sử này.");
            return ResponseEntity.badRequest().body(response);
        }
        listeningHistoryRepository.delete(history);
        response.put("success", true);
        response.put("message", "Xóa lịch sử thành công.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<?> clearHistory(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<ListeningHistory> history = listeningHistoryRepository.findByUserEmailOrderByListenedAtDesc(email.trim());
        listeningHistoryRepository.deleteAll(history);
        response.put("success", true);
        response.put("message", "Đã xóa toàn bộ lịch sử nghe.");
        return ResponseEntity.ok(response);
    }
}
