package com.example.demo.repositoty;

import com.example.demo.entity.BatchJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BatchJobRepo extends JpaRepository<BatchJob, Long> {

    Optional<BatchJob> findByJobId(String jobId);
}
