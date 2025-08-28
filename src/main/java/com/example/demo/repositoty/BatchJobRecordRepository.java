package com.example.demo.repositoty;

import com.example.demo.entity.BatchJobRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchJobRecordRepository extends JpaRepository<BatchJobRecord, Long> {

    Optional<BatchJobRecord> findByJobId(String jobId);
}
