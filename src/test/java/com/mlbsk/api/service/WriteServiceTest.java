package com.mlbsk.api.service;

import com.mlbsk.api.dto.LoginRequest;
import com.mlbsk.api.dto.LoginResponse;
import com.mlbsk.api.repository.WriteRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class WriteServiceTest {

    @Test
    void loginAcceptsBcrypt2bHashesStoredByFrontend() {
        String password = "maconha2026";
        String hash2a = BCrypt.hashpw(password, BCrypt.gensalt(12));
        String hash2b = "$2b$" + hash2a.substring(4);
        WriteRepository.AuthUserRow user = new WriteRepository.AuthUserRow(
            "user-1",
            "maconha@gmail.com",
            "Maconha",
            hash2b,
            true
        );

        WriteService service = new WriteService(new InMemoryWriteRepository(user));

        LoginResponse response = service.login(new LoginRequest("maconha@gmail.com", password)).block();

        Assertions.assertNotNull(response);
        Assertions.assertEquals("user-1", response.id());
        Assertions.assertEquals("maconha@gmail.com", response.email());
    }

    private static final class InMemoryWriteRepository implements WriteRepository {

        private final AuthUserRow user;

        private InMemoryWriteRepository(AuthUserRow user) {
            this.user = user;
        }

        @Override
        public Mono<AuthUserRow> findUserByEmail(String email) {
            return user.email().equals(email) ? Mono.just(user) : Mono.empty();
        }

        @Override
        public Mono<Void> upsertUserPreferences(String userId, Double defaultStake, String defaultPlatform, Double minEdgeFilter) {
            return Mono.empty();
        }

        @Override
        public Mono<PredictionAvailabilityRow> findPredictionAvailability(long predictionId) {
            return Mono.empty();
        }

        @Override
        public Mono<String> findLatestTrackedBetId(String userId, long predictionId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> updateTrackedBet(String betId, Double stake, Double odds, String platform) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> createTrackedBet(String id, String userId, long predictionId, Double stake, Double odds, String platform) {
            return Mono.empty();
        }

        @Override
        public Mono<BetSettlementInputRow> findBetSettlementInput(String userId, String betId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> updateBet(String userId, String betId, Double stake, Double odds, String platform) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> upsertBetResult(String resultId, String userId, String betId, Boolean won, Double profit) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> clearBetResult(String userId, String betId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> deleteBet(String userId, String betId) {
            return Mono.empty();
        }

        @Override
        public Flux<PredictionAvailabilityRow> findPredictionAvailabilities(Iterable<Long> predictionIds) {
            return Flux.empty();
        }

        @Override
        public Mono<Void> createParlayWithBets(String parlayId, String userId, Double stake, Double odds, String platform, Iterable<Long> predictionIds, Iterable<String> betIds) {
            return Mono.empty();
        }

        @Override
        public Mono<AuthUserRow> findUserById(String userId) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> createUser(String id, String email, String displayName, String passwordHash) {
            return Mono.empty();
        }

        @Override
        public Mono<Void> updateUserActive(String userId, boolean active) {
            return Mono.empty();
        }
    }
}
