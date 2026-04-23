# Grid07 Backend Assessment

Spring Boot backend for a social interaction workflow that supports post creation, comments, likes, Redis-backed virality tracking, bot reply throttling, and scheduled notification aggregation.

## Overview

This service exposes a small REST API for creating posts, adding comments, and liking posts. PostgreSQL stores durable entities such as posts and comments, while Redis handles high-contention state such as virality scores, bot reply caps, cooldown windows, and notification timing.

The design separates:

- durable relational data in PostgreSQL
- fast-changing counters and cooldown state in Redis
- orchestration and business rules in the service layer

## Core Features

- Create posts authored by either a user or a bot
- Add comments with a maximum nesting depth of 20
- Like posts and update a Redis-backed virality score
- Limit bot replies per post to 100
- Apply a 10-minute cooldown between the same bot and human pair
- Trigger notification handling for bot interactions
- Run a scheduled sweep every 5 minutes for queued notifications

## Tech Stack

- Java 21
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- Spring Data Redis
- PostgreSQL 15
- Redis 7
- Maven
- Docker and Docker Compose

## Architecture

### Application Flow

1. A client calls the REST API under `/api/posts`.
2. `PostController` delegates the request to `PostService`.
3. `PostService` validates business rules and coordinates persistence plus Redis updates.
4. `ViralityService` manages counters and cooldown keys in Redis.
5. `NotificationService` handles notification cooldown logic.
6. `NotificationScheduler` periodically sweeps pending notification lists.

### Persistence Split

- PostgreSQL stores `posts`, `comments`, `users`, and `bots`
- Redis stores:
  - `post:{postId}:virality_score`
  - `post:{postId}:bot_count`
  - `cooldown:bot_{botId}:human_{humanId}`
  - `user:{userId}:notif_sent`
  - `user:{userId}:pending_notifs`

## Business Rules

### Commenting Rules

- Maximum comment depth is 20
- A post can receive at most 100 bot replies
- A specific bot cannot target the same human again until the 10-minute cooldown expires

### Virality Scoring

- `BOT_REPLY` = `+1`
- `HUMAN_LIKE` = `+20`
- `HUMAN_COMMENT` = `+50`

### Notification Rules

- Immediate notification if the user is not on notification cooldown
- Notification cooldown duration is 15 minutes
- A scheduler runs every 5 minutes to sweep pending notification keys

## Approach and Thread Safety for Atomic Locks

My approach was to keep contention-heavy coordination out of the JVM and move it into Redis, where operations on a single key are executed atomically. That avoids relying on `synchronized` blocks or in-memory locks, which would only protect one application instance.

### What is thread-safe today

- The bot reply cap is protected with Redis `INCR` and compensating `DECR`.
- The virality score is updated with Redis atomic increment operations.
- Because Redis processes these commands atomically, concurrent requests from multiple threads or multiple app instances cannot corrupt these counters.

### Why the bot cap remains safe under concurrency

When several bot requests hit the same post at nearly the same time, each request increments `post:{postId}:bot_count` in Redis. If the incremented value is greater than `100`, that request is rejected and the counter is immediately decremented back. Only requests that observe a value at or below the limit proceed to the database write, which prevents more than 100 accepted bot replies for a post.

### Cooldown lock design

The cooldown mechanism uses a Redis key scoped to a bot-human pair:

`cooldown:bot_{botId}:human_{humanId}`

That key design isolates contention to the exact pair being throttled and prevents unrelated requests from blocking each other. The key also expires automatically after 10 minutes, which keeps the lock lifecycle simple and removes manual cleanup.

### Important implementation note

The strict atomic guarantee currently applies most clearly to the Redis counters. The cooldown path is Redis-backed and distributed, but the current implementation checks key existence and sets the TTL in two separate operations. So the README can accurately claim strong thread safety for the atomic counters and Redis-based lock strategy, while the cooldown acquisition itself could be hardened further with a single atomic compare-and-set style operation if you wanted a stricter lock guarantee.

## API Reference

Base path: `http://localhost:8080/api/posts`

### 1. Create Post

`POST /api/posts`

Request body:

```json
{
  "authorId": 1,
  "authorType": "USER",
  "content": "My first post"
}
```

Response:

- `201 Created`
- Returns the created `Post`

### 2. Add Comment

`POST /api/posts/{postId}/comments`

Request body:

```json
{
  "authorId": 101,
  "authorType": "BOT",
  "content": "Automated reply",
  "depthLevel": 0,
  "humanAuthorId": 1
}
```

Response:

- `201 Created` on success
- `400 Bad Request` if `depthLevel > 20`
- `429 Too Many Requests` if:
  - the bot-human cooldown is active
  - the post already reached 100 bot replies

### 3. Like Post

`POST /api/posts/{postId}/like`

Request body:

```json
{
  "userId": 1
}
```

Response:

```json
{
  "status": "liked",
  "postId": "1"
}
```

## Local Development

### Prerequisites

- Java 21
- Maven 3.9+ or the included Maven Wrapper
- PostgreSQL running on port `5432`
- Redis running on port `6379`

### Environment Variables

The application reads these values, with sensible local defaults already configured in `application.properties`.

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/assessmentdb` |
| `DB_USER` | `root` |
| `DB_PASS` | `root@456` |
| `REDIS_HOST` | `localhost` |
| `REDIS_PORT` | `6379` |
| `SERVER_PORT` | `8080` |

### Run Locally

```powershell
.\mvnw.cmd spring-boot:run
```

Or build first:

```powershell
.\mvnw.cmd clean package
java -jar target\backend-0.0.1-SNAPSHOT.jar
```

## Docker Setup

The repository includes:

- `Dockerfile` for the Spring Boot application
- `docker-compose.yml` for PostgreSQL, Redis, and the app container

Start everything with:

```powershell
docker compose up --build
```

Exposed ports:

- App: `8080`
- PostgreSQL: `5432`
- Redis: `6379`

## Concurrency Test

The repository includes `test_race.ps1`, which fires 200 concurrent bot comment requests against the same post and then checks:

- PostgreSQL bot comment count
- Redis `post:{postId}:bot_count`

Run it after the app and containers are up:

```powershell
.\test_race.ps1
```

Expected result for the tested post:

- database bot comments should not exceed `100`
- Redis `bot_count` should remain `100`

## Project Structure

```text
src/
  main/
    java/org/grid07/backend/
      config/
      controller/
      dto/
      entity/
      repository/
      service/
      sheduler/
    resources/
  test/
```

## Assumptions and Notes

- Authentication and authorization are not implemented in this service.
- `authorId`, `userId`, and `humanAuthorId` are accepted from the request payload and treated as external identifiers.
- JPA is configured with `spring.jpa.hibernate.ddl-auto=update`, which is convenient for local development but should be reviewed before production use.
- Notification sweeping infrastructure exists, and the scheduler is enabled via `@EnableScheduling`.

## Future Improvements

- Make cooldown acquisition fully atomic with a single Redis compare-and-set style operation
- Add controller and service-level tests for concurrency-sensitive paths
- Introduce authentication and stronger input validation
- Add OpenAPI or Swagger documentation
- Persist notification delivery events for auditability

## Author's Summary

The system is built around a simple principle: keep durable business records in PostgreSQL and keep high-contention coordination in Redis. That lets the API stay simple while still handling concurrent bot activity, virality updates, and cooldown windows with predictable behavior under load.
