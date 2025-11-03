package com.twodo0.capstoneWeb.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

// !!-- 설정값 바인딩 --!!
@Getter @Setter
@ConfigurationProperties(prefix = "minio") // application.yml의 minio.*을 여기에 바인딩
public class MinioProperties {

    private String accesskey; //minio.accesskey
    private String secretkey; // minio.secretkey
    private String endPoint; // http://localhost:9000
    private String rawBucket;
    private String heatmapBucket;

    private int presignTtlSeconds = 3600; // minio.presign-expire-seconds (기본 3600)
}