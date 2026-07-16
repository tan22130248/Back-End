package com.example.Back_End.controller;

import com.example.Back_End.dto.AudioRequest;
import com.example.Back_End.entity.Audio;
import com.example.Back_End.entity.Like;
import com.example.Back_End.repository.AudioRepository;
import com.example.Back_End.repository.LikeRepository;
import com.example.Back_End.service.NotificationService;
import com.example.Back_End.service.R2StorageService;
import jakarta.validation.Valid;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audios")
public class AudioController {

    private final AudioRepository audioRepository;
    private final LikeRepository likeRepository;
    private final R2StorageService r2StorageService;
    private final NotificationService notificationService;

    public AudioController(AudioRepository audioRepository, LikeRepository likeRepository, R2StorageService r2StorageService, NotificationService notificationService) {
        this.audioRepository = audioRepository;
        this.likeRepository = likeRepository;
        this.r2StorageService = r2StorageService;
        this.notificationService = notificationService;
    }

    private String formatDuration(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return m + ":" + String.format("%02d", s);
    }

    private String extractDuration(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            File temp = Files.createTempFile("audio_", "." + getExtension(file.getOriginalFilename())).toFile();
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                IOUtils.copy(is, fos);
            }
            AudioFile audioFile = AudioFileIO.read(temp);
            long durationSec = audioFile.getAudioHeader().getTrackLength();
            temp.delete();
            return formatDuration(durationSec);
        } catch (Exception e) {
            return null;
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "tmp";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    @GetMapping
    public ResponseEntity<?> listAudios() {
        List<Map<String, Object>> audios = new ArrayList<>();
        for (Audio audio : audioRepository.findAll()) {
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
            audios.add(map);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", audios);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createAudio(@Valid @RequestBody AudioRequest body) {
        Map<String, Object> response = new HashMap<>();
        try {
            Audio audio = new Audio();
            audio.setTitle(body.getTitle());
            audio.setAuthor(body.getAuthor() != null ? body.getAuthor() : "");
            audio.setGenre(body.getGenre() != null ? body.getGenre() : "");
            audio.setDuration(body.getDuration() != null ? body.getDuration() : "0:00");
            audio.setFileSize(body.getFileSize() != null ? body.getFileSize() : 0L);
            audio.setAudioUrl(body.getAudioUrl());
            audio.setCoverImageUrl(body.getCoverImageUrl() != null ? body.getCoverImageUrl() : null);
            audioRepository.save(audio);

            notificationService.notifyNewAudio(
                audio.getTitle(),
                audio.getAuthor(),
                audio.getGenre(),
                audio.getDuration(),
                audio.getAudioUrl(),
                audio.getCoverImageUrl(),
                audio.getId()
            );

            response.put("success", true);
            response.put("message", "Thêm audio thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Thêm audio thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAudio(@PathVariable Long id, @Valid @RequestBody AudioRequest body) {
        Map<String, Object> response = new HashMap<>();
        Audio audio = audioRepository.findById(id).orElse(null);
        if (audio == null) {
            response.put("success", false);
            response.put("message", "Audio không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        audio.setTitle(body.getTitle());
        if (body.getAuthor() != null) audio.setAuthor(body.getAuthor());
        if (body.getGenre() != null) audio.setGenre(body.getGenre());
        if (body.getDuration() != null) audio.setDuration(body.getDuration());
        if (body.getFileSize() != null) audio.setFileSize(body.getFileSize());
        if (body.getAudioUrl() != null) audio.setAudioUrl(body.getAudioUrl());
        if (body.getCoverImageUrl() != null) audio.setCoverImageUrl(body.getCoverImageUrl());
        audioRepository.save(audio);
        response.put("success", true);
        response.put("message", "Cập nhật audio thành công.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAudio(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Audio audio = audioRepository.findById(id).orElse(null);
        if (audio == null) {
            response.put("success", false);
            response.put("message", "Audio không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        if (audio.getAudioUrl() != null && !audio.getAudioUrl().isEmpty()) {
            r2StorageService.deleteFile(audio.getAudioUrl());
        }
        audioRepository.delete(audio);
        response.put("success", true);
        response.put("message", "Xóa audio thành công.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAudio(@RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "folder", defaultValue = "audios") String folder) {
        Map<String, Object> response = new HashMap<>();
        try {
            String url = r2StorageService.uploadFile(file, folder);
            String duration = "audios".equals(folder) ? extractDuration(file) : null;
            response.put("success", true);
            response.put("url", url);
            if (duration != null) response.put("duration", duration);
            response.put("message", "Upload thành công.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Upload thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementView(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Audio audio = audioRepository.findById(id).orElse(null);
        if (audio == null) {
            response.put("success", false);
            response.put("message", "Audio không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        audio.setViewCount(audio.getViewCount() != null ? audio.getViewCount() + 1 : 1);
        audioRepository.save(audio);
        response.put("success", true);
        response.put("viewCount", audio.getViewCount());
        return ResponseEntity.ok(response);
    }

    @Transactional
    @PostMapping("/{id}/like")
    public ResponseEntity<?> toggleLike(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Audio audio = audioRepository.findById(id).orElse(null);
        if (audio == null) {
            response.put("success", false);
            response.put("message", "Audio không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        String email = body != null ? body.get("email") : null;
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email người dùng không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        boolean alreadyLiked = likeRepository.findByUserEmailAndAudioId(email, id).isPresent();
        if (alreadyLiked) {
            likeRepository.deleteByUserEmailAndAudioId(email, id);
            audioRepository.decrementLikeCount(id);
        } else {
            Like like = new Like();
            like.setUserEmail(email);
            like.setAudioId(id);
            likeRepository.save(like);
            audioRepository.incrementLikeCount(id);
        }
        Audio fresh = audioRepository.findById(id).orElse(audio);
        long currentCount = fresh.getLikeCount() != null ? fresh.getLikeCount() : 0;
        boolean likedNow = !alreadyLiked;
        response.put("success", true);
        response.put("liked", likedNow);
        response.put("likeCount", currentCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/liked")
    public ResponseEntity<?> getUserLikedAudios(@RequestParam String email) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"));
        }
        List<Like> likes = likeRepository.findByUserEmail(email);
        List<Long> likedAudioIds = new ArrayList<>();
        for (Like like : likes) {
            likedAudioIds.add(like.getAudioId());
        }
        return ResponseEntity.ok(Map.of("success", true, "data", likedAudioIds));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchAudios(@RequestParam(required = false) String q) {
        Map<String, Object> response = new HashMap<>();
        String query = q != null ? q.trim().toLowerCase() : "";
        if (query.isEmpty()) {
            response.put("success", true);
            response.put("data", List.of());
            return ResponseEntity.ok(response);
        }
        List<Audio> all = audioRepository.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Audio audio : all) {
            String title = audio.getTitle() != null ? audio.getTitle().toLowerCase() : "";
            String author = audio.getAuthor() != null ? audio.getAuthor().toLowerCase() : "";
            String genre = audio.getGenre() != null ? audio.getGenre().toLowerCase() : "";
            if (title.contains(query) || author.contains(query) || genre.contains(query)) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", audio.getId());
                map.put("title", audio.getTitle());
                map.put("author", audio.getAuthor());
                map.put("genre", audio.getGenre());
                map.put("duration", audio.getDuration());
                map.put("audioUrl", audio.getAudioUrl());
                map.put("coverImageUrl", audio.getCoverImageUrl());
                map.put("viewCount", audio.getViewCount() != null ? audio.getViewCount() : 0);
                map.put("likeCount", audio.getLikeCount() != null ? audio.getLikeCount() : 0);
                results.add(map);
            }
        }
        response.put("success", true);
        response.put("data", results);
        return ResponseEntity.ok(response);
    }
}
