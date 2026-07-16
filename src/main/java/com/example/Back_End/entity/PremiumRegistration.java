package com.example.Back_End.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "premium_registrations")
public class PremiumRegistration {

    public enum Status {
        PENDING, COMPLETED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(name = "user_name", nullable = false, length = 255)
    private String userName;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "price", nullable = false, length = 50)
    private String price;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status = Status.PENDING;

    @Column(name = "receipt_image", length = 500)
    private String receiptImage;

    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getReceiptImage() { return receiptImage; }
    public void setReceiptImage(String receiptImage) { this.receiptImage = receiptImage; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
