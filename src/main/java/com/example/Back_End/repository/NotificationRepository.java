package com.example.Back_End.repository;

import com.example.Back_End.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<Notification> findByUserEmailAndReadOrderByCreatedAtDesc(String userEmail, boolean read);
    long countByUserEmailAndRead(String userEmail, boolean read);
}
