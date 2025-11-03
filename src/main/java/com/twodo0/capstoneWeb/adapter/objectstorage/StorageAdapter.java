package com.twodo0.capstoneWeb.adapter.objectstorage;

import com.twodo0.capstoneWeb.port.ObjectStoragePort;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

@Component
@RequiredArgsConstructor
public class StorageAdapter implements ObjectStoragePort {

    private final MinioClient minioClient;

    @Override
    public void put(String bucket, String key,  byte[] data, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .contentType(
                                    (contentType != null && !contentType.isBlank())
                                        ? contentType : "application/octet-stream"
                            )
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .build()
            );
        } catch (ErrorResponseException e) {
            throw new RuntimeException("Minio " + e.errorResponse().code()
                    + ": " + e.errorResponse().message(), e);
        } catch (Exception e) {
            throw new RuntimeException("Minio putObject failed: " + e.getMessage(), e);
        }
    }


}
