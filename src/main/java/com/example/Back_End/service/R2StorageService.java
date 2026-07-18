package com.example.Back_End.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class R2StorageService {

    private final S3Client s3Client;
    private final String bucket;
    private final String publicDomain;

    public R2StorageService(
            S3Client s3Client,
            @Value("${cloudflare.r2.bucket}") String bucket,
            @Value("${cloudflare.r2.public-domain:}") String publicDomain
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.publicDomain = publicDomain != null ? publicDomain.replaceAll("/$", "") : null;
    }

    public String uploadFile(MultipartFile file, String folder) {
        System.out.println("[R2] uploadFile start, folder=" + folder + ", filename=" + file.getOriginalFilename() + ", size=" + file.getSize());
        try {
            String originalFilename = file.getOriginalFilename();
            String ext = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String key = folder + "/" + UUID.randomUUID() + ext;
            return putObject(key, file.getContentType(), RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (Exception e) {
            System.out.println("[R2] uploadFile FAILED: " + e.getMessage());
            throw new RuntimeException("Upload thất bại: " + e.getMessage(), e);
        }
    }

    /** Upload file local (sau FFmpeg) lên R2 */
    public String uploadPath(Path filePath, String folder, String filename, String contentType) {
        System.out.println("[R2] uploadPath start, folder=" + folder + ", filename=" + filename + ", path=" + filePath);
        try {
            if (filePath == null || !Files.exists(filePath)) {
                throw new IllegalArgumentException("File upload không tồn tại.");
            }
            String originalFilename = filename != null ? filename : filePath.getFileName().toString();
            String ext = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".mp3";
            String key = folder + "/" + UUID.randomUUID() + ext;
            String ct = contentType != null && !contentType.isBlank() ? contentType : "application/octet-stream";
            return putObject(key, ct, RequestBody.fromFile(filePath));
        } catch (Exception e) {
            System.out.println("[R2] uploadPath FAILED: " + e.getMessage());
            throw new RuntimeException("Upload thất bại: " + e.getMessage(), e);
        }
    }

    private String putObject(String key, String contentType, RequestBody body) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(request, body);
        System.out.println("[R2] putObject success, key=" + key);
        if (publicDomain != null && !publicDomain.isEmpty()) {
            return publicDomain + "/" + key;
        }
        return "https://" + bucket + ".r2.cloudflarestorage.com/" + key;
    }

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) return;
        try {
            String prefix = publicDomain != null ? publicDomain + "/" : "";
            String key = fileUrl;
            if (prefix != null && fileUrl.startsWith(prefix)) {
                key = fileUrl.substring(prefix.length());
            } else if (fileUrl.contains("/")) {
                int idx = fileUrl.indexOf("/", 8);
                if (idx > 0) key = fileUrl.substring(idx + 1);
            }
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            System.out.println("[R2] deleteFile failed: " + e.getMessage());
        }
    }
}
