package com.example.Back_End.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class S3Config {

    private static String resolveEnvPath() {
        String cwd = System.getProperty("user.dir");
        String[] candidates = {
                cwd + "/.env",
                cwd + "/Back-End/.env",
                cwd + "/../.env"
        };
        for (String path : candidates) {
            Path p = Paths.get(path);
            if (Files.isRegularFile(p)) {
                return p.toString();
            }
        }
        return null;
    }

    private static Properties loadEnvProps() {
        Properties props = new Properties();
        String envPath = resolveEnvPath();
        if (envPath == null) return props;
        try (BufferedReader reader = new BufferedReader(new FileReader(envPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    String k = line.substring(0, eq).trim();
                    String v = line.substring(eq + 1).trim();
                    if (v.startsWith("\"") && v.endsWith("\"") && v.length() >= 2) v = v.substring(1, v.length() - 1);
                    if (v.startsWith("'") && v.endsWith("'") && v.length() >= 2) v = v.substring(1, v.length() - 1);
                    props.setProperty(k, v);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return props;
    }

    private static String getEnv(String key, String defaultValue) {
        String v = System.getenv(key);
        if (v != null && !v.isBlank()) return v;
        Properties props = loadEnvProps();
        String p = props.getProperty(key);
        return (p != null && !p.isBlank()) ? p : defaultValue;
    }

    @Value("${cloudflare.r2.endpoint}")
    private String endpoint;

    @Value("${cloudflare.r2.access-key:}")
    private String accessKey;

    @Value("${cloudflare.r2.secret-key:}")
    private String secretKey;

    @Value("${cloudflare.r2.bucket:web-audio}")
    private String bucket;

    @Value("${cloudflare.r2.public-domain:}")
    private String publicDomain;

    @Bean
    public S3Client s3Client() {
        String ak = (accessKey != null && !accessKey.isBlank()) ? accessKey : getEnv("R2_ACCESS_KEY", "");
        String sk = (secretKey != null && !secretKey.isBlank()) ? secretKey : getEnv("R2_SECRET_KEY", "");
        String ep = (endpoint != null && !endpoint.isBlank()) ? endpoint : getEnv("R2_ENDPOINT", "https://ce93c7c795116afd02f973d455765f75.r2.cloudflarestorage.com");

        if (ak.isBlank() || sk.isBlank()) {
            throw new IllegalStateException("Missing R2 credentials. Set R2_ACCESS_KEY / R2_SECRET_KEY in env or Back-End/.env");
        }

        System.out.println("[R2] endpoint=" + ep + ", bucket=" + bucket + ", accessKey=" + ak.substring(0, 8) + "...");

        return S3Client.builder()
                .endpointOverride(URI.create(ep))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ak, sk)
                ))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}

