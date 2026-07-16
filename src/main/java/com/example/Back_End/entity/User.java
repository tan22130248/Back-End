package com.example.Back_End.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String fullName;

    @Column(nullable = false, unique = true, length = 255)
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Pattern(regexp = ".*@gmail\\..*", message = "Email phải có đuôi @gmail.com")
    private String email;

    @Column(nullable = false)
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 7, max = 255, message = "Mật khẩu phải từ 7 ký tự trở lên")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d).+$", message = "Mật khẩu phải có ít nhất 1 chữ hoa và 1 số")
    private String password;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(name = "plan_type", nullable = false, length = 20)
    private String planType = "FREE";

    @Column(name = "plan_expires_at")
    private java.time.LocalDate planExpiresAt;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled = false;

    @Column(length = 20)
    private String phone;

    @Column(name = "birthday")
    private java.time.LocalDate birthday;

    @Column(length = 10)
    private String gender;

    @Column(length = 500)
    private String avatar;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.role == null) this.role = "USER";
        if (this.planType == null) this.planType = "FREE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public java.time.LocalDate getPlanExpiresAt() {
        return planExpiresAt;
    }

    public void setPlanExpiresAt(java.time.LocalDate planExpiresAt) {
        this.planExpiresAt = planExpiresAt;
    }

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public java.time.LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(java.time.LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
