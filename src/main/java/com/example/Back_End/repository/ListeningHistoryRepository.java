package com.example.Back_End.repository;

import com.example.Back_End.entity.ListeningHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListeningHistoryRepository extends JpaRepository<ListeningHistory, Long> {
    List<ListeningHistory> findByUserEmailOrderByListenedAtDesc(String userEmail);
    void deleteByUserEmailAndAudioId(String userEmail, Long audioId);
}
