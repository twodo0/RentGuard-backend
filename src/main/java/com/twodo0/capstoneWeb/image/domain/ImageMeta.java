package com.twodo0.capstoneWeb.image.domain;

import com.twodo0.capstoneWeb.common.domain.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "imagemeta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageMeta extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contentType;
    private String bucket;
    @Column(name = "object_key")
    private String key; // MinIO object key
    private Integer width;
    private Integer height;
}
