<a id="readme-top"></a>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Оглавление</summary>
  <ol>
    <li>
      <a href="#about-the-project">О проекте</a>
      <ul>
        <li><a href="#built-with">Стек</a></li>
      </ul>
    </li>
    <li>
      <a href="#getting-started">Быстрый старт</a>
      <ul>
        <li><a href="#prerequisites">Требования</a></li>
        <li><a href="#installation">Установка и запуск</a></li>
      </ul>
    </li>
    <li><a href="#usage">Использование</a></li>
    <li><a href="#configuration">Конфигурация</a></li>
    <li><a href="#architecture">Архитектура</a></li>
    <li><a href="#api">API</a></li>
    <li><a href="#testing">Тестирование</a></li>
    <li><a href="#roadmap">Планы</a></li>
    <li><a href="#contact">Контакты</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## О проекте

**AI Assistant** — это микросервис-ассистент на базе Spring Boot и Ollama AI для обработки пользовательских запросов в банковской системе. Сервис работает в связке с **BankSystem**, получает вопросы через Kafka, классифицирует их с помощью AI и возвращает готовые ответы.

Проект использует **hybrid-подход**:
- **KeywordFinder** — быстрая классификация по ключевым словам (~30 категорий)
- **Ollama AI** — классификация сложных/неоднозначных запросов через локальную LLM (модель `gemma3:4b`)
- **AIResponseService** — возврат готовых шаблонных ответов

Это обеспечивает низкую латентность для популярных запросов и гибкость для нестандартных формулировок.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Стек

* [![Java][java-shield]][java-url]
* [![Spring Boot][spring-boot-shield]][spring-boot-url]
* ![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
* ![Ollama](https://img.shields.io/badge/Ollama-gemma3:4b-000000?style=for-the-badge&logo=ollama&logoColor=white)
* ![Kafka](https://img.shields.io/badge/Kafka-Event%20Streaming-231F20?style=for-the-badge&logo=apachekafka)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

### Особенности

- **Event-driven архитектура** через Apache Kafka
- **Гибридная классификация** (keyword matching + AI)
- **Готовые шаблоны ответов** для 30+ типов банковских запросов
- **Низкая латентность** за счет локальной LLM (Ollama)
- **Request-Reply паттерн** через Kafka топики

<!-- GETTING STARTED -->
## Быстрый старт

### Требования

- **Java 21**
- **Ollama** с загруженной моделью `gemma3:4b`
- **Apache Kafka** (running on `localhost:9092`)
- **BankSystem** (основной микросервис) - опционально для тестирования интеграции

### Установка и запуск

#### 1. Установка Ollama и модели

```bash
# Установите Ollama с официального сайта: https://ollama.ai

# Загрузите модель gemma3:4b
ollama pull gemma3:4b

# Проверьте что Ollama запущен (по умолчанию на http://localhost:11434)
curl http://localhost:11434
```

#### 2. Запуск Kafka

Если Kafka еще не запущен, используйте Docker:

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

#### 3. Запуск приложения

```bash
# Скомпилируйте проект
./gradlew build

# Запустите приложение
./gradlew bootRun
```

Приложение запустится на порту **8081**.

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Использование

### Standalone режим

Для тестирования без Kafka можно отправить прямой HTTP запрос:

```bash
curl "http://localhost:8081/ask?question=как%20открыть%20счет"
```

**Ответ:**
```
Чтобы открыть счет:
1. Войдите в личный кабинет
2. Раздел 'Счета' → 'Создать счет'
3. Выберите тип: текущий (CHECKING), сберегательный (SAVINGS) или депозитный (DEPOSIT)
4. Выберите валюту: RUB, USD или EUR
5. Подтвердите создание

Счет будет создан моментально с уникальным номером.
```

### Kafka-режим (продакшн)

Сервис автоматически:
1. **Слушает топик** `ai_messages` (groupId: `ai_messages_group_v3`)
2. **Получает** сообщения вида:
   ```json
   {
     "requestId": "uuid-123",
     "text": "как заблокировать карту?"
   }
   ```
3. **Обрабатывает** через `AIRoutingService`
4. **Отправляет ответ** в топик `bank_ai_answers`:
   ```json
   {
     "requestId": "uuid-123",
     "text": "Блокировка карты при потере/краже:\n1. Личный кабинет → 'Мои карты'..."
   }
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONFIGURATION -->
## Конфигурация

Основные настройки в `application.yml`:

| Параметр | Значение по умолчанию | Описание |
| --- | --- | --- |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | URL Ollama сервера |
| `spring.ai.ollama.chat.model` | `gemma3:4b` | Модель для классификации |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka брокер |
| `app.kafka.topic.messages` | `ai_messages` | Входящий топик (запросы) |
| `app.kafka.topic.answers` | `bank_ai_answers` | Исходящий топик (ответы) |
| `spring.kafka.consumer.group-id` | `ai_messages_group_v3` | Consumer group ID |
| `server.port` | `8081` | Порт приложения |

### Переменные окружения

Вы можете переопределить любые параметры через environment variables:

```bash
export SPRING_AI_OLLAMA_BASE_URL=http://ollama-server:11434
export KAFKA_BOOTSTRAP_SERVERS=kafka-cluster:9092
export SERVER_PORT=8082
./gradlew bootRun
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## Архитектура

### Поток обработки запроса

1. **KeywordFinder** проверяет наличие ключевых слов:
   - Если найдено совпадение → сразу возврат категории
   - Если не найдено → передача в Ollama AI

2. **Ollama AI** (если требуется):
   - Получает промпт с вопросом и списком категорий
   - Классифицирует запрос (temperature=0.0 для детерминированности)
   - Возвращает категорию

3. **AIResponseService**:
   - Получает категорию (например, `GET_ACCOUNT_INFO`)
   - Возвращает готовый шаблонный ответ

### Kafka топики

| Topic | Producer | Consumer | Назначение |
|------|----------|----------|------------|
| `ai_messages` | BankSystem | AI Assistant | Запросы пользователей |
| `bank_ai_answers` | AI Assistant | BankSystem | Ответы AI сервиса |

### Поддерживаемые категории

Сервис поддерживает 30+ категорий банковских запросов:

**Счета:**
- `GET_ACCOUNT_INFO`, `GET_ACCOUNT_TYPES`, `GET_ACCOUNT_BALANCE`
- `GET_ACCOUNT_LIMITS`, `GET_ACCOUNT_STATUS`, `GET_CURRENCIES`

**Карты:**
- `GET_CARD_ISSUE_INFO`, `GET_CARD_TYPES`, `BLOCK_CARD_INFO`
- `UNBLOCK_CARD_INFO`, `GET_CARD_BALANCE`, `GET_CARD_LIMITS`

**Транзакции:**
- `DEPOSIT_INFO`, `WITHDRAW_INFO`, `TRANSFER_INFO`
- `GET_TRANSACTION_HISTORY`, `IDEMPOTENCY_INFO`, `GET_LIMITS_INFO`

**Безопасность:**
- `REGISTRATION_INFO`, `LOGIN_INFO`, `LOGOUT_INFO`
- `CHANGE_PASSWORD_INFO`, `EMAIL_CONFIRMATION_INFO`, `SECURITY_INFO`

**Сервис:**
- `SYSTEM_INFO`, `WORKING_HOURS`, `FEES_INFO`, `CONTACT_INFO`
- `MOBILE_APP_INFO`, `ESCALATE_TO_OPERATOR`

<p align="right">(<a href="#readme-top">back to top</a>)</p>

## API

### REST Endpoints

#### GET /ask
Прямой запрос к AI сервису (для тестирования).

**Параметры:**
- `question` (query param) - текст вопроса

**Пример:**
```bash
curl "http://localhost:8081/ask?question=какие%20типы%20карт%20есть"
```

**Ответ:**
```
Два типа карт:

💳 DEBIT (Дебетовая) - привязана к счету, тратите свои деньги

💳 CREDIT (Кредитная) - кредитные средства банка с возвратом

Платежные системы: VISA, MasterCard, МИР
```

### Kafka Integration

#### Входящие сообщения (Topic: `ai_messages`)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "text": "как открыть депозит?"
}
```

#### Исходящие сообщения (Topic: `bank_ai_answers`)

```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "text": "Чтобы открыть счет:\n1. Войдите в личный кабинет\n2. Раздел 'Счета' → 'Создать счет'..."
}
```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- TESTING -->
## Тестирование

```bash
./gradlew test
```

### Ручное тестирование через Kafka

1. Отправьте тестовое сообщение в топик `ai_messages`:
   ```bash
   docker exec -it ai_assistant_kafka /opt/kafka/bin/kafka-console-producer.sh \
     --broker-list localhost:9092 \
     --topic ai_messages \
     --property "parse.key=true" \
     --property "key.separator=:"
   
   # Введите (в одну строку):
   test-key:{"requestId":"test-123","text":"как заблокировать карту"}
   ```

2. Прослушайте топик `bank_ai_answers`:
   ```bash
   docker exec -it ai_assistant_kafka /opt/kafka/bin/kafka-console-consumer.sh \
     --bootstrap-server localhost:9092 \
     --topic bank_ai_answers \
     --from-beginning
   ```

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- ROADMAP -->
## Планы

- [ ] Добавить кэширование частых запросов (Redis)
- [ ] Метрики и мониторинг (Prometheus + Grafana)
- [ ] Поддержка мультиязычности (EN, RU)
- [ ] Интеграция с векторными БД для RAG
- [ ] Добавить rate limiting для AI запросов
- [ ] Healthcheck endpoints для Kubernetes

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- RELATED PROJECTS -->
## Связанные проекты

**BankSystem** — основной банковский микросервис:  
[https://github.com/favian1967/BankSystem](https://github.com/favian1967/BankSystem)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- CONTACT -->
## Контакты

Автор проекта: tg - @Rafink, x - https://x.com/Favian4747

Project Link: [https://github.com/favian1967/Ai_Assistant](https://github.com/favian1967/Ai_Assistant)

<p align="right">(<a href="#readme-top">back to top</a>)</p>

<!-- MARKDOWN LINKS & IMAGES -->
[java-shield]: https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white
[java-url]: https://openjdk.org/
[spring-boot-shield]: https://img.shields.io/badge/Spring%20Boot-3.5.10-6DB33F?style=for-the-badge&logo=springboot&logoColor=white
[spring-boot-url]: https://spring.io/projects/spring-boot
