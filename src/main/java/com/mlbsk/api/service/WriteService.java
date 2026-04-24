package com.mlbsk.api.service;

import com.mlbsk.api.dto.CreateUserRequest;
import com.mlbsk.api.dto.DeleteBetRequest;
import com.mlbsk.api.dto.LoginRequest;
import com.mlbsk.api.dto.LoginResponse;
import com.mlbsk.api.dto.ParlayBetRequest;
import com.mlbsk.api.dto.SavePreferencesRequest;
import com.mlbsk.api.dto.SetBetResultRequest;
import com.mlbsk.api.dto.ToggleUserActiveRequest;
import com.mlbsk.api.dto.TrackedBetRequest;
import com.mlbsk.api.dto.UpdateBetRequest;
import com.mlbsk.api.exception.ResourceNotFoundException;
import com.mlbsk.api.repository.WriteRepository;
import jakarta.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;
import reactor.core.publisher.Mono;

@Singleton
public class WriteService {

    private static final String DEFAULT_PLATFORM = "Betano";
    private static final int BCRYPT_ROUNDS = 12;

    private final WriteRepository repository;

    public WriteService(WriteRepository repository) {
        this.repository = repository;
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        if (request.email() == null || request.email().isBlank() || request.password() == null || request.password().isBlank()) {
            return Mono.empty();
        }
        return repository.findUserByEmail(request.email().trim())
            .filter(WriteRepository.AuthUserRow::active)
            .filter(user -> BCrypt.checkpw(request.password(), normalizeBcryptHash(user.passwordHash())))
            .map(user -> new LoginResponse(user.id(), user.email(), user.displayName(), user.active()));
    }

    public Mono<Void> savePreferences(SavePreferencesRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.defaultStake() != null && request.defaultStake() <= 0) {
            return Mono.error(new IllegalArgumentException("Default stake must be positive"));
        }
        if (request.minEdgePercent() != null && (request.minEdgePercent() < 0 || request.minEdgePercent() > 100)) {
            return Mono.error(new IllegalArgumentException("Min edge must be between 0 and 100"));
        }
        String platform = request.defaultPlatform() == null || request.defaultPlatform().isBlank()
            ? DEFAULT_PLATFORM
            : request.defaultPlatform().trim();
        Double minEdgeFilter = request.minEdgePercent() == null ? null : request.minEdgePercent() / 100.0;
        return repository.upsertUserPreferences(request.userId(), request.defaultStake(), platform, minEdgeFilter);
    }

    public Mono<Void> upsertTrackedBet(TrackedBetRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.predictionId() == null || request.predictionId() <= 0) {
            return Mono.error(new IllegalArgumentException("Invalid prediction"));
        }
        if (request.stake() == null || request.stake() <= 0) {
            return Mono.error(new IllegalArgumentException("Stake must be positive"));
        }
        if (request.odds() == null || request.odds() <= 1) {
            return Mono.error(new IllegalArgumentException("Odds must be greater than 1.00"));
        }
        String platform = request.platform() == null || request.platform().isBlank() ? DEFAULT_PLATFORM : request.platform().trim();

        return repository.findPredictionAvailability(request.predictionId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Prediction is not available for bet tracking")))
            .flatMap(prediction -> {
                if (prediction.recommendedSide() == null) {
                    return Mono.error(new IllegalArgumentException("Prediction is not available for bet tracking"));
                }
                return repository.findLatestTrackedBetId(request.userId(), prediction.id())
                    .flatMap(existingId -> repository.updateTrackedBet(existingId, request.stake(), request.odds(), platform))
                    .switchIfEmpty(repository.createTrackedBet(UUID.randomUUID().toString(), request.userId(), prediction.id(), request.stake(), request.odds(), platform));
            });
    }

    public Mono<Void> placeParlayBet(ParlayBetRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.predictionIds() == null || request.predictionIds().size() < 2) {
            return Mono.error(new IllegalArgumentException("A combined bet must have at least 2 legs"));
        }
        if (request.stake() == null || request.stake() <= 0) {
            return Mono.error(new IllegalArgumentException("Stake must be positive"));
        }
        if (request.odds() == null || request.odds() <= 1) {
            return Mono.error(new IllegalArgumentException("Odds must be greater than 1.00"));
        }
        String platform = request.platform() == null || request.platform().isBlank() ? DEFAULT_PLATFORM : request.platform().trim();

        return repository.findPredictionAvailabilities(request.predictionIds()).collectList()
            .flatMap(predictions -> {
                Set<Long> foundIds = new HashSet<>();
                for (WriteRepository.PredictionAvailabilityRow prediction : predictions) {
                    foundIds.add(prediction.id());
                    if (prediction.recommendedSide() == null) {
                        return Mono.error(new IllegalArgumentException("One or more predictions are invalid or not available for betting"));
                    }
                }
                if (foundIds.size() != request.predictionIds().size()) {
                    return Mono.error(new IllegalArgumentException("One or more predictions are invalid or not available for betting"));
                }

                String parlayId = UUID.randomUUID().toString();
                return repository.createParlayWithBets(
                    parlayId,
                    request.userId(),
                    request.stake(),
                    request.odds(),
                    platform,
                    request.predictionIds(),
                    request.predictionIds().stream().map(id -> UUID.randomUUID().toString()).toList()
                );
            });
    }

    public Mono<Void> updateBet(UpdateBetRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.betId() == null || request.betId().isBlank()) {
            return Mono.error(new IllegalArgumentException("Bet is required"));
        }
        if (request.stake() == null || request.stake() <= 0) {
            return Mono.error(new IllegalArgumentException("Stake must be positive"));
        }
        if (request.odds() == null || request.odds() <= 1) {
            return Mono.error(new IllegalArgumentException("Odds must be greater than 1.00"));
        }
        String platform = request.platform() == null || request.platform().isBlank() ? DEFAULT_PLATFORM : request.platform().trim();
        return repository.updateBet(request.userId(), request.betId(), request.stake(), request.odds(), platform);
    }

    public Mono<Void> setBetResult(SetBetResultRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.betId() == null || request.betId().isBlank()) {
            return Mono.error(new IllegalArgumentException("Bet is required"));
        }

        String result = request.result() == null ? "" : request.result().trim().toUpperCase();
        if ("OPEN".equals(result)) {
            return repository.clearBetResult(request.userId(), request.betId());
        }
        if (!"GREEN".equals(result) && !"RED".equals(result)) {
            return Mono.error(new IllegalArgumentException("Result must be GREEN, RED, or OPEN"));
        }

        return repository.findBetSettlementInput(request.userId(), request.betId())
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Bet was not found")))
            .flatMap(bet -> {
                Double profit = calculateProfit(result, bet.stake(), bet.odds());
                Boolean won = "GREEN".equals(result);
                return repository.upsertBetResult(UUID.randomUUID().toString(), request.userId(), bet.betId(), won, profit);
            });
    }

    public Mono<Void> deleteBet(DeleteBetRequest request) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User is required"));
        }
        if (request.betId() == null || request.betId().isBlank()) {
            return Mono.error(new IllegalArgumentException("Bet is required"));
        }
        return repository.deleteBet(request.userId(), request.betId());
    }

    public Mono<Void> createUser(CreateUserRequest request) {
        String email = request.email() == null ? null : request.email().trim();
        if (email == null || email.isBlank()) {
            return Mono.error(new IllegalArgumentException("Email is required"));
        }
        if (!email.contains("@")) {
            return Mono.error(new IllegalArgumentException("Invalid email address"));
        }
        if (request.password() == null || request.password().length() < 8) {
            return Mono.error(new IllegalArgumentException("Password must be at least 8 characters"));
        }
        return repository.findUserByEmail(email)
            .flatMap(existing -> Mono.<Void>error(new IllegalArgumentException("A user with email " + email + " already exists")))
            .switchIfEmpty(Mono.defer(() -> repository.createUser(
                UUID.randomUUID().toString(),
                email,
                request.displayName(),
                BCrypt.hashpw(request.password(), BCrypt.gensalt(BCRYPT_ROUNDS))
            )));
    }

    public Mono<Void> toggleUserActive(ToggleUserActiveRequest request, String actingUserId) {
        if (request.userId() == null || request.userId().isBlank()) {
            return Mono.error(new IllegalArgumentException("User ID is required"));
        }
        if (actingUserId != null && actingUserId.equals(request.userId())) {
            return Mono.error(new IllegalArgumentException("You cannot deactivate your own account"));
        }
        boolean nextActive = request.currentActive() == null || !request.currentActive();
        return repository.updateUserActive(request.userId(), nextActive);
    }

    private String normalizeBcryptHash(String passwordHash) {
        if (passwordHash != null && passwordHash.startsWith("$2b$")) {
            return "$2a$" + passwordHash.substring(4);
        }
        return passwordHash;
    }

    private Double calculateProfit(String result, Double stake, Double odds) {
        if (stake == null || odds == null) {
            return null;
        }
        if ("GREEN".equals(result)) {
            return stake * (odds - 1);
        }
        if ("RED".equals(result)) {
            return -stake;
        }
        return 0.0;
    }
}
