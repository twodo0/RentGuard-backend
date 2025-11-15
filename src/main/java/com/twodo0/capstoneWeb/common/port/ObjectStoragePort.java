package com.twodo0.capstoneWeb.common.port;

public interface ObjectStoragePort {
    void put(String bucket, String key, byte[] data, String contentType);
}
