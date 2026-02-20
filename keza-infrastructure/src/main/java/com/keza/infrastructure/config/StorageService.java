package com.keza.infrastructure.config;

import java.io.InputStream;
import java.time.Duration;

public interface StorageService {

    String upload(String bucket, String key, InputStream inputStream, long contentLength, String contentType);

    InputStream download(String bucket, String key);

    void delete(String bucket, String key);

    String generatePresignedUrl(String bucket, String key, Duration expiration);
}
