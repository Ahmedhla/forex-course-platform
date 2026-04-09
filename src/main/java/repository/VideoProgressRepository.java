package com.forex.repository;

import com.forex.model.VideoProgress;
import com.forex.model.User;
import com.forex.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface VideoProgressRepository extends JpaRepository<VideoProgress, Long> {
    Optional<VideoProgress> findByUserAndVideo(User user, Video video);
    List<VideoProgress> findByUser(User user);
}