package com.example.demo.controller;

import com.example.demo.entity.BatchJob;
import com.example.demo.entity.JobQueue;
import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRepo;
import com.example.demo.repositoty.JobQueueRepo;
import com.example.demo.service.LocalStorageService;
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

    private final BatchJobRepo jobRepo;
    private final JobQueueRepo queueRepo;
    private final LocalStorageService storage;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) throws IOException{
        String jobId = UUID.randomUUID().toString();

        // upload file on local storage
        String localPath = storage.saveFile(jobId, file);

        // create job record on db
        BatchJob batchJob = new BatchJob();
        batchJob.setJobId(jobId);
        batchJob.setState(JobState.PENDING);
        batchJob.setS3Key(localPath);
        batchJob.setCreatedAt(Instant.now());
        batchJob.setUpdatedAt(Instant.now());
        jobRepo.save(batchJob);

        // push to queue table
        JobQueue jobQueue = new JobQueue();
        jobQueue.setJobId(jobId);
        jobQueue.setFilePath(localPath);
        queueRepo.save(jobQueue);

        return ResponseEntity.ok(Map.of("message", "File uploaded successfully", "jobId", jobId));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<BatchJob> getStatus(@PathVariable String jobId) {
        BatchJob batchJob = jobRepo.findByJobId(jobId).orElseThrow(() -> new RuntimeException("Job not found"));
        return ResponseEntity.ok(batchJob);
    }
}
