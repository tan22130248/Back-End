package com.example.Back_End.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AudioRequest {

    @NotBlank(message = "Tên truyện không được để trống")
    @Size(max = 255, message = "Tên truyện tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Tác giả không được để trống")
    @Size(max = 100, message = "Tác giả tối đa 100 ký tự")
    private String author;

    @NotBlank(message = "Thể loại không được để trống")
    @Size(max = 50, message = "Thể loại tối đa 50 ký tự")
    private String genre;

    @NotBlank(message = "Thời lượng không được để trống")
    @Size(max = 20, message = "Thời lượng tối đa 20 ký tự")
    private String duration;

    @NotNull(message = "Kích thước file không được để trống")
    private Long fileSize;

    @NotBlank(message = "Link audio không được để trống")
    private String audioUrl;

    private String coverImageUrl;

    public AudioRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
}
