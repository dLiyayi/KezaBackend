package com.keza.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageConfig storageConfig;

    @PostConstruct
    public void initBuckets() {
        if (storageConfig.getBuckets() != null) {
            storageConfig.getBuckets().values().forEach(this::ensureBucketExists);
        }
    }

    @Override
    public String upload(String bucket, String key, InputStream inputStream,
                         long contentLength, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(contentLength)
                        .build(),
                RequestBody.fromInputStream(inputStream, contentLength));

        log.info("Uploaded file to s3://{}/{}", bucket, key);
        return key;
    }

    @Override
    public InputStream download(String bucket, String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
    }

    @Override
    public void delete(String bucket, String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
        log.info("Deleted file s3://{}/{}", bucket, key);
    }

    @Override
    public String generatePresignedUrl(String bucket, String key, Duration expiration) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(storageConfig.getEndpoint()))
                .region(software.amazon.awssdk.regions.Region.of(storageConfig.getRegion()))
                .build()) {

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build())
                    .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Bucket '{}' already exists", bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("Created bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("Could not verify/create bucket '{}': {}", bucket, e.getMessage());
        }
    }
}
