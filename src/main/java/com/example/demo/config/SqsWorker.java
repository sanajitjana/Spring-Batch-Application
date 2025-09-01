package com.example.demo.config;

import com.example.demo.entity.BatchJob;
import com.example.demo.enums.JobState;
import com.example.demo.repositoty.BatchJobRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Instant;
import java.util.Map;

@Component
public class SqsWorker {

    private final SqsClient sqs;
    private final String queueUrl;
    private final BatchJobRepo jobRepo;
    private final JobLauncher jobLauncher;          // async launcher bean
    private final Job importJob;                    // your Spring Batch Job bean
    private final ObjectMapper mapper = new ObjectMapper();

    public SqsWorker(SqsClient sqs, @Value("${app.sqs.queue-url}") String queueUrl,
                     BatchJobRepo jobRepo,
                     @Qualifier("asyncJobLauncher") JobLauncher jobLauncher,
                     @Qualifier("importJob") Job importJob) {
        this.sqs = sqs; this.queueUrl = queueUrl; this.jobRepo = jobRepo;
        this.jobLauncher = jobLauncher; this.importJob = importJob;
    }

    // long-poll with waitTimeSeconds=20
    @Scheduled(fixedDelayString = "${app.sqs.poll-interval-ms:5000}")
    public void poll() {
        ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(5)
                .waitTimeSeconds(20)
                .build();

        var resp = sqs.receiveMessage(req);
        for (Message msg : resp.messages()) {
            try {
                Map<String,String> body = mapper.readValue(
                        msg.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
                );
                String jobId = body.get("jobId");
                String s3Key = body.get("s3Key");

                // check DB state — protect against double processing
                BatchJob rec = jobRepo.findByJobId(jobId)
                        .orElseThrow(() -> new IllegalStateException("No job record for " + jobId));

                if (rec.getState() != JobState.PENDING) {
                    // already started or finished — delete message and continue
                    deleteMessage(msg);
                    continue;
                }

                // mark RUNNING
                rec.setState(JobState.RUNNING);
                rec.setUpdatedAt(Instant.now());
                jobRepo.save(rec);

                // launch the Spring Batch job asynchronously
                JobParameters params = new JobParametersBuilder()
                        .addString("jobId", jobId)
                        .addString("s3Key", s3Key)
                        .addLong("ts", System.currentTimeMillis())
                        .toJobParameters();

                jobLauncher.run(importJob, params); // returns fast because async launcher

                // delete SQS message (we’ll rely on job listener to mark COMPLETE/FAILED)
                deleteMessage(msg);
            } catch (Exception ex) {
                // log; optionally leave message for retries or move to DLQ
                // do not delete; SQS visibility timeout will expire and message will be retried
            }
        }
    }

    private void deleteMessage(Message msg) {
        sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(msg.receiptHandle())
                .build());
    }
}
