package com.example.demo.repositoty;

import com.example.demo.entity.JobQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobQueueRepository extends JpaRepository<JobQueue, Long> {

    List<JobQueue> findTop10ByConsumedFalseOrderByIdAsc();
}
