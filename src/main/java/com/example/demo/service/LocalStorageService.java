package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class LocalStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String saveFile(String jobId, MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir, jobId);
        Files.createDirectories(dir);
        Path target = dir.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString(); // return path as "s3Key"
    }

    public Path getFilePath(String storedPath) {
        return Paths.get(storedPath);
    }
}
