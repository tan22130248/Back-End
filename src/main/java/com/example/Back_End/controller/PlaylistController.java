package com.example.Back_End.controller;

import com.example.Back_End.entity.Playlist;
import com.example.Back_End.entity.PlaylistAudio;
import com.example.Back_End.entity.User;
import com.example.Back_End.repository.PlaylistAudioRepository;
import com.example.Back_End.repository.PlaylistRepository;
import com.example.Back_End.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistRepository playlistRepository;
    private final PlaylistAudioRepository playlistAudioRepository;
    private final UserRepository userRepository;

    public PlaylistController(PlaylistRepository playlistRepository, PlaylistAudioRepository playlistAudioRepository, UserRepository userRepository) {
        this.playlistRepository = playlistRepository;
        this.playlistAudioRepository = playlistAudioRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getPlaylists(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        if (email == null || email.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }

        String userPlanType = "FREE";
        Optional<User> userOpt = userRepository.findByEmail(email.trim());
        if (userOpt.isPresent()) {
            userPlanType = userOpt.get().getPlanType() != null ? userOpt.get().getPlanType().toUpperCase() : "FREE";
        }

        List<Playlist> playlists = playlistRepository.findByUserEmailOrderByIdDesc(email.trim());
        List<Map<String, Object>> data = playlists.stream().map(p -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("userEmail", p.getUserEmail());
            m.put("createdAt", p.getCreatedAt());
            List<PlaylistAudio> audios = playlistAudioRepository.findByPlaylistId(p.getId());
            m.put("audioCount", audios.size());
            return m;
        }).collect(Collectors.toList());
        response.put("success", true);
        response.put("data", data);
        response.put("userPlanType", userPlanType);
        response.put("playlistLimit", "VIP".equals(userPlanType) ? null : ("PREMIUM".equals(userPlanType) ? 3 : 0));
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createPlaylist(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        String name = body.get("name");
        String userEmail = body.get("userEmail");
        if (name == null || name.isBlank()) {
            response.put("success", false);
            response.put("message", "Tên danh sách không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        if (userEmail == null || userEmail.isBlank()) {
            response.put("success", false);
            response.put("message", "Email không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }
        Playlist playlist = new Playlist();
        playlist.setName(name.trim());
        playlist.setUserEmail(userEmail.trim());
        playlistRepository.save(playlist);
        response.put("success", true);
        response.put("message", "Tạo danh sách phát thành công.");
        response.put("data", Map.of(
            "id", playlist.getId(),
            "name", playlist.getName(),
            "audioCount", 0
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{playlistId}/audios")
    public ResponseEntity<?> getPlaylistAudios(@PathVariable Long playlistId,
                                                @RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();
        Optional<Playlist> opt = playlistRepository.findById(playlistId);
        if (opt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Danh sách phát không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        Playlist playlist = opt.get();
        if (email != null && !email.isBlank() && !playlist.getUserEmail().equals(email.trim())) {
            response.put("success", false);
            response.put("message", "Bạn không có quyền truy cập danh sách phát này.");
            return ResponseEntity.status(403).body(response);
        }
        List<PlaylistAudio> links = playlistAudioRepository.findByPlaylistId(playlistId);
        List<Long> audioIds = links.stream().map(PlaylistAudio::getAudioId).collect(Collectors.toList());
        response.put("success", true);
        response.put("data", audioIds);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playlistId}/audio/{audioId}")
    public ResponseEntity<?> addAudioToPlaylist(@PathVariable Long playlistId, @PathVariable Long audioId,
                                                @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        if (!playlistRepository.existsById(playlistId)) {
            response.put("success", false);
            response.put("message", "Danh sách phát không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<PlaylistAudio> existing = playlistAudioRepository.findByPlaylistIdAndAudioId(playlistId, audioId);
        if (existing.isPresent()) {
            response.put("success", true);
            response.put("message", "Audio đã có trong danh sách phát.");
            response.put("duplicate", true);
            return ResponseEntity.ok(response);
        }

        String userEmail = body != null ? body.get("userEmail") : null;
        if (userEmail == null || userEmail.isBlank()) {
            response.put("success", false);
            response.put("message", "Email người dùng không được để trống.");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findByEmail(userEmail.trim());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "Người dùng không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }

        User user = userOpt.get();
        String planType = user.getPlanType() != null ? user.getPlanType().toUpperCase() : "FREE";

        if (!"VIP".equals(planType)) {
            long currentCount = playlistAudioRepository.findByPlaylistId(playlistId).size();
            long maxAllowed = "PREMIUM".equals(planType) ? 3 : 0;
            if (currentCount >= maxAllowed) {
                String upgradeMessage = "PREMIUM".equals(planType)
                    ? "Gói PREMIUM chỉ cho phép tối đa 3 audio mỗi danh sách phát. Hãy nâng cấp lên gói VIP để không giới hạn."
                    : "Gói FREE không được phép thêm audio vào danh sách phát. Hãy nâng cấp lên gói PREMIUM hoặc VIP.";
                response.put("success", false);
                response.put("message", upgradeMessage);
                response.put("limitReached", true);
                response.put("currentCount", currentCount);
                response.put("maxAllowed", maxAllowed);
                response.put("planType", planType);
                return ResponseEntity.badRequest().body(response);
            }
        }

        PlaylistAudio playlistAudio = new PlaylistAudio();
        playlistAudio.setPlaylistId(playlistId);
        playlistAudio.setAudioId(audioId);
        playlistAudioRepository.save(playlistAudio);
        response.put("success", true);
        response.put("message", "Đã thêm audio vào danh sách phát.");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{playlistId}/audio/{audioId}")
    public ResponseEntity<?> removeAudioFromPlaylist(@PathVariable Long playlistId, @PathVariable Long audioId) {
        Map<String, Object> response = new HashMap<>();
        if (!playlistRepository.existsById(playlistId)) {
            response.put("success", false);
            response.put("message", "Danh sách phát không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        Optional<PlaylistAudio> existing = playlistAudioRepository.findByPlaylistIdAndAudioId(playlistId, audioId);
        if (existing.isPresent()) {
            playlistAudioRepository.delete(existing.get());
        }
        response.put("success", true);
        response.put("message", "Đã xóa audio khỏi danh sách phát.");
        return ResponseEntity.ok(response);
    }
}
