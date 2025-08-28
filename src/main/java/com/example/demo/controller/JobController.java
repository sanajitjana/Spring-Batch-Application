package com.example.demo.controller;

import com.example.demo.entity.BatchJobRecord;
import com.example.demo.entity.JobQueue;
import com.example.demo.enums.JobState;
import com.example.demo.model.User;
import com.example.demo.repositoty.BatchJobRecordRepository;
import com.example.demo.repositoty.JobQueueRepository;
import com.example.demo.service.LocalStorageService;
import com.example.demo.service.S3Service;
import com.example.demo.service.SqsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final S3Service s3Service;
    private final BatchJobRecordRepository jobRepo;
    private final SqsSender sqsSender;
    private final JobQueueRepository queueRepo;
    private final LocalStorageService storage;

    @PostMapping("/local/upload")
    public ResponseEntity<Map<String, String>> uploadFileOnLocal(@RequestParam("file") MultipartFile file) throws IOException{
        String jobId = UUID.randomUUID().toString();

        // upload file on local storage
        String localPath = storage.saveFile(jobId, file);

        // create job record on db
        BatchJobRecord batchJobRecord = new BatchJobRecord();
        batchJobRecord.setJobId(jobId);
        batchJobRecord.setState(JobState.PENDING);
        batchJobRecord.setS3Key(localPath);
        batchJobRecord.setCreatedAt(Instant.now());
        batchJobRecord.setUpdatedAt(Instant.now());
        jobRepo.save(batchJobRecord);

        // push to queue table
        JobQueue jobQueue = new JobQueue();
        jobQueue.setJobId(jobId);
        jobQueue.setFilePath(localPath);
        queueRepo.save(jobQueue);

        return ResponseEntity.ok(Map.of("message", "File uploaded successfully", "jobId", jobId));
    }

    @PostMapping("/s3/upload")
    public ResponseEntity<Map<String, String>> uploadFileOnS3(
            @RequestParam("file") MultipartFile file,
            User user) throws IOException {

        String jobId = UUID.randomUUID().toString();
        String s3Key = String.format("uploads/%s/%s", jobId, file.getOriginalFilename());

        // upload to s3
        s3Service.uploadFile(s3Key, file.getInputStream(), file.getSize(), file.getContentType());

        // create DB job record
        BatchJobRecord batchJobRecord = new BatchJobRecord();
        batchJobRecord.setJobId(jobId);
        batchJobRecord.setState(JobState.PENDING);
        batchJobRecord.setS3Key(s3Key);
        batchJobRecord.setCreatedAt(Instant.now());
        batchJobRecord.setUpdatedAt(Instant.now());
        jobRepo.save(batchJobRecord);

        // push message to queue (event driven)
        sqsSender.sendJobMessage(jobId, s3Key);

        return ResponseEntity.ok(Map.of("message", "File uploaded successfully", "jobId", jobId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<BatchJobRecord> getStatus(@PathVariable String jobId) {
        BatchJobRecord batchJobRecord = jobRepo.findByJobId(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        return ResponseEntity.ok(batchJobRecord);
    }
}
