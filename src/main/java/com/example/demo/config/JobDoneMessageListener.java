package com.example.demo.config;

import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRepo;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class JobDoneMessageListener implements JobExecutionListener {

    private final BatchJobRepo jobRepo;

    public JobDoneMessageListener(BatchJobRepo jobRepo) {
        this.jobRepo = jobRepo;
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        var optional = jobRepo.findByJobId(jobId);
        if (optional.isEmpty()) return;
        var rec = optional.get();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            rec.setState(JobState.COMPLETED);
            // If you produce a result (e.g., aggregated report), you can store the result path
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
