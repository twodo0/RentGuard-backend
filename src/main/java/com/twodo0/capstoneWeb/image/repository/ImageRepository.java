package com.twodo0.capstoneWeb.image.repository;

import com.twodo0.capstoneWeb.image.domain.ImageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<ImageMeta, Long> {
    Page<ImageMeta> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

