package com.favian.ai_assistant.kafka.consumer;


import com.favian.ai_assistant.AIRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import shared.dto.AiMessageRequest;


@Service
public class MessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
    private final AIRoutingService routingService;
    private final KafkaTemplate<String, AiMessageRequest> kafkaTemplate;

    public MessageConsumer(AIRoutingService routingService, KafkaTemplate<String, AiMessageRequest> kafkaTemplate) {
        this.routingService = routingService;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${app.kafka.topic.answers}")
    private String topicNameResponse;

    @KafkaListener(
            topics = "${app.kafka.topic.messages}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMessage(AiMessageRequest aiMessageRequest) {
        log.info("✅ RECEIVED MESSAGE: {}", aiMessageRequest);
        String answer = routingService.process(aiMessageRequest.text());
        log.info("✅ ANSWER: {}", answer);
        AiMessageRequest response = new AiMessageRequest(aiMessageRequest.requestId(), answer);

        kafkaTemplate.send(topicNameResponse, response);
        log.info("response send to topic: {}", topicNameResponse);
    }
}