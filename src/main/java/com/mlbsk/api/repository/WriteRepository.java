package com.mlbsk.api.repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WriteRepository {

    Mono<AuthUserRow> findUserByEmail(String email);

    Mono<Void> upsertUserPreferences(String userId, Double defaultStake, String defaultPlatform, Double minEdgeFilter);

    Mono<PredictionAvailabilityRow> findPredictionAvailability(long predictionId);

    Mono<String> findLatestTrackedBetId(String userId, long predictionId);

    Mono<Void> updateTrackedBet(String betId, Double stake, Double odds, String platform);

    Mono<Void> createTrackedBet(String id, String userId, long predictionId, Double stake, Double odds, String platform);

    Mono<BetSettlementInputRow> findBetSettlementInput(String userId, String betId);

    Mono<Void> updateBet(String userId, String betId, Double stake, Double odds, String platform);

    Mono<Void> upsertBetResult(String resultId, String userId, String betId, Boolean won, Double profit);

    Mono<Void> clearBetResult(String userId, String betId);

    Mono<Void> deleteBet(String userId, String betId);

    Flux<PredictionAvailabilityRow> findPredictionAvailabilities(Iterable<Long> predictionIds);

    Mono<Void> createParlayWithBets(String parlayId, String userId, Double stake, Double odds, String platform, Iterable<Long> predictionIds, Iterable<String> betIds);

    Mono<AuthUserRow> findUserById(String userId);

    Mono<Void> createUser(String id, String email, String displayName, String passwordHash);

    Mono<Void> updateUserActive(String userId, boolean active);

    record AuthUserRow(
        String id,
        String email,
        String displayName,
        String passwordHash,
        boolean active
    ) {}

    record PredictionAvailabilityRow(
        long id,
        String recommendedSide
    ) {}

    record BetSettlementInputRow(
        String betId,
        Double stake,
        Double odds
    ) {}
}
