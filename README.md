<a id="readme-top"></a>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About the Project</a>
      <ul>
        <li><a href="#built-with">Tech Stack</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation and Launch</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#configuration">Configuration</a></li>
    <li><a href="#architecture">Architecture</a></li>
    <li><a href="#api">API</a></li>
    <li><a href="#testing">Testing</a></li>
    <li><a href="#roadmap">Roadmap</a></li>
    <li><a href="#contact">Contact</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About the Project

**AI Assistant** is a microservice assistant based on Spring Boot and Ollama AI for processing user requests in a banking system. The service works in conjunction with **BankSystem**, receives questions through Kafka, classifies them using AI, and returns ready-made answers.

The project uses a **hybrid approach**:
- **KeywordFinder** — fast classification by keywords (~30 categories)
- **Ollama AI** — classification of complex/ambiguous requests through local LLM (model `gemma3:4b`)
- **AIResponseService** — returns ready-made template responses

This ensures low latency for popular requests and flexibility for non-standard formulations.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Tech Stack

* [![Java][java-shield]][java-url]
* [![Spring Boot][spring-boot-shield]][spring-boot-url]
* ![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
* ![Ollama](https://img.shields.io/badge/Ollama-gemma3:4b-000000?style=for-the-badge&logo=ollama&logoColor=white)
* ![Kafka](https://img.shields.io/badge/Kafka-Event%20Streaming-231F20?style=for-the-badge&logo=apachekafka)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Features

- **Event-driven architecture** via Apache Kafka
- **Hybrid classification** (keyword matching + AI)
- **Ready-made response templates** for 30+ types of banking requests
- **Low latency** due to local LLM (Ollama)
- **Request-Reply pattern** via Kafka topics

<!-- GETTING STARTED -->
## Getting Started

### Prerequisites

- **Java 21**
- **Ollama** with loaded model `gemma3:4b`
- **Apache Kafka** (running on `localhost:9092`)
- **BankSystem** (main microservice) - optional for integration testing

### Installation and Launch

#### 1. Install Ollama and Model

```bash
# Install Ollama from the official website: https://ollama.ai

# Download the gemma3:4b model
ollama pull gemma3:4b

# Check that Ollama is running (default at http://localhost:11434)
curl http://localhost:11434
```

#### 2. Launch Kafka

If Kafka is not yet running, use Docker:

```bash
docker run -d \
  --name ai_assistant_kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_LOG_DIRS=/tmp/kraft-combined-logs \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  apache/kafka:latest
```

#### 3. Launch Application

```bash
# Compile the project
./gradlew build

# Run the application
./gradlew bootRun
```

The application will start on port **8081**.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

### Standalone Mode

For testing without Kafka, you can send a direct HTTP request:

```bash
curl "http://localhost:8081/ask?question=how%20to%20open%20an%20account"
```

**Response:**
```
To open an account:
1. Log in to your personal account
2. Section 'Accounts' → 'Create account'
3. Select type: checking (CHECKING), savings (SAVINGS), or deposit (DEPOSIT)
4. Select currency: RUB, USD, or EUR
5. Confirm creation

The account will be created instantly with a unique number.
```

### Kafka Mode (Production)

The service automatically:
1. **Listens to topic** `ai_messages` (groupId: `ai_messages_group_v3`)
2. **Receives** messages like:
   ```json
   {
     "requestId": "uuid-123",
     "text": "how to block a card?"
   }
   ```
3. **Processes** through `AIRoutingService`
4. **Sends response** to topic `bank_ai_answers`:
   ```json
   {
     "requestId": "uuid-123",
     "text": "Blocking a card in case of loss/theft:\n1. Personal account → 'My cards'..."
   }
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONFIGURATION -->
## Configuration

Main settings in `application.yml`:

| Parameter | Default Value | Description |
| --- | --- | --- |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `spring.ai.ollama.chat.model` | `gemma3:4b` | Model for classification |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker |
| `app.kafka.topic.messages` | `ai_messages` | Incoming topic (requests) |
| `app.kafka.topic.answers` | `bank_ai_answers` | Outgoing topic (responses) |
| `spring.kafka.consumer.group-id` | `ai_messages_group_v3` | Consumer group ID |
| `server.port` | `8081` | Application port |

### Environment Variables

You can override any parameters via environment variables:

```bash
export SPRING_AI_OLLAMA_BASE_URL=http://ollama-server:11434
export KAFKA_BOOTSTRAP_SERVERS=kafka-cluster:9092
export SERVER_PORT=8082
./gradlew bootRun
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Architecture

### Request Processing Flow

1. **KeywordFinder** checks for keywords:
   - If match found → immediate return of category
   - If not found → pass to Ollama AI

2. **Ollama AI** (if required):
   - Receives prompt with question and list of categories
   - Classifies request (temperature=0.0 for determinism)
   - Returns category

3. **AIResponseService**:
   - Receives category (e.g., `GET_ACCOUNT_INFO`)
   - Returns ready-made template response

### Kafka Topics

| Topic | Producer | Consumer | Purpose |
|------|----------|----------|------------|
| `ai_messages` | BankSystem | AI Assistant | User requests |
| `bank_ai_answers` | AI Assistant | BankSystem | AI service responses |

### Supported Categories

The service supports 30+ categories of banking requests:

**Accounts:**
- `GET_ACCOUNT_INFO`, `GET_ACCOUNT_TYPES`, `GET_ACCOUNT_BALANCE`
- `GET_ACCOUNT_LIMITS`, `GET_ACCOUNT_STATUS`, `GET_CURRENCIES`

**Cards:**
- `GET_CARD_ISSUE_INFO`, `GET_CARD_TYPES`, `BLOCK_CARD_INFO`
- `UNBLOCK_CARD_INFO`, `GET_CARD_BALANCE`, `GET_CARD_LIMITS`

**Transactions:**
- `DEPOSIT_INFO`, `WITHDRAW_INFO`, `TRANSFER_INFO`
- `GET_TRANSACTION_HISTORY`, `IDEMPOTENCY_INFO`, `GET_LIMITS_INFO`

**Security:**
- `REGISTRATION_INFO`, `LOGIN_INFO`, `LOGOUT_INFO`
- `CHANGE_PASSWORD_INFO`, `EMAIL_CONFIRMATION_INFO`, `SECURITY_INFO`

**Service:**
- `SYSTEM_INFO`, `WORKING_HOURS`, `FEES_INFO`, `CONTACT_INFO`
- `MOBILE_APP_INFO`, `ESCALATE_TO_OPERATOR`

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## API

### REST Endpoints

#### GET /ask
Direct request to AI service (for testing).

**Parameters:**
- `question` (query param) - question text

**Example:**
```bash
curl "http://localhost:8081/ask?question=what%20types%20of%20cards%20are%20there"
```

**Response:**
```
Two types of cards:

💳 DEBIT - linked to account, spend your own money

💳 CREDIT - bank's credit funds with repayment

Payment systems: VISA, MasterCard, MIR
```

### Kafka Integration

#### Incoming Messages (Topic: `ai_messages`)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "text": "how to open a deposit?"
}
```

#### Outgoing Messages (Topic: `bank_ai_answers`)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "text": "To open an account:\n1. Log in to your personal account\n2. Section 'Accounts' → 'Create account'..."
}
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- TESTING -->
## Testing

```bash
./gradlew test
```

### Manual Testing via Kafka

1. Send a test message to topic `ai_messages`:
   ```bash
   docker exec -it ai_assistant_kafka /opt/kafka/bin/kafka-console-producer.sh \
     --broker-list localhost:9092 \
     --topic ai_messages \
     --property "parse.key=true" \
     --property "key.separator=:"
   
   # Enter (in one line):
   test-key:{"requestId":"test-123","text":"how to block a card"}
   ```

2. Listen to topic `bank_ai_answers`:
   ```bash
   docker exec -it ai_assistant_kafka /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic bank_ai_answers \
     --from-beginning
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Roadmap

- [ ] Add caching for frequent requests (Redis)
- [ ] Metrics and monitoring (Prometheus + Grafana)
- [ ] Multilingual support (EN, RU)
- [ ] Integration with vector databases for RAG
- [ ] Add rate limiting for AI requests
- [ ] Healthcheck endpoints for Kubernetes

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- RELATED PROJECTS -->
## Related Projects

**BankSystem** — main banking microservice:  
[https://github.com/favian1967/BankSystem](https://github.com/favian1967/BankSystem)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

Project Author: tg - @Rafink, x - https://x.com/Favian4747

Project Link: [https://github.com/favian1967/Ai_Assistant](https://github.com/favian1967/Ai_Assistant)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[java-shield]: https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[java-url]: https://openjdk.org/
[spring-boot-shield]: https://img.shields.io/badge/Spring%20Boot-3.5.10-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[spring-boot-url]: https://spring.io/projects/spring-boot
