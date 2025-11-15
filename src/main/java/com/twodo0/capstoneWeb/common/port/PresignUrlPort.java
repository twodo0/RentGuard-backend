package com.twodo0.capstoneWeb.common.port;

public interface PresignUrlPort {
    String presignGet(String bucket, String key);
    String presignPut(String bucket, String key);
}
