package io.github.anistor.jackpot.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.anistor.jackpot.service.BetOutcome;
import io.github.anistor.jackpot.service.BetService;

@WebMvcTest(BetController.class)
class BetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BetService betService;

    @Test
    void postAcceptsBetAndReturnsPendingStatus() throws Exception {
        given(betService.placeBet(any(), eq("user-1"), eq("JP-FIXED"), any(BigDecimal.class)))
                .willReturn("bet-123");

        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-1\",\"jackpotId\":\"JP-FIXED\",\"amount\":200.00}"))
                .andExpect(status().isAccepted())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.betId", is("bet-123")))
                .andExpect(jsonPath("$.status", is("PENDING")));
    }

    @Test
    void postRejectsBlankUserId() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"\",\"jackpotId\":\"JP-FIXED\",\"amount\":200.00}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(betService);
    }

    @Test
    void postRejectsNonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/bets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user-1\",\"jackpotId\":\"JP-FIXED\",\"amount\":-5.00}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(betService);
    }

    @Test
    void getReturnsWonOutcome() throws Exception {
        given(betService.getBetOutcome("bet-win"))
                .willReturn(BetOutcome.won("bet-win", "JP-FIXED", BigDecimal.valueOf(1234.56)));

        mockMvc.perform(get("/api/bets/{betId}/reward", "bet-win"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.betId", is("bet-win")))
                .andExpect(jsonPath("$.jackpotId", is("JP-FIXED")))
                .andExpect(jsonPath("$.status", is("WON")))
                .andExpect(jsonPath("$.rewardAmount", is(1234.56)));
    }

    @Test
    void getReturnsLostOutcome() throws Exception {
        given(betService.getBetOutcome("bet-lose")).willReturn(BetOutcome.lost("bet-lose", "JP-VARIABLE"));

        mockMvc.perform(get("/api/bets/{betId}/reward", "bet-lose"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId", is("bet-lose")))
                .andExpect(jsonPath("$.jackpotId", is("JP-VARIABLE")))
                .andExpect(jsonPath("$.status", is("LOST")))
                .andExpect(jsonPath("$.rewardAmount").doesNotExist());
    }

    @Test
    void getReturnsPendingWhenOutcomeNotYetAvailable() throws Exception {
        given(betService.getBetOutcome("bet-pending")).willReturn(BetOutcome.pending("bet-pending"));

        mockMvc.perform(get("/api/bets/{betId}/reward", "bet-pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.betId", is("bet-pending")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.jackpotId", is(nullValue())))
                .andExpect(jsonPath("$.rewardAmount").doesNotExist());
    }

    @Test
    void getReturnsNotFoundForUnknownBetId() throws Exception {
        given(betService.getBetOutcome("bet-unknown")).willReturn(BetOutcome.notFound("bet-unknown"));

        mockMvc.perform(get("/api/bets/{betId}/reward", "bet-unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postWithoutBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/bets").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(betService);
    }
}
