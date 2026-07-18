package com.example.Back_End.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Nén MP3/MP4 → MP3 mono 48kbps 22050Hz bằng FFmpeg trước khi upload R2.
 */
@Service
public class AudioCompressService {

    private static final Set<String> ALLOWED_EXT = Set.of("mp3", "mp4", "m4a", "wav", "mpeg", "x-m4a");
    private static final long MAX_INPUT_BYTES = 200L * 1024 * 1024; // 200MB

    private final String ffmpegPath;
    private final int bitrateKbps;
    private final int sampleRate;
    private final long processTimeoutSec;

    public AudioCompressService(
            @Value("${audio.ffmpeg-path:ffmpeg}") String ffmpegPath,
            @Value("${audio.bitrate-kbps:48}") int bitrateKbps,
            @Value("${audio.sample-rate:22050}") int sampleRate,
            @Value("${audio.process-timeout-sec:600}") long processTimeoutSec
    ) {
        this.ffmpegPath = ffmpegPath != null && !ffmpegPath.isBlank() ? ffmpegPath.trim() : "ffmpeg";
        this.bitrateKbps = bitrateKbps > 0 ? bitrateKbps : 48;
        this.sampleRate = sampleRate > 0 ? sampleRate : 22050;
        this.processTimeoutSec = processTimeoutSec > 0 ? processTimeoutSec : 600;
    }

    public static class CompressResult {
        private final Path outputPath;
        private final long outputSize;
        private final String duration;
        private final String contentType;
        private final String originalName;

        public CompressResult(Path outputPath, long outputSize, String duration, String contentType, String originalName) {
            this.outputPath = outputPath;
            this.outputSize = outputSize;
            this.duration = duration;
            this.contentType = contentType;
            this.originalName = originalName;
        }

        public Path getOutputPath() { return outputPath; }
        public long getOutputSize() { return outputSize; }
        public String getDuration() { return duration; }
        public String getContentType() { return contentType; }
        public String getOriginalName() { return originalName; }
    }

    public boolean isAudioFolder(String folder) {
        return folder != null && "audios".equalsIgnoreCase(folder.trim());
    }

    public void validateAudioInput(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File audio không hợp lệ.");
        }
        if (file.getSize() > MAX_INPUT_BYTES) {
            throw new IllegalArgumentException("File quá lớn (tối đa 200MB).");
        }
        String ext = extensionOf(file.getOriginalFilename());
        String ct = file.getContentType() != null ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        boolean okExt = ALLOWED_EXT.contains(ext);
        boolean okCt = ct.startsWith("audio/") || ct.startsWith("video/mp4") || ct.contains("mpeg") || ct.contains("mp4");
        if (!okExt && !okCt) {
            throw new IllegalArgumentException("Chỉ chấp nhận MP3 hoặc MP4 (và một số định dạng audio phổ biến).");
        }
    }

    /**
     * Lưu file tạm → FFmpeg nén → trả path MP3 đã nén.
     * Caller phải xóa file output (và không cần lo input — đã xóa trong finally).
     */
    public CompressResult compressToMp3(MultipartFile file) {
        validateAudioInput(file);
        ensureFfmpegAvailable();

        Path workDir = null;
        Path inputPath = null;
        Path outputPath = null;
        try {
            workDir = Files.createTempDirectory("audio-compress-");
            String inExt = extensionOf(file.getOriginalFilename());
            if (inExt.isBlank()) inExt = "bin";
            inputPath = workDir.resolve("input." + inExt);
            outputPath = workDir.resolve("output-" + UUID.randomUUID() + ".mp3");

            file.transferTo(inputPath.toFile());
            System.out.println("[FFmpeg] compress start in=" + inputPath + " size=" + Files.size(inputPath));

            runFfmpeg(inputPath, outputPath);

            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                throw new IllegalStateException("FFmpeg không tạo được file output.");
            }

            long outSize = Files.size(outputPath);
            String duration = probeDuration(outputPath);
            String baseName = stripExtension(file.getOriginalFilename());
            if (baseName == null || baseName.isBlank()) baseName = "audio";

            System.out.println("[FFmpeg] compress done outSize=" + outSize + " duration=" + duration);

            // Xóa input ngay; giữ output cho caller upload
            safeDelete(inputPath);
            inputPath = null;

            return new CompressResult(outputPath, outSize, duration, "audio/mpeg", baseName + ".mp3");
        } catch (IllegalArgumentException | IllegalStateException e) {
            safeDelete(outputPath);
            safeDelete(inputPath);
            safeDeleteDir(workDir);
            throw e;
        } catch (Exception e) {
            safeDelete(outputPath);
            safeDelete(inputPath);
            safeDeleteDir(workDir);
            throw new RuntimeException("Nén audio thất bại: " + e.getMessage(), e);
        }
    }

    public void cleanup(CompressResult result) {
        if (result == null || result.getOutputPath() == null) return;
        Path out = result.getOutputPath();
        Path parent = out.getParent();
        safeDelete(out);
        safeDeleteDir(parent);
    }

    private void runFfmpeg(Path input, Path output) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>();
        cmd.add(ffmpegPath);
        cmd.add("-y");
        cmd.add("-i");
        cmd.add(input.toAbsolutePath().toString());
        cmd.add("-vn");                 // bỏ video (MP4)
        cmd.add("-ac");
        cmd.add("1");                    // mono
        cmd.add("-ar");
        cmd.add(String.valueOf(sampleRate)); // 22050 Hz
        cmd.add("-b:a");
        cmd.add(bitrateKbps + "k");      // 48k
        cmd.add("-codec:a");
        cmd.add("libmp3lame");
        cmd.add(output.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder log = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (log.length() < 4000) {
                    log.append(line).append('\n');
                }
            }
        }

        boolean finished = process.waitFor(processTimeoutSec, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("FFmpeg timeout sau " + processTimeoutSec + "s.");
        }
        int code = process.exitValue();
        if (code != 0) {
            System.out.println("[FFmpeg] failed code=" + code + " log=\n" + log);
            throw new IllegalStateException("FFmpeg exit code " + code + ". Kiểm tra file đầu vào hoặc cài FFmpeg.");
        }
    }

    private String probeDuration(Path mp3Path) {
        try {
            List<String> cmd = List.of(
                    ffmpegPath,
                    "-i", mp3Path.toAbsolutePath().toString()
            );
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder log = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.append(line).append('\n');
                }
            }
            process.waitFor(30, TimeUnit.SECONDS);
            // Duration: 00:03:45.12
            String text = log.toString();
            int idx = text.indexOf("Duration:");
            if (idx < 0) return "0:00";
            int start = idx + "Duration:".length();
            int end = text.indexOf(',', start);
            if (end < 0) end = Math.min(start + 20, text.length());
            String raw = text.substring(start, end).trim(); // HH:MM:SS.xx
            return normalizeDuration(raw);
        } catch (Exception e) {
            return "0:00";
        }
    }

    private String normalizeDuration(String raw) {
        try {
            String[] parts = raw.split(":");
            if (parts.length < 3) return "0:00";
            int h = Integer.parseInt(parts[0].trim());
            int m = Integer.parseInt(parts[1].trim());
            double secD = Double.parseDouble(parts[2].trim());
            int totalSec = h * 3600 + m * 60 + (int) Math.round(secD);
            int mm = totalSec / 60;
            int ss = totalSec % 60;
            return mm + ":" + String.format("%02d", ss);
        } catch (Exception e) {
            return "0:00";
        }
    }

    private void ensureFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(ffmpegPath, "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            boolean ok = p.waitFor(10, TimeUnit.SECONDS);
            if (!ok || p.exitValue() != 0) {
                throw new IllegalStateException("FFmpeg không khả dụng. Cài FFmpeg và thêm vào PATH (hoặc cấu hình audio.ffmpeg-path).");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Không tìm thấy FFmpeg (" + ffmpegPath + "). Cài FFmpeg rồi restart backend.", e);
        }
    }

    private static String extensionOf(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String name) {
        if (name == null) return null;
        int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : name;
    }

    private static void safeDelete(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private static void safeDeleteDir(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return;
        try (var stream = Files.list(dir)) {
            stream.forEach(AudioCompressService::safeDelete);
        } catch (Exception ignored) {
        }
        safeDelete(dir);
    }
}
