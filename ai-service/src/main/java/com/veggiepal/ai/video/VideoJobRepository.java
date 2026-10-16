package com.veggiepal.ai.video;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VideoJobRepository extends JpaRepository<VideoJob,Long>{Optional<VideoJob> findByIdAndUserId(Long id,Long userId);}
