package com.example.Back_End.repository;

import com.example.Back_End.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderByCreatedAtDesc();
    List<Feedback> findByUserEmailOrderByCreatedAtDesc(String userEmail);
    List<Feedback> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
