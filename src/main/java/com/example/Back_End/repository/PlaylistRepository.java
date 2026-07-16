package com.example.Back_End.repository;

import com.example.Back_End.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByUserEmailOrderByIdDesc(String userEmail);
}
