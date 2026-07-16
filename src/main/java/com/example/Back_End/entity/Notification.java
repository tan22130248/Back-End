package com.example.Back_End.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "audio_id", nullable = false)
    private Long audioId;

    @Column(name = "audio_title", nullable = false, length = 255)
    private String audioTitle;

    @Column(name = "audio_author", nullable = false, length = 100)
    private String audioAuthor;

    @Column(name = "audio_genre", nullable = false, length = 50)
    private String audioGenre;

    @Column(name = "audio_duration", nullable = false, length = 20)
    private String audioDuration;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public Long getAudioId() { return audioId; }
    public void setAudioId(Long audioId) { this.audioId = audioId; }
    public String getAudioTitle() { return audioTitle; }
    public void setAudioTitle(String audioTitle) { this.audioTitle = audioTitle; }
    public String getAudioAuthor() { return audioAuthor; }
    public void setAudioAuthor(String audioAuthor) { this.audioAuthor = audioAuthor; }
    public String getAudioGenre() { return audioGenre; }
    public void setAudioGenre(String audioGenre) { this.audioGenre = audioGenre; }
    public String getAudioDuration() { return audioDuration; }
    public void setAudioDuration(String audioDuration) { this.audioDuration = audioDuration; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
