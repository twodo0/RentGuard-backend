package com.twodo0.capstoneWeb.port;

public interface ObjectStoragePort {
    void put(String bucket, String key, byte[] data, String contentType);
}
