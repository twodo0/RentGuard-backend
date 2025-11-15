package com.twodo0.capstoneWeb.common.adapter.presign;

import com.twodo0.capstoneWeb.common.config.MinioProperties;
import com.twodo0.capstoneWeb.common.port.PresignUrlPort;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("minio") // 애플리케이션이 minio 프로필로 실행될 떄만 이 빈이 등록됨
@RequiredArgsConstructor
@Slf4j
public class MinioPresignAdapter implements PresignUrlPort {

    private final MinioClient minio; // Config의 bean이 주입됨
    private final MinioProperties props; // yml 바인딩 값 주입


    @Override
    public String presignGet(String bucket, String key) {
        try {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("bucket is null or blank");
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key is null or blank");
            }

            int seconds = props.getPresignTtlSeconds();
            return minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET) // GET 만 허용
                            .bucket(bucket) // 대상 버킷
                            .object(key) // 대상 오프젝트 키
                            .expiry(seconds) // 유효시간(초)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO presign failed for bucket={}, key={}, cause={}",
                    bucket, key, e.toString());
            throw new RuntimeException("MinIO presign failed for key = " + key, e);
        }
    }

    @Override
    public String presignPut(String bucket, String key) {
        try {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("bucket is null or blank");
            }
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key is null or blank");
            }

            int seconds = props.getPresignTtlSeconds();
            // Content-Type은 PUT 요청 시 꼭 헤더로 같이 전송해야 함
            return minio.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(key)
                            .expiry(seconds)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("MinIO presign failed for key = " + key, e);
        }
    }

}
