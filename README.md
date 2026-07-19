# Jackpot Demo - Contribution & Reward Backend

A Spring Boot service that processes jackpot bets: each bet contributes to a shared jackpot
pool and is immediately evaluated for a reward. 
Built on *Java 25*, *Spring Boot 4*, *Spring Kafka*, *Spring Data JPA*.

## Flow

```
-> POST /api/bets --> OutboxEvent (PENDING) [one transaction: bet is durable, never lost in case of downstream failures]
                          |
                          v  
                     @Scheduled OutboxPublisher (retries until confirmed)
                          |
                          v
                     Kafka topic "jackpot-bets" (key = Jackpot ID)
                          |
                          v  
                     @KafkaListener BetConsumer (idempotent)
                          |
                          v
                     BetProcessingService (one transaction):
                       1. dedup on Bet ID (skip if already processed)
                       2. apply ContributionStrategy -> grow pool, save contribution
                       3. roll RewardStrategy on the *just-updated* pool
                       4. on win: save reward, reset pool to initial value
                       5. save ProcessedBetEntity (bet outcome + idempotency marker)
                        
-> GET /api/bets/{betId}/reward --> returns the already-decided outcome (PENDING/WON/LOST/ERROR)
```

## API

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/bets` | Place a bet. Body: `{ "userId", "jackpotId", "amount" }`. Returns `202` with a generated `betId` and `status: PENDING`. |
| `GET` | `/api/bets/{betId}/reward` | Returns the outcome: `PENDING` until processed, then `WON` (with `rewardAmount`) or `LOST`. |


Seeded jackpots: `JP-FIXED` (fixed contribution & odds) and `JP-VARIABLE` (variable
contribution & pool-driven odds). See `JackpotDataInitializer`.

### Interactive API docs (OpenAPI / Swagger UI)

The bet endpoints are annotated with springdoc-openapi. When the app is running:

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

## Running

We assume you have **Docker** and **Maven** installed. The app runs on Java 25 and any Maven 3.9.x should be fine.

Start MySQL + Kafka, then the SB app:

```bash
docker compose up -d
mvn -s ./maven-settings.xml clean package spring-boot:run
# or: mvn -s ./maven-settings.xml -DskipTests package && java -jar target/jackpot-demo-1.0-SNAPSHOT.jar
```

`docker compose` starts **MySQL 8.4** (`localhost:3307`, database/user/password `jackpotuser/jackpotpwd`,
persisted in a named volume) and Kafka. The broker runs in KRaft mode (no ZooKeeper). It exposes two listeners: `localhost:9092` for host clients
(this app) and `kafka:29092` for in-network clients. A **Kafka UI** is also included at <http://localhost:8090> for inspecting topics, partitions and messages.

Example:

```bash
# place a bet
curl -s -XPOST localhost:8080/api/bets -H 'Content-Type: application/json' \
  -d '{"userId":"u1","jackpotId":"JP-VARIABLE","amount":500}'
# -> {"betId":"<uuid>","status":"PENDING"}

# check the outcome
curl -s localhost:8080/api/bets/<uuid>/reward
# -> {"betId":"<uuid>","jackpotId":"JP-VARIABLE","status":"WON","rewardAmount":5094.98}
```

You can also use the `placeBets.sh` script to place some bets in a loop and watch the pool grow and eventually pay out.

Tests: `mvn -s ./maven-settings.xml test` (no broker or database needed - Kafka is disabled and
an in-memory H2 database in MySQL-compatibility mode is used in the test profile).

Once you are done, to remove the docker containers and volume simply use:
```
docker compose down
docker volume rm jackpot-demo-mysql-data
```

## Design decisions & justifications

### 1. The win is decided at bet-processing time, not at the evaluate call
The reward is rolled inside the consumer, **immediately after the bet's own contribution is
applied**, using the pool state that bet just created. The `GET .../reward` endpoint merely
returns the stored outcome.

*Why:* if the win were rolled live inside a later, separate call, the odds would depend on the
pool's state at that unrelated moment - shifted by other concurrent bets. The outcome would
then depend on timing/ordering of unrelated activity, which is unfair and non-deterministic.
Deciding at processing time makes each bet's result depend only on that bet.

### 2. Transactional outbox (not publish-to-Kafka-then-return)
`POST /api/bets` writes the bet to an `outbox_event` row in the same transaction that
acknowledges it; a scheduled `OutboxPublisher` publishes to Kafka afterwards and only marks the
row `SENT` once the broker confirms. A failed publish leaves the row `PENDING` for (bounded) retry.

*Justification:* the player's wallet was probably already been debited upstream before the bet reaches this
service, so the bet record is the **only proof money is owed to a jackpot**. A lost Kafka
publish would be lost money, not merely lost data - so durable capture before publishing is a
requirement here, not just a nice-to-have.

### 3. Idempotent consumer (at-least-once delivery)
Kafka guarantees at-least-once delivery, so a bet can be redelivered (e.g. the consumer
processes it but crashes before acking). Before doing any work the consumer checks
`ProcessedBetEntity` for the bet ID and skips if present. Duplicate publishes are also keyed by bet ID.

### 4. Concurrency via partition key + optimistic locking
Bets are published with **Jackpot ID as the Kafka message key**, so all bets for one jackpot
land in the same partition and are processed **in order by a single consumer thread** - no
concurrent writers to that jackpot's pool. Different jackpots use different partitions and run
in parallel. This satisfies the single-*topic* requirement (the constraint is on topic count,
not partition count).

As a DB-level safety net, `JackpotEntity` carries a `@Version` column (optimistic locking); the
consumer retries a few times in case of an optimistic-lock conflict.

*Trade-off:* correctness of the "no concurrent writers" guarantee relies on the
one-partition-per-jackpot property. Optimistic locking is a cheap safety net (no blocking when
there is no conflict, which is the common case here). If that guarantee were ever broken -
e.g. a future scaling change, a stray endpoint writing the pool, or duplicate consumers -
**pessimistic row locking would be the fallback**. Under a viral traffic spike the single
partition simply builds a longer queue rather than creating concurrent writers, so optimistic
locking remains appropriate.

### 5. Extensible strategies (Strategy pattern)
Contribution and reward calculations are `ContributionStrategy` / `RewardStrategy` interfaces
with `FIXED` and `VARIABLE` implementations, resolved by a factory keyed on the jackpot's
configured type. Adding a new scheme is a new bean - no existing code changes.

- **Fixed contribution:** a constant % of the stake.
- **Variable contribution:** % shrinks as the pool fills (reaches 0 at the configured limit).
- **Fixed reward:** constant win chance.
- **Variable reward:** win chance rises with the pool, reaching 100% at the configured limit
  (guaranteeing eventual payout).

### 6. API versioning: omitted
The two endpoints are left unversioned for simplicity. If the service were exposed to external consumers,
the natural extension point would be URI prefixing (`/api/v1/...`) or media-type/header-based versioning.

## Notes / limitations
- MySQL runs via docker-compose with a persisted volume, so jackpot state survives restarts.
  `ddl-auto: update` keeps the schema in sync; the seeder only inserts jackpots when none exist.
- Tests run against in-memory H2 (in MySQL-compatibility mode) so `mvn test` needs no containers.
- The outbox publisher uses a simple fixed-delay poll, sufficient for this scope, but not ideal for horizontally scaled services.
- The code still contains several TODOs for future improvements.
