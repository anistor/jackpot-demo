package io.github.anistor.jackpot.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

/**
 * Verifies springdoc exposes the generated OpenAPI document and Swagger UI and that
 * the annotated endpoints are present in it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiDocsIT {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        restClient = RestClient.create("http://localhost:" + port);
    }

    @Test
    void apiDocsExposeTheBetEndpoints() {
        ResponseEntity<String> response = restClient.get()
                .uri("/v3/api-docs")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("/api/bets")
                .contains("/api/bets/{betId}/reward")
                .contains("Place a bet")
                .contains("Get a bet's reward outcome");
    }

    @Test
    void swaggerUiIsAvailable() {
        ResponseEntity<String> response = restClient.get()
                .uri("/swagger-ui/index.html")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
