package com.example.Back_End.repository;

import com.example.Back_End.entity.PlaylistAudio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistAudioRepository extends JpaRepository<PlaylistAudio, Long> {
    Optional<PlaylistAudio> findByPlaylistIdAndAudioId(Long playlistId, Long audioId);
    List<PlaylistAudio> findByPlaylistId(Long playlistId);
    void deleteByPlaylistIdAndAudioId(Long playlistId, Long audioId);
}
