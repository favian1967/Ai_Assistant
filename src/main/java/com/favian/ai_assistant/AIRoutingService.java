package com.favian.ai_assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Service;

@Service
public class AIRoutingService {

    private static final Logger log =
            LoggerFactory.getLogger(AIRoutingService.class);

    private final OllamaChatModel chatModel;
    private final AIResponseService responseService;

    private static final String KNOWLEDGE = """
GET_ACCOUNT_INFO
GET_ACCOUNT_TYPES
GET_ACCOUNT_BALANCE
GET_ACCOUNT_LIMITS
GET_ACCOUNT_STATUS
GET_CURRENCIES
GET_CARD_ISSUE_INFO
GET_CARD_TYPES
BLOCK_CARD_INFO
UNBLOCK_CARD_INFO
GET_CARD_BALANCE
GET_CARD_LIMITS
GET_CARD_STATUS
DEPOSIT_INFO
WITHDRAW_INFO
TRANSFER_INFO
GET_TRANSACTION_HISTORY
IDEMPOTENCY_INFO
GET_LIMITS_INFO
REGISTRATION_INFO
LOGIN_INFO
LOGOUT_INFO
CHANGE_PASSWORD_INFO
EMAIL_CONFIRMATION_INFO
SECURITY_INFO
SYSTEM_INFO
WORKING_HOURS
ESCALATE_TO_OPERATOR
FEES_INFO
CONTACT_INFO
MOBILE_APP_INFO
""";

    public AIRoutingService(
            OllamaChatModel chatModel,
            AIResponseService responseService
    ) {
        this.chatModel = chatModel;
        this.responseService = responseService;
    }

    public String process(String question) {

        String route = KeywordFinder.route(question);

        if (!route.equals("CALL_AI")) {
            return responseService.getResponse(route);
        }

        String method = callAI(question);

        return responseService.getResponse(method);
    }

    private String callAI(String question) {

        String prompt = """
Ты классификатор банковских запросов.

Выбери РОВНО ОДИН метод из списка.
Если нет совпадения — ESCALATE_TO_OPERATOR.

Методы:
""" + KNOWLEDGE + """

Вопрос:
""" + question;

        log.info("Prompt length: {}", prompt.length());

        Prompt promptObj = new Prompt(
                prompt,
                OllamaOptions.builder()
                        .numPredict(12)
                        .temperature(0.0)
                        .build()
        );

        var response = chatModel.call(promptObj);

        String raw = response
                .getResult()
                .getOutput()
                .getText();

        return sanitize(raw);
    }

    private String sanitize(String raw) {

        if (raw == null) return "ESCALATE_TO_OPERATOR";

        return raw.trim()
                .replaceAll("[^A-Z_]", "");
    }
}
