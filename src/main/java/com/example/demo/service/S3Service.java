package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3;

    @Value("${app.s3.bucket}")
    private String bucket;

    public void uploadFile(String key, InputStream inputStream, long size, String contentType) {
        PutObjectRequest por = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(size)
                .contentType(contentType)
                .build();

        s3.putObject(por, RequestBody.fromInputStream(inputStream, size));
    }

    public Path downloadToTempFile(String key) throws IOException {

        Path temp = Files.createTempFile("s3input-", ".csv");
        GetObjectRequest g = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3.getObject(g, ResponseTransformer.toFile(temp));
        return temp;
    }
}
