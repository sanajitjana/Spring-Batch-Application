package com.example.demo.config;

import com.example.demo.entity.BatchJob;
import com.example.demo.entity.JobQueue;
import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRepo;
import com.example.demo.repositoty.JobQueueRepo;
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
public class DbQWorker {

    private final JobQueueRepo queueRepo;
    private final BatchJobRepo jobRepo;
    private final JobLauncher jobLauncher;
    private final Job importJob;

    public DbQWorker(JobQueueRepo queueRepo,
                     BatchJobRepo jobRepo,
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
            BatchJob rec = jobRepo.findByJobId(msg.getJobId()).orElseThrow();

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
                    .addString("filePath", rec.getS3Key()) // local file path
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
