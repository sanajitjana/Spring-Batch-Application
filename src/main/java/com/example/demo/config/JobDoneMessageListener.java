package com.example.demo.config;

import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRepo;
import com.example.demo.service.S3Service;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobDoneMessageListener extends JobExecutionListenerSupport {

    private final BatchJobRepo jobRepo;
    private final S3Service s3Service;

    public JobDoneMessageListener(BatchJobRepo jobRepo, S3Service s3Service) {
        this.jobRepo = jobRepo;
        this.s3Service = s3Service;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        var optional = jobRepo.findByJobId(jobId);
        if (optional.isEmpty()) return;
        var rec = optional.get();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            rec.setState(JobState.COMPLETED);
            // If you produce a result (e.g., aggregated report), upload to S3 and set resultS3Key
            // rec.setResultS3Key("results/" + jobId + "/report.csv");
        } else {
            rec.setState(JobState.FAILED);
            StringBuilder sb = new StringBuilder();
            for (Throwable t : jobExecution.getAllFailureExceptions()) {
                sb.append(t.getMessage()).append("\n");
            }
            rec.setErrorMessage(sb.toString());
        }
        rec.setUpdatedAt(Instant.now());
        jobRepo.save(rec);

        // Optionally push notification / webhook / email here
    }
}
