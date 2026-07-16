package com.example.Back_End.controller;

import com.example.Back_End.entity.Audio;
import com.example.Back_End.entity.Like;
import com.example.Back_End.repository.AudioRepository;
import com.example.Back_End.repository.LikeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoritesController {

    private final LikeRepository likeRepository;
    private final AudioRepository audioRepository;

    public FavoritesController(LikeRepository likeRepository, AudioRepository audioRepository) {
        this.likeRepository = likeRepository;
        this.audioRepository = audioRepository;
    }

    @GetMapping
    public ResponseEntity<?> getFavorites(@RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        List<Like> likes = likeRepository.findByUserEmailOrderByCreatedAtDesc(email.trim());
        List<Map<String, Object>> data = new ArrayList<>();
        for (Like like : likes) {
            Audio audio = audioRepository.findById(like.getAudioId()).orElse(null);
            if (audio == null) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("id", audio.getId());
            map.put("title", audio.getTitle());
            map.put("author", audio.getAuthor());
            map.put("genre", audio.getGenre());
            map.put("duration", audio.getDuration());
            map.put("fileSize", audio.getFileSize());
            map.put("audioUrl", audio.getAudioUrl());
            map.put("coverImageUrl", audio.getCoverImageUrl());
            map.put("viewCount", audio.getViewCount() != null ? audio.getViewCount() : 0);
            map.put("likeCount", audio.getLikeCount() != null ? audio.getLikeCount() : 0);
            map.put("createdAt", audio.getCreatedAt());
            map.put("updatedAt", audio.getUpdatedAt());
            data.add(map);
        }
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
