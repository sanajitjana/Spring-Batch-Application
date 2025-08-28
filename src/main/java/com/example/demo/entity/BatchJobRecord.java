package com.example.demo.entity;

import com.example.demo.enums.JobState;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "batch_jobs")
public class BatchJobRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jobId;        // UUID string we generate

    @Enumerated(EnumType.STRING)
    private JobState state;      // PENDING, RUNNING, COMPLETED, FAILED

    private String s3Key;
    private String resultS3Key;
    private String errorMessage;

    private Instant createdAt;
    private Instant updatedAt;
}
