package io.github.anistor.jackpot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.anistor.jackpot.service.BetOutcome;
import io.github.anistor.jackpot.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO API versioning not considered yet

/**
 * Bet API. {@code POST /api/bets} publishes a bet for processing;
 * {@code GET /api/bets/{betId}/reward} returns the already-decided outcome, if available.
 */
@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
@Tag(name = "Bets", description = "Place bets and check their reward outcome")
public class BetController {

    private final BetService betService;

    @PostMapping
    @Operation(summary = "Place a bet", description = """
            Records the bet in the system and returns immediately with a PENDING status.
            The bet is processed asynchronously; poll the reward endpoint for the outcome.
            """)
    @ApiResponse(responseCode = "202", description = "Bet accepted for processing")
    @ApiResponse(responseCode = "400", description = "Invalid bet request", content = @Content)
    public ResponseEntity<PlaceBetResponse> placeBet(@Valid @RequestBody PlaceBetRequest request) {
        String betId = betService.placeBet(request.userId(), request.jackpotId(), request.amount());
        return ResponseEntity.accepted().body(new PlaceBetResponse(betId, BetStatus.PENDING));
    }

    @GetMapping("/{betId}/reward")
    @Operation(summary = "Get a bet's reward outcome", description = """
            Returns the already-decided outcome for a bet. The status is PENDING until the
            system has completed processing the bet, then WON (with a reward amount) or LOST.
            Returns 404 if no bet with this id was placed.
            """)
    @ApiResponse(responseCode = "200", description = "Current outcome for the bet")
    @ApiResponse(responseCode = "404", description = "No bet found with this id", content = @Content)
    public ResponseEntity<RewardResponse> getReward(
            @Parameter(description = "Identifier of the bet to evaluate", schema = @Schema(type = "string"))
            @PathVariable String betId) {
        BetOutcome outcome = betService.getBetOutcome(betId);
        return switch (outcome.status()) {
            case NOT_FOUND -> ResponseEntity.notFound().build();
            case PENDING -> ResponseEntity.ok(RewardResponse.pending(betId));
            case WON -> ResponseEntity.ok(RewardResponse.won(outcome.betId(), outcome.jackpotId(), outcome.rewardAmount()));
            case LOST -> ResponseEntity.ok(RewardResponse.lost(outcome.betId(), outcome.jackpotId()));
            case ERROR -> ResponseEntity.ok(RewardResponse.error(outcome.betId(), outcome.jackpotId()));
        };
    }
}
