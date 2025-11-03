package com.twodo0.capstoneWeb.repository;

import com.twodo0.capstoneWeb.domain.ImageMeta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<ImageMeta, Long> {
    Page<ImageMeta> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

