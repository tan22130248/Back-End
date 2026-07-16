package com.example.Back_End.repository;

import com.example.Back_End.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {
    Optional<Like> findByUserEmailAndAudioId(String userEmail, Long audioId);
    void deleteByUserEmailAndAudioId(String userEmail, Long audioId);
    java.util.List<Like> findByUserEmail(String userEmail);
    java.util.List<Like> findByUserEmailOrderByCreatedAtDesc(String userEmail);
}
