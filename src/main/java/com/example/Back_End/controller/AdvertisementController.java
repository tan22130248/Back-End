package com.example.Back_End.controller;

import com.example.Back_End.entity.Advertisement;
import com.example.Back_End.repository.AdvertisementRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/promotions")
public class AdvertisementController {

    private final AdvertisementRepository advertisementRepository;

    public AdvertisementController(AdvertisementRepository advertisementRepository) {
        this.advertisementRepository = advertisementRepository;
    }

    @GetMapping("/random")
    public ResponseEntity<?> getRandomAd() {
        Map<String, Object> response = new HashMap<>();
        long total = advertisementRepository.countAll();
        if (total == 0) {
            response.put("success", false);
            response.put("message", "Không có quảng cáo nào.");
            return ResponseEntity.ok(response);
        }

        Advertisement ad = advertisementRepository.findRandom();
        Map<String, Object> data = new HashMap<>();
        data.put("id", ad.getId());
        data.put("url", ad.getUrl());
        data.put("name", ad.getName());
        response.put("success", true);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    public ResponseEntity<?> listAllAds() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> ads = new ArrayList<>();
        for (Advertisement ad : advertisementRepository.findAll()) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ad.getId());
            map.put("url", ad.getUrl());
            map.put("name", ad.getName());
            map.put("createdAt", ad.getCreatedAt());
            map.put("updatedAt", ad.getUpdatedAt());
            ads.add(map);
        }
        response.put("success", true);
        response.put("data", ads);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> createAd(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String url = body.get("url");
            String name = body.get("name");

            if (url == null || url.isBlank()) {
                response.put("success", false);
                response.put("message", "URL quảng cáo không được để trống.");
                return ResponseEntity.badRequest().body(response);
            }

            Advertisement ad = new Advertisement();
            ad.setUrl(url.trim());
            ad.setName(name != null ? name.trim() : null);
            advertisementRepository.save(ad);

            response.put("success", true);
            response.put("message", "Thêm quảng cáo thành công.");
            response.put("data", Map.of(
                "id", ad.getId(),
                "url", ad.getUrl(),
                "name", ad.getName()
            ));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Thêm quảng cáo thất bại: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<?> updateAd(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Advertisement ad = advertisementRepository.findById(id).orElse(null);
        if (ad == null) {
            response.put("success", false);
            response.put("message", "Quảng cáo không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }

        String url = body.get("url");
        String name = body.get("name");

        if (url != null && !url.isBlank()) {
            ad.setUrl(url.trim());
        }
        if (name != null) {
            ad.setName(name.trim().isEmpty() ? null : name.trim());
        }

        advertisementRepository.save(ad);

        response.put("success", true);
        response.put("message", "Cập nhật quảng cáo thành công.");
        response.put("data", Map.of(
            "id", ad.getId(),
            "url", ad.getUrl(),
            "name", ad.getName()
        ));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteAd(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Advertisement ad = advertisementRepository.findById(id).orElse(null);
        if (ad == null) {
            response.put("success", false);
            response.put("message", "Quảng cáo không tồn tại.");
            return ResponseEntity.badRequest().body(response);
        }
        advertisementRepository.delete(ad);
        response.put("success", true);
        response.put("message", "Xóa quảng cáo thành công.");
        return ResponseEntity.ok(response);
    }
}
