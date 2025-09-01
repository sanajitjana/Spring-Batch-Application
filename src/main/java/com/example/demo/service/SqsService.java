package com.example.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SqsService {

    private final SqsClient sqs;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${app.sqs.queue-url}")
    private String queueUrl;

    public void sendJobMessage(String jobId, String s3Key) throws JsonProcessingException {
        Map<String, String> body = Map.of("jobId", jobId, "s3Key", s3Key);
        String message = mapper.writeValueAsString(body);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .build();

        sqs.sendMessage(request);
    }
}
