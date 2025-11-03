package com.twodo0.capstoneWeb.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// MinioProperties를 실제로 쓰려면 아래처럼 활성화해야 함

@Configuration
@EnableConfigurationProperties(MinioProperties.class) // MinioProperties에  바인딩 활성화
public class MinioConfig {
    @Bean
    public MinioClient minioClient(MinioProperties p) {
        return MinioClient.builder()
                .endpoint(p.getEndPoint())
                .credentials(p.getAccesskey(), p.getSecretkey())
                .build();
    }
    // MinioClient를 만들어 어디서든 주입해서 재사용 가능
}
