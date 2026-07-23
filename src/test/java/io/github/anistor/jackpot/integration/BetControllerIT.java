package io.github.anistor.jackpot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import io.github.anistor.jackpot.controller.BetStatus;
import io.github.anistor.jackpot.controller.PlaceBetRequest;
import io.github.anistor.jackpot.controller.PlaceBetResponse;
import io.github.anistor.jackpot.controller.RewardResponse;
import io.github.anistor.jackpot.domain.OutboxEventEntity;
import io.github.anistor.jackpot.domain.ProcessedBetEntity;
import io.github.anistor.jackpot.repository.OutboxEventRepository;
import io.github.anistor.jackpot.repository.ProcessedBetRepository;

import com.github.f4b6a3.uuid.UuidCreator;

/**
 * End-to-end integration test for the two bet endpoints against a real running app (embedded
 * server on a random port, real HTTP via {@link RestClient}). No MockMvc.
 *
 * <p>The test profile keeps the Kafka listener and outbox publisher disabled, so a freshly placed
 * bet stays unprocessed and its reward is {@code PENDING}. An unknown betId (never placed) returns
 * 404. The already-decided WON/LOST branches of the reward endpoint are exercised by seeding a
 * {@link ProcessedBetEntity} directly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BetControllerIT {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private ProcessedBetRepository processedBetRepository;

    @BeforeEach
    void setUp() {
        restClient = RestClient.create("http://localhost:" + port);
    }

    @Test
    void postAcceptsBetAndPersistsOutboxEvent() {
        ResponseEntity<PlaceBetResponse> response = restClient.post()
                .uri("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceBetRequest("user-1", "JP-FIXED", BigDecimal.valueOf(200)))
                .retrieve()
                .toEntity(PlaceBetResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .satisfies(type -> type.isCompatibleWith(MediaType.APPLICATION_JSON));
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().betId()).isNotBlank();
        assertThat(response.getBody().status()).isEqualTo(BetStatus.PENDING);

        Optional<OutboxEventEntity> outboxEvent = outboxRepository.findById(response.getBody().betId());
        assertThat(outboxEvent).isPresent();
        assertThat(outboxEvent.orElseThrow().getCreatedAt()).isNotNull();
    }

    @Test
    void postRejectsInvalidRequestWithBadRequest() {
        HttpStatusCode status = restClient.post()
                .uri("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceBetRequest("", "JP-FIXED", BigDecimal.valueOf(-5)))
                .exchange((_, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getReturnsPendingForJustPlacedBet() {
        ResponseEntity<PlaceBetResponse> entity = restClient.post()
                .uri("/api/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PlaceBetRequest("user-2", "JP-FIXED", BigDecimal.valueOf(150)))
                .retrieve()
                .toEntity(PlaceBetResponse.class);
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(entity.getBody()).isNotNull();

        String betId = entity.getBody().betId();

        ResponseEntity<RewardResponse> response = restClient.get()
                .uri("/api/bets/{betId}/reward", betId)
                .retrieve()
                .toEntity(RewardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().betId()).isEqualTo(betId);
        assertThat(response.getBody().status()).isEqualTo(BetStatus.PENDING);
        assertThat(response.getBody().rewardAmount()).isNull();
    }

    @Test
    void getReturnsNotFoundForUnknownBet() {
        HttpStatusCode status = restClient.get()
                .uri("/api/bets/{betId}/reward", "does-not-exist")
                .exchange((_, res) -> res.getStatusCode());

        assertThat(status).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getReturnsWonOutcomeForProcessedWinningBet() {
        String betId = randomUUID();
        ProcessedBetEntity processedBet = ProcessedBetEntity.builder()
                .betId(betId)
                .userId("user-3")
                .jackpotId("JP-FIXED")
                .status(ProcessedBetEntity.Status.WON)
                .rewardAmount(BigDecimal.valueOf(1234.56))
                .build();
        processedBetRepository.save(processedBet);
        assertThat(processedBet.getProcessedAt()).isNotNull();

        ResponseEntity<RewardResponse> response = restClient.get()
                .uri("/api/bets/{betId}/reward", betId)
                .retrieve()
                .toEntity(RewardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().betId()).isEqualTo(betId);
        assertThat(response.getBody().jackpotId()).isEqualTo("JP-FIXED");
        assertThat(response.getBody().status()).isEqualTo(BetStatus.WON);
        assertThat(response.getBody().rewardAmount()).isEqualByComparingTo("1234.56");
    }

    @Test
    void getReturnsLostOutcomeForProcessedLosingBet() {
        String betId = randomUUID();
        ProcessedBetEntity processedBet = ProcessedBetEntity.builder()
                .betId(betId)
                .userId("user-4")
                .jackpotId("JP-VARIABLE")
                .status(ProcessedBetEntity.Status.LOST)
                .build();
        processedBetRepository.save(processedBet);
        assertThat(processedBet.getProcessedAt()).isNotNull();

        ResponseEntity<RewardResponse> response = restClient.get()
                .uri("/api/bets/{betId}/reward", betId)
                .retrieve()
                .toEntity(RewardResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().betId()).isEqualTo(betId);
        assertThat(response.getBody().jackpotId()).isEqualTo("JP-VARIABLE");
        assertThat(response.getBody().status()).isEqualTo(BetStatus.LOST);
        assertThat(response.getBody().rewardAmount()).isNull();
    }

    private static String randomUUID() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }
}
