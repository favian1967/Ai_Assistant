package com.favian.ai_assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class AiController {

    private final AIRoutingService routingService;

    public AiController(AIRoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return routingService.process(question);
    }
}
