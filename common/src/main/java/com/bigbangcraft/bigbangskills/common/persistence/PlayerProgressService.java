package com.bigbangcraft.bigbangskills.common.persistence;

import com.bigbangcraft.bigbangskills.api.ProgressionScope;
import com.bigbangcraft.bigbangskills.api.XpSource;
import com.bigbangcraft.bigbangskills.common.config.RuntimePersistenceConfig;
import com.bigbangcraft.bigbangskills.common.progression.PlayerProgress;
import com.bigbangcraft.bigbangskills.common.progression.SkillProgress;
import com.bigbangcraft.bigbangskills.common.skill.BlockBreakAction;
import com.bigbangcraft.bigbangskills.common.skill.GameplayService;
import com.bigbangcraft.bigbangskills.common.skill.SkillRegistry;
import com.bigbangcraft.bigbangskills.common.skill.SkillAwardAction;
import com.bigbangcraft.bigbangskills.common.skill.SkillRelationships;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** MAIN THREAD API; repository continuations return here through mainExecutor. */
public final class PlayerProgressService implements AutoCloseable {
    public enum State { LOADING, READY, DIRTY, SAVING, FAILED, UNLOADING }
    public record AdminResult(boolean accepted, UUID playerId, com.bigbangcraft.bigbangskills.api.SkillId skillId, BigDecimal oldXp, BigDecimal newXp, int oldLevel, int newLevel, String reason) {}
    private record PendingAction(BlockBreakAction action, BigDecimal miningXp, BigDecimal woodcuttingXp, SkillAwardAction award) {}
    private record PendingSave(UUID eventId, UUID playerId, com.bigbangcraft.bigbangskills.api.SkillId skillId, ProgressionScope scope, BigDecimal amount, XpSource source, String reason) {}
    private record LeaderboardCache(List<LeaderboardRow> rows, Instant refreshedAt) {}
    private static final class Entry {
        private final UUID playerId;
        private final ProgressionScope scope;
        private final ArrayDeque<PendingAction> preload = new ArrayDeque<>();
        private final ArrayDeque<PendingSave> saves = new ArrayDeque<>();
        private PlayerProgress progress;
        private State state = State.LOADING;
        private boolean loadStarted;
        private boolean flushInFlight;
        private int retryAttempt;
        private Instant retryAt = Instant.MIN;
        private CompletableFuture<Void> unload = new CompletableFuture<>();

        private Entry(UUID playerId, ProgressionScope scope) { this.playerId = playerId; this.scope = scope; }
    }

    private final ProgressRepository repository;
    private final SkillRegistry registry;
    private final GameplayService gameplay;
    private final RuntimePersistenceConfig config;
    private final Executor mainExecutor;
    private final ScheduledExecutorService scheduler;
    private final Consumer<String> log;
    private final Clock clock;
    private final Map<UUID, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, LeaderboardCache> leaderboardCache = new ConcurrentHashMap<>();
    private final AtomicLong loads = new AtomicLong();
    private final AtomicLong loadFailures = new AtomicLong();
    private final AtomicLong saves = new AtomicLong();
    private final AtomicLong saveFailures = new AtomicLong();
    private volatile boolean accepting = true;
    private volatile boolean databaseHealthy;
    private volatile String databaseDriver = "unknown";
    private volatile Instant lastSuccessfulFlush;
    private volatile String lastFailedFlush = "none";
    private volatile long lastDatabaseLatencyMillis;
    private ScheduledFuture<?> periodicFlush;

    public PlayerProgressService(ProgressRepository repository, SkillRegistry registry, GameplayService gameplay, RuntimePersistenceConfig config, Executor mainExecutor, ScheduledExecutorService scheduler, Consumer<String> log) {
        this(repository, registry, gameplay, config, mainExecutor, scheduler, log, Clock.systemUTC());
    }

    public PlayerProgressService(ProgressRepository repository, SkillRegistry registry, GameplayService gameplay, RuntimePersistenceConfig config, Executor mainExecutor, ScheduledExecutorService scheduler, Consumer<String> log, Clock clock) {
        this.repository = repository;
        this.registry = registry;
        this.gameplay = gameplay;
        this.config = config;
        this.mainExecutor = mainExecutor;
        this.scheduler = scheduler;
        this.log = log;
        this.clock = clock;
    }

    public void start(Supplier<CompletionStage<Void>> initializer) {
        periodicFlush = scheduler.scheduleAtFixedRate(() -> mainExecutor.execute(this::flush), config.flushIntervalSeconds(), config.flushIntervalSeconds(), TimeUnit.SECONDS);
        attemptInitialization(initializer, 0);
    }

    private void attemptInitialization(Supplier<CompletionStage<Void>> initializer, int attempt) {
        if (!accepting) return;
        CompletionStage<Void> result;
        try { result = initializer.get(); } catch (Throwable failure) { result = CompletableFuture.failedFuture(failure); }
        result.whenComplete((ignored, failure) -> mainExecutor.execute(() -> {
            if (!accepting) return;
            if (failure == null) {
                databaseHealthy = true;
                entries.values().forEach(this::startLoad);
                return;
            }
            databaseHealthy = false;
            log.accept("BigBangSkills database unavailable during startup: " + message(failure) + "; gameplay remains queued with bounded limits");
            var next = Math.min(attempt + 1, config.retryBackoff().size() - 1);
            scheduler.schedule(() -> attemptInitialization(initializer, next), config.retryBackoff().get(next).toSeconds(), TimeUnit.SECONDS);
        }));
    }

    public void setDatabaseDriver(String driver) { databaseDriver = driver == null || driver.isBlank() ? "unknown" : driver; }

    public void load(UUID playerId, ProgressionScope scope) {
        if (!accepting) return;
        var entry = entries.computeIfAbsent(playerId, id -> new Entry(id, scope));
        if (!entry.scope.equals(scope)) throw new IllegalStateException("Progression scope changed while player is cached");
        if (entry.state == State.UNLOADING) {
            entry.unload.whenComplete((ignored, failure) -> mainExecutor.execute(() -> {
                if (!accepting) return;
                if (entries.get(playerId) == entry) { entry.progress = null; entry.state = State.LOADING; entry.retryAttempt = 0; startLoad(entry); }
                else load(playerId, scope);
            }));
            return;
        }
        if (databaseHealthy) startLoad(entry);
    }

    private void startLoad(Entry entry) {
        if (!accepting || !databaseHealthy || entry.loadStarted || entry.state == State.UNLOADING || entry.state == State.READY || entry.state == State.DIRTY || entry.state == State.SAVING) return;
        entry.loadStarted = true;
        entry.state = State.LOADING;
        repository.loadAll(entry.playerId, registry.snapshot().keySet(), entry.scope).whenComplete((rows, failure) -> mainExecutor.execute(() -> finishLoad(entry, rows, failure)));
    }

    private void finishLoad(Entry entry, List<ProgressRow> rows, Throwable failure) {
        entry.loadStarted = false;
        if (failure != null) {
            loadFailures.incrementAndGet();
            databaseHealthy = false;
            entry.state = State.FAILED;
            entry.retryAt = clock.instant().plus(nextBackoff(entry.retryAttempt++));
            lastFailedFlush = "load: " + message(failure);
            log.accept("BigBangSkills profile load failed for " + entry.playerId + ": " + message(failure));
            scheduler.schedule(() -> mainExecutor.execute(() -> { if (entries.get(entry.playerId) == entry) { databaseHealthy = true; startLoad(entry); } }), Math.max(1, Duration.between(clock.instant(), entry.retryAt).toSeconds()), TimeUnit.SECONDS);
            return;
        }
        var unloading = entry.state == State.UNLOADING;
        var progress = new PlayerProgress(entry.playerId);
        registry.snapshot().forEach((id, definition) -> progress.put(new SkillProgress(id, BigDecimal.ZERO, 1, 0)));
        rows.forEach(row -> registry.get(row.skillId()).ifPresent(definition -> progress.put(new SkillProgress(row.skillId(), row.totalXp(), definition.curve().levelAt(row.totalXp(), definition.maxLevel()), row.revision()))));
        progress.refreshDerived();
        entry.progress = progress;
        loads.incrementAndGet();
        databaseHealthy = true;
        entry.state = unloading ? State.UNLOADING : State.READY;
        while (!entry.preload.isEmpty()) {
            var pending = entry.preload.removeFirst();
            var result = processAction(entry, pending);
            if ("persistence_queue_full".equals(result.reason())) { entry.preload.addFirst(pending); break; }
        }
        if (entry.saves.isEmpty()) {
            if (unloading) finishUnload(entry); else entry.state = State.READY;
        } else if (unloading) flush();
    }

    public GameplayService.Outcome blockBreak(BlockBreakAction action, BigDecimal miningXp, BigDecimal woodcuttingXp) {
        var entry = entries.get(action.playerId());
        if (entry == null) return rejected("profile_not_loaded");
        if (entry.state == State.LOADING || entry.state == State.FAILED) {
            if (entry.preload.size() >= config.maxPreloadXpPerPlayer()) return rejected("profile_loading_queue_full");
            entry.preload.addLast(new PendingAction(action, miningXp, woodcuttingXp, null));
            return rejected("profile_loading_queued");
        }
        if (entry.state == State.UNLOADING || !accepting) return rejected("profile_unloading");
        if (entry.saves.size() >= config.maxPendingSaveEventsPerPlayer()) return rejected("persistence_queue_full");
        return processAction(entry, new PendingAction(action, miningXp, woodcuttingXp, null));
    }

    public GameplayService.Outcome blockBreak(BlockBreakAction action) {
        var entry = entries.get(action.playerId());
        if (entry == null) return rejected("profile_not_loaded");
        if (entry.state == State.LOADING || entry.state == State.FAILED) {
            if (entry.preload.size() >= config.maxPreloadXpPerPlayer()) return rejected("profile_loading_queue_full");
            entry.preload.addLast(new PendingAction(action, null, null, null));
            return rejected("profile_loading_queued");
        }
        if (entry.state == State.UNLOADING || !accepting) return rejected("profile_unloading");
        if (entry.saves.size() >= config.maxPendingSaveEventsPerPlayer()) return rejected("persistence_queue_full");
        return processAction(entry, new PendingAction(action, null, null, null));
    }

    public GameplayService.Outcome award(SkillAwardAction action) {
        var entry = entries.get(action.playerId());
        if (entry == null) return rejected("profile_not_loaded");
        if (entry.state == State.LOADING || entry.state == State.FAILED) {
            if (entry.preload.size() >= config.maxPreloadXpPerPlayer()) return rejected("profile_loading_queue_full");
            entry.preload.addLast(new PendingAction(null, null, null, action));
            return rejected("profile_loading_queued");
        }
        if (entry.state == State.UNLOADING || !accepting) return rejected("profile_unloading");
        if (entry.saves.size() >= config.maxPendingSaveEventsPerPlayer()) return rejected("persistence_queue_full");
        return processAction(entry, new PendingAction(null, null, null, action));
    }

    private GameplayService.Outcome processAction(Entry entry, PendingAction pending) {
        if (entry.saves.size() >= config.maxPendingSaveEventsPerPlayer()) return rejected("persistence_queue_full");
        if (pending.award() != null && SkillRelationships.isChild(pending.award().skillId())) return processChildAward(entry, pending.award());
        var result = pending.award() != null
                ? gameplay.award(entry.progress, pending.award())
                : pending.miningXp() == null
                    ? gameplay.blockBreak(entry.progress, pending.action(), entry.scope)
                    : gameplay.blockBreak(entry.progress, pending.action(), pending.miningXp(), pending.woodcuttingXp(), entry.scope);
        if (result.accepted()) {
            var playerId = pending.award() == null ? pending.action().playerId() : pending.award().playerId();
            entry.saves.addLast(new PendingSave(result.requestId(), playerId, result.skillId(), result.scope(), result.amount(), pending.award() == null ? XpSource.BLOCK_BREAK : pending.award().source(), pending.award() == null ? pending.action().blockId() : pending.award().reason()));
            entry.state = State.DIRTY;
        }
        return result;
    }

    private GameplayService.Outcome processChildAward(Entry entry, SkillAwardAction child) {
        var configurationRejection = gameplay.configurationRejection(child);
        if (configurationRejection != null) return rejected(configurationRejection);
        var parents = SkillRelationships.parents(child.skillId());
        if (entry.saves.size() + parents.size() > config.maxPendingSaveEventsPerPlayer()) return rejected("persistence_queue_full");
        var before = entry.progress.get(child.skillId()) == null ? 1 : entry.progress.get(child.skillId()).level();
        var split = child.amount().divide(BigDecimal.valueOf(parents.size()), 8, java.math.RoundingMode.DOWN);
        var total = BigDecimal.ZERO;
        UUID requestId = null;
        String reason = "zero_xp";
        for (var parent : parents) {
            var parentAction = new SkillAwardAction(child.playerId(), parent, split, child.source(), child.reason(), child.scope(), child.realPlayer(), child.eventCancelled(), child.pvp(), child.pve());
            var result = gameplay.award(entry.progress, parentAction);
            reason = result.reason();
            if (!result.accepted()) continue;
            if (requestId == null) requestId = result.requestId();
            total = total.add(result.amount());
            entry.saves.addLast(new PendingSave(result.requestId(), child.playerId(), parent, child.scope(), result.amount(), child.source(), child.reason()));
        }
        entry.progress.refreshDerived();
        var after = entry.progress.get(child.skillId()) == null ? before : entry.progress.get(child.skillId()).level();
        if (total.signum() > 0) entry.state = State.DIRTY;
        return new GameplayService.Outcome(total.signum() > 0, child.skillId(), total, total.signum() > 0 ? "accepted" : reason, requestId, child.scope(), before, after);
    }

    public Optional<PlayerProgress> progress(UUID playerId) {
        var entry = entries.get(playerId);
        if (entry == null || entry.progress == null || entry.state == State.LOADING) return Optional.empty();
        entry.progress.refreshDerived();
        return Optional.of(entry.progress);
    }

    public Map<UUID, PlayerProgress> progressSnapshot() {
        return entries.values().stream().filter(entry -> entry.progress != null).peek(entry -> entry.progress.refreshDerived()).collect(java.util.stream.Collectors.toUnmodifiableMap(entry -> entry.playerId, entry -> entry.progress));
    }

    public State state(UUID playerId) { var entry = entries.get(playerId); return entry == null ? State.FAILED : entry.state; }

    /** MAIN THREAD API; offline reads/writes stay on the repository executor. */
    public CompletionStage<AdminResult> adminSet(UUID playerId, com.bigbangcraft.bigbangskills.api.SkillId skillId, BigDecimal totalXp, ProgressionScope scope, String reason) {
        if (!accepting || totalXp == null || totalXp.signum() < 0) return CompletableFuture.completedFuture(adminRejected(playerId, skillId, "invalid_xp"));
        if (SkillRelationships.isChild(skillId)) return CompletableFuture.completedFuture(adminRejected(playerId, skillId, "derived_skill"));
        var definition = registry.get(skillId).orElse(null);
        if (definition == null) return CompletableFuture.completedFuture(adminRejected(playerId, skillId, "unknown_skill"));
        var entry = entries.get(playerId);
        if (entry != null) {
            if (entry.progress == null || entry.state == State.LOADING || entry.state == State.FAILED || entry.state == State.UNLOADING) return CompletableFuture.completedFuture(adminRejected(playerId, skillId, "profile_not_ready"));
            return CompletableFuture.completedFuture(applyAdmin(entry, skillId, totalXp, reason));
        }
        return repository.load(playerId, skillId, scope).thenCompose(row -> {
            var oldXp = row.map(ProgressRow::totalXp).orElse(BigDecimal.ZERO);
            var oldLevel = definition.curve().levelAt(oldXp, definition.maxLevel());
            var newLevel = definition.curve().levelAt(totalXp, definition.maxLevel());
            var delta = totalXp.subtract(oldXp);
            if (delta.signum() == 0) return CompletableFuture.completedFuture(new AdminResult(true, playerId, skillId, oldXp, totalXp, oldLevel, newLevel, "unchanged"));
            return repository.applyDelta(UUID.randomUUID(), playerId, skillId, scope, delta, XpSource.ADMIN, reason).thenApply(saved -> saved
                ? new AdminResult(true, playerId, skillId, oldXp, totalXp, oldLevel, newLevel, "accepted")
                : new AdminResult(false, playerId, skillId, oldXp, oldXp, oldLevel, oldLevel, "persistence_rejected"));
        });
    }

    public CompletionStage<AdminResult> adminAdjust(UUID playerId, com.bigbangcraft.bigbangskills.api.SkillId skillId, BigDecimal delta, ProgressionScope scope, String reason) {
        if (!accepting || delta == null) return CompletableFuture.completedFuture(adminRejected(playerId, skillId, "invalid_xp"));
        var entry = entries.get(playerId);
        if (entry != null && entry.progress != null && entry.state != State.LOADING && entry.state != State.FAILED) {
            var current = entry.progress.get(skillId);
            return adminSet(playerId, skillId, (current == null ? BigDecimal.ZERO : current.totalXp()).add(delta), scope, reason);
        }
        return repository.load(playerId, skillId, scope).thenCompose(row -> adminSet(playerId, skillId, row.map(ProgressRow::totalXp).orElse(BigDecimal.ZERO).add(delta), scope, reason));
    }

    public CompletionStage<List<LeaderboardRow>> leaderboard(com.bigbangcraft.bigbangskills.api.SkillId skillId, ProgressionScope scope, int limit) {
        var key = skillId + "|" + scope;
        var cached = leaderboardCache.get(key);
        if (cached != null && Duration.between(cached.refreshedAt(), clock.instant()).compareTo(Duration.ofSeconds(30)) < 0) return CompletableFuture.completedFuture(cached.rows().stream().limit(Math.min(limit, 100)).toList());
        return repository.leaderboard(skillId, scope, 100).thenApply(rows -> { leaderboardCache.put(key, new LeaderboardCache(List.copyOf(rows), clock.instant())); return rows.stream().limit(Math.min(limit, 100)).toList(); });
    }

    private AdminResult applyAdmin(Entry entry, com.bigbangcraft.bigbangskills.api.SkillId skillId, BigDecimal totalXp, String reason) {
        var definition = registry.get(skillId).orElseThrow();
        var current = entry.progress.get(skillId);
        var oldXp = current == null ? BigDecimal.ZERO : current.totalXp();
        var oldLevel = current == null ? 1 : current.level();
        var newLevel = definition.curve().levelAt(totalXp, definition.maxLevel());
        if (oldXp.compareTo(totalXp) == 0) return new AdminResult(true, entry.playerId, skillId, oldXp, totalXp, oldLevel, newLevel, "unchanged");
        if (entry.saves.size() >= config.maxPendingSaveEventsPerPlayer()) return adminRejected(entry.playerId, skillId, "persistence_queue_full");
        entry.progress.put(new SkillProgress(skillId, totalXp, newLevel, current == null ? 1 : current.revision() + 1));
        entry.progress.refreshDerived();
        entry.saves.addLast(new PendingSave(UUID.randomUUID(), entry.playerId, skillId, entry.scope, totalXp.subtract(oldXp), XpSource.ADMIN, reason));
        leaderboardCache.remove(skillId + "|" + entry.scope);
        entry.state = State.DIRTY;
        return new AdminResult(true, entry.playerId, skillId, oldXp, totalXp, oldLevel, newLevel, "accepted");
    }

    private static AdminResult adminRejected(UUID playerId, com.bigbangcraft.bigbangskills.api.SkillId skillId, String reason) { return new AdminResult(false, playerId, skillId, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, reason); }

    public void flush() {
        var now = clock.instant();
        entries.values().forEach(entry -> { if (!entry.saves.isEmpty() && !entry.flushInFlight && !now.isBefore(entry.retryAt)) startFlush(entry); });
    }

    private void startFlush(Entry entry) {
        if (entry.flushInFlight || entry.saves.isEmpty()) return;
        entry.flushInFlight = true;
        var unloading = entry.state == State.UNLOADING;
        entry.state = State.SAVING;
        var batch = List.copyOf(entry.saves);
        var started = System.nanoTime();
        CompletionStage<Void> chain = CompletableFuture.completedFuture(null);
        for (var event : batch) chain = chain.thenCompose(ignored -> repository.applyDelta(event.eventId(), event.playerId(), event.skillId(), event.scope(), event.amount(), event.source(), event.reason()).thenApply(saved -> null));
        chain.whenComplete((ignored, failure) -> mainExecutor.execute(() -> finishFlush(entry, batch, unloading, started, failure)));
    }

    private void finishFlush(Entry entry, List<PendingSave> batch, boolean unloading, long started, Throwable failure) {
        entry.flushInFlight = false;
        lastDatabaseLatencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
        if (failure != null) {
            saveFailures.incrementAndGet();
            databaseHealthy = false;
            entry.state = State.FAILED;
            entry.retryAt = clock.instant().plus(nextBackoff(entry.retryAttempt++));
            lastFailedFlush = message(failure);
            log.accept("BigBangSkills flush failed for " + entry.playerId + ": " + message(failure) + "; pending events kept=" + entry.saves.size());
            var delay = Math.max(1, Duration.between(clock.instant(), entry.retryAt).toSeconds());
            scheduler.schedule(() -> mainExecutor.execute(this::flush), delay, TimeUnit.SECONDS);
            return;
        }
        var ids = batch.stream().map(PendingSave::eventId).collect(java.util.stream.Collectors.toSet());
        entry.saves.removeIf(event -> ids.contains(event.eventId()));
        saves.addAndGet(batch.size());
        entry.retryAttempt = 0;
        entry.retryAt = Instant.MIN;
        databaseHealthy = true;
        lastSuccessfulFlush = clock.instant();
        lastFailedFlush = "none";
        if (!unloading) {
            while (!entry.preload.isEmpty()) {
                var pending = entry.preload.removeFirst();
                var result = processAction(entry, pending);
                if ("persistence_queue_full".equals(result.reason())) { entry.preload.addFirst(pending); break; }
            }
        }
        entry.state = entry.saves.isEmpty() ? (unloading ? State.UNLOADING : State.READY) : State.DIRTY;
        if (unloading && entry.saves.isEmpty()) finishUnload(entry);
    }

    public CompletionStage<Void> unload(UUID playerId) {
        var entry = entries.get(playerId);
        if (entry == null) return CompletableFuture.completedFuture(null);
        entry.state = State.UNLOADING;
        if (!entry.loadStarted && entry.progress != null && entry.saves.isEmpty()) finishUnload(entry); else flush();
        return entry.unload;
    }

    private void finishUnload(Entry entry) {
        if (entry.flushInFlight || entry.loadStarted || entry.progress == null || !entry.saves.isEmpty()) return;
        if (entries.remove(entry.playerId, entry)) entry.unload.complete(null);
    }

    public CompletionStage<Void> shutdown() {
        accepting = false;
        if (periodicFlush != null) periodicFlush.cancel(false);
        entries.values().forEach(entry -> { entry.state = State.UNLOADING; if (entry.saves.isEmpty()) finishUnload(entry); });
        flush();
        var futures = entries.values().stream().map(entry -> entry.unload).toArray(CompletableFuture[]::new);
        var result = CompletableFuture.allOf(futures).orTimeout(config.shutdownFlushTimeoutSeconds(), TimeUnit.SECONDS);
        return result.handle((ignored, failure) -> {
            if (failure != null) log.accept("BigBangSkills shutdown flush timed out; cached players=" + entries.size() + ", pending operations=" + status().pendingOperations());
            repository.close();
            scheduler.shutdown();
            return null;
        });
    }

    public PersistenceStatus status() {
        var counts = entries.values().stream().collect(java.util.stream.Collectors.groupingBy(entry -> entry.state, java.util.stream.Collectors.counting()));
        var pending = entries.values().stream().mapToInt(entry -> entry.saves.size()).sum();
        return new PersistenceStatus(databaseHealthy, accepting, databaseDriver, entries.size(), count(counts, State.LOADING), (int) entries.values().stream().filter(entry -> entry.state == State.DIRTY || !entry.saves.isEmpty()).count(), count(counts, State.SAVING), count(counts, State.FAILED), pending, loads.get(), loadFailures.get(), saves.get(), saveFailures.get(), lastDatabaseLatencyMillis, lastSuccessfulFlush, lastFailedFlush);
    }

    private Duration nextBackoff(int attempt) { return config.retryBackoff().get(Math.min(attempt, config.retryBackoff().size() - 1)); }
    private static int count(Map<State, Long> counts, State state) { return counts.getOrDefault(state, 0L).intValue(); }
    private static GameplayService.Outcome rejected(String reason) { return new GameplayService.Outcome(false, null, BigDecimal.ZERO, reason, null, null, 0, 0, com.bigbangcraft.bigbangskills.common.skill.BlockBreakEffect.none()); }
    private static String message(Throwable failure) { var cause = failure; while (cause.getCause() != null && (cause instanceof java.util.concurrent.CompletionException || cause instanceof java.util.concurrent.ExecutionException)) cause = cause.getCause(); return cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage()); }
    @Override public void close() { shutdown(); }
}
