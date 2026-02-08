package com.favian.ai_assistant.kafka.consumer;


import com.favian.ai_assistant.AIRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;



@Service
public class MessageConsumer {
    private static final Logger log = LoggerFactory.getLogger(MessageConsumer.class);
    private final AIRoutingService routingService;

    public MessageConsumer(AIRoutingService routingService) {
        this.routingService = routingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.messages}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeMessage(String question) {
        log.info("✅ RECEIVED MESSAGE: {}", question);
        String answer = routingService.process(question);
        log.info("✅ ANSWER: {}", answer);
    }
}