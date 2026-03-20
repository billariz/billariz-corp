# billariz-corp

Backend API of the Billariz platform — open-source billing solution for utilities.

Built with **Java 17** and **Spring Boot 2.7**, it exposes a REST API for managing customers, contracts, invoices, and billing cycles.

[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

---

## Features

- Customer management (residential & B2B)
- Contract and subscription lifecycle
- Invoice generation and billing cycles
- Multi-provider support (local & AWS)
- REST API with OpenAPI / Swagger documentation
- Multi-language support (FR, EN, ES)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 2.7 |
| Database | PostgreSQL 15 |
| Migrations | Liquibase |
| Auth | Spring Security / AWS Cognito |
| Build | Maven |
| Container | Docker |

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 15+ (or Docker)

### Local setup

**1. Clone the repository**
```bash
git clone https://github.com/billariz/billariz-corp.git
cd billariz-corp
```

**2. Configure your local environment**
```bash
cp app/src/main/resources/application-local.yaml.example \
   app/src/main/resources/application-local.yaml
# Edit application-local.yaml with your local database credentials
```

**3. Start a local PostgreSQL instance (optional — via Docker)**
```bash
docker run -d \
  --name billariz-postgres \
  -e POSTGRES_DB=billariz \
  -e POSTGRES_USER=billariz \
  -e POSTGRES_PASSWORD=billariz \
  -p 5432:5432 \
  postgres:15-alpine
```

**4. Build and run**
```bash
mvn clean install -DskipTests
mvn spring-boot:run -pl launcher -Dspring-boot.run.profiles=local
```

The API will be available at `http://localhost:8080/v1`

---

## Running with H2 (simplest local setup)

H2 is the easiest way to run billariz-corp locally — no PostgreSQL or Docker required. It runs as a standalone TCP server on port `9092` and persists data to a local file.

### 1. Start the H2 server

```bash
java -cp $(find ~/.m2 -name "h2-*.jar" | head -1) \
  org.h2.tools.Server \
  -tcp -tcpPort 9092 \
  -web -webPort 8082 \
  -baseDir ~/billariz/h2 \
  -ifNotExists
```

Keep this terminal open. The H2 console will be available at `http://localhost:8082`.

> To reset the database, stop the server and delete the files in `~/billariz/h2/`.

### 2. Configure `application-local.yaml`

In `app/src/main/resources/application-local.yaml`, set the datasource to H2:

```yaml
spring:
  datasource:
    url: jdbc:h2:tcp://localhost:9092/file:~/billariz/h2/billariz;DATABASE_TO_UPPER=false;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: none
  liquibase:
    enabled: true
```

### 3. Build and run

```bash
mvn clean install -DskipTests
mvn spring-boot:run -pl app -Dspring-boot.run.profiles=local
```

### 4. Connect to H2 console (optional)

Open `http://localhost:8082` and connect with:

| Field | Value |
|---|---|
| JDBC URL | `jdbc:h2:tcp://localhost:9092/file:~/billariz/h2/billariz;DATABASE_TO_UPPER=false;MODE=PostgreSQL` |
| Username | `sa` |
| Password | `sa` |

---

### Run with Docker Compose (full stack)

```bash
docker compose up -d
```

---

## API Documentation

Swagger UI: `http://localhost:8080/v1/swagger-ui/index.html`

---

## Project Structure

```
billariz-corp/
├── app/            # Controllers, services, config
├── database/       # Liquibase migrations
├── launcher/       # Spring Boot entry point
├── notifier/       # Notification service
├── provider/       # Provider abstraction layer
├── provider-aws/   # AWS implementation (S3, SQS, Cognito)
├── provider-local/ # Local implementation
└── zreport/        # Aggregated test coverage reports
```

---

## License

Licensed under **AGPL-3.0** — see [LICENSE](LICENSE) for details.
For commercial licensing: contact@billariz.com

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).
