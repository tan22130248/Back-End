package com.example.Back_End.repository;

import com.example.Back_End.entity.Audio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AudioRepository extends JpaRepository<Audio, Long> {
    Optional<Audio> findByTitle(String title);

    @Modifying
    @Transactional
    @Query("UPDATE Audio a SET a.viewCount = COALESCE(a.viewCount, 0) + 1 WHERE a.id = :id")
    int incrementViewCount(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Audio a SET a.likeCount = COALESCE(a.likeCount, 0) + 1 WHERE a.id = :id")
    int incrementLikeCount(Long id);

    @Modifying
    @Transactional
    @Query("UPDATE Audio a SET a.likeCount = GREATEST(COALESCE(a.likeCount, 0) - 1, 0) WHERE a.id = :id")
    int decrementLikeCount(Long id);
}
