package com.example.demo.config;

import com.example.demo.entity.BatchJobRecord;
import com.example.demo.entity.JobQueue;
import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRecordRepository;
import com.example.demo.repositoty.JobQueueRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class LocalQueueWorker {

    private final JobQueueRepository queueRepo;
    private final BatchJobRecordRepository jobRepo;
    private final JobLauncher jobLauncher;
    private final Job importJob;

    public LocalQueueWorker(JobQueueRepository queueRepo,
                            BatchJobRecordRepository jobRepo,
                            @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
                            @Qualifier("importJob") Job importJob) {
        this.queueRepo = queueRepo;
        this.jobRepo = jobRepo;
        this.jobLauncher = jobLauncher;
        this.importJob = importJob;
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        List<JobQueue> messages = queueRepo.findTop10ByConsumedFalseOrderByIdAsc();

        for (JobQueue msg : messages) {
            BatchJobRecord rec = jobRepo.findByJobId(msg.getJobId()).orElseThrow();

            if (rec.getState() != JobState.PENDING) {
                msg.setConsumed(true);
                queueRepo.save(msg);
                continue;
            }

            rec.setState(JobState.RUNNING);
            rec.setUpdatedAt(Instant.now());
            jobRepo.save(rec);

            JobParameters params = new JobParametersBuilder()
                    .addString("jobId", rec.getJobId())
                    .addString("s3Key", rec.getS3Key()) // now local path
                    .addLong("ts", System.currentTimeMillis())
                    .toJobParameters();

            try {
                jobLauncher.run(importJob, params);
                msg.setConsumed(true);
                queueRepo.save(msg);
            } catch (Exception e) {
                rec.setState(JobState.FAILED);
                rec.setErrorMessage(e.getMessage());
                rec.setUpdatedAt(Instant.now());
                jobRepo.save(rec);
            }
        }
    }
}
