package br.com.soulbound.companion.recall;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.companion.SoulStaffCommands;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.companion.identity.SoulboundSavedData;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.registry.ModTickets;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/** Recovers only the canonical entity. No entity factory, synchronous chunk request or unbounded wait is used. */
@EventBusSubscriber(modid = SoulboundCompanions.MODID)
public final class SoulRecallService {
    public static final int COOLDOWN_TICKS = 600;
    private static final Map<MinecraftServer, State> STATES = new IdentityHashMap<>();
    private SoulRecallService() {}

    private record LoadKey(ResourceKey<Level> dimension, ChunkPos chunk) {}
    private static final class Lease {
        final ServerLevel level;
        final CompletableFuture<?> future;
        int references = 1;
        Lease(ServerLevel level, CompletableFuture<?> future) { this.level = level; this.future = future; }
    }
    private static final class Request {
        final SoulboundRecord session;
        final InteractionHand hand;
        final @Nullable LoadKey source;
        boolean holdsLease;
        Request(SoulboundRecord session, InteractionHand hand, @Nullable LoadKey source) {
            this.session = session; this.hand = hand; this.source = source;
        }
    }
    private static final class State {
        final RecallQueue<Request> requests = new RecallQueue<>();
        final Map<LoadKey, Lease> leases = new HashMap<>();
    }
    private static final class RecallFailure extends RuntimeException {
        final String key;
        RecallFailure(String key) { this.key = key; }
    }

    public static long now(MinecraftServer server) { return server.overworld().getGameTime(); }

    public static boolean inProgress(MinecraftServer server, UUID bond) {
        var state = STATES.get(server);
        return state != null && state.requests.contains(bond);
    }

    public static boolean request(ServerPlayer player, InteractionHand hand, SoulboundRecord session) {
        var server = player.level().getServer();
        var record = authorized(player, hand, session);
        if (record == null) { tell(player, "denied"); return false; }
        int seconds = record.recall().remainingSeconds(now(server));
        if (seconds > 0) { tell(player, "cooldown", seconds); return false; }
        br.com.soulbound.companion.AncestralResonance.cancel(player,record);
        var state = STATES.computeIfAbsent(server, ignored -> new State());
        if (state.requests.contains(record.bondUuid())) { tell(player, "pending"); return false; }
        Request request = null;
        try {
            var companion = locate(server, record);
            LoadKey key = null;
            ServerLevel sourceLevel = null;
            if (companion == null) {
                if (record.lastKnownDimension().isEmpty() || record.lastKnownPosition().isEmpty()) throw new RecallFailure("unknown_location");
                sourceLevel = server.getLevel(record.lastKnownDimension().get());
                if (sourceLevel == null) throw new RecallFailure("dimension_missing");
                key = new LoadKey(sourceLevel.dimension(), new ChunkPos(BlockPos.containing(record.lastKnownPosition().get())));
            }
            request = new Request(record, hand, key);
            if (!state.requests.offer(record.bondUuid(), request, now(server))) { tell(player, "busy"); return false; }
            if (companion != null) {
                try { perform(player, record, companion); }
                finally { finish(state, record.bondUuid()); }
            } else {
                acquire(state, key, sourceLevel);
                request.holdsLease = true;
                tell(player, "loading");
            }
            return true;
        } catch (RuntimeException exception) {
            if (request != null) finish(state, record.bondUuid());
            report(player, exception);
            return false;
        }
    }

    private static @Nullable SoulboundRecord authorized(ServerPlayer player, InteractionHand hand, SoulboundRecord session) {
        var record = SoulStaffCommands.authorize(player, player.getItemInHand(hand));
        return record != null && record.matches(session.ownerUuid(), session.bondUuid(), session.companionUuid()) ? record : null;
    }

    private static @Nullable LupusCompanionEntity locate(MinecraftServer server, SoulboundRecord record) {
        LupusCompanionEntity found = null;
        for (var level : server.getAllLevels()) {
            var entity = level.getEntity(record.companionUuid());
            if (entity == null || entity.isRemoved()) continue;
            if (!(entity instanceof LupusCompanionEntity companion)
                    || !record.matches(companion.getOwnerUuid(), companion.getBondUuid(), companion.getUUID())
                    || !SoulboundIdentity.isCanonical(companion) || found != null) throw new RecallFailure("identity_mismatch");
            if (!companion.isAlive()) throw new RecallFailure("dead");
            found = companion;
        }
        return found;
    }

    private static void acquire(State state, LoadKey key, ServerLevel level) {
        var existing = state.leases.get(key);
        if (existing != null) {
            // Tickets in 1.21.11 merge by type and level, not by owner. Share/refcount this lease.
            level.getChunkSource().addTicketWithRadius(ModTickets.SOUL_RECALL.get(), key.chunk(), 0);
            existing.references++;
            return;
        }
        try {
            var future = level.getChunkSource().addTicketAndLoadWithRadius(ModTickets.SOUL_RECALL.get(), key.chunk(), 0);
            state.leases.put(key, new Lease(level, future));
        } catch (RuntimeException exception) {
            level.getChunkSource().removeTicketWithRadius(ModTickets.SOUL_RECALL.get(), key.chunk(), 0);
            throw exception;
        }
    }

    private static void finish(State state, UUID bond) {
        var entry = state.requests.remove(bond);
        if (entry == null || !entry.value().holdsLease) return;
        var request = entry.value();
        request.holdsLease = false;
        var lease = state.leases.get(request.source);
        if (lease != null && --lease.references == 0) {
            state.leases.remove(request.source);
            lease.level.getChunkSource().removeTicketWithRadius(ModTickets.SOUL_RECALL.get(), request.source.chunk(), 0);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var state = STATES.get(server);
        if (state == null) return;
        for (var queued : state.requests.snapshot()) {
            var request = queued.getValue().value();
            var player = server.getPlayerList().getPlayer(request.session.ownerUuid());
            if (player == null) { finish(state, queued.getKey()); continue; }
            try {
                var record = authorized(player, request.hand, request.session);
                if (record == null) throw new RecallFailure("denied");
                if (queued.getValue().expired(now(server))) throw new RecallFailure("timeout");
                if (record.recallCooldownEnd() > now(server)) throw new RecallFailure("denied");
                var companion = locate(server, record);
                if (companion != null) {
                    try { perform(player, record, companion); }
                    finally { finish(state, queued.getKey()); }
                    continue;
                }
                var lease = state.leases.get(request.source);
                if (lease == null) throw new RecallFailure("load_failed");
                if (server.getLevel(request.source.dimension()) != lease.level) throw new RecallFailure("dimension_missing");
                if (!lease.future.isDone()) continue;
                // getNow is nonblocking: world/entity work remains on this server tick, never an IO callback.
                Object result = lease.future.getNow(null);
                if (result instanceof ChunkResult<?> chunk && !chunk.isSuccess()) throw new RecallFailure("load_failed");
                if (lease.level.areEntitiesLoaded(request.source.chunk().toLong())) throw new RecallFailure("not_found");
            } catch (RuntimeException exception) {
                finish(state, queued.getKey());
                report(player, exception);
            }
        }
    }

    private static void perform(ServerPlayer player, SoulboundRecord record, LupusCompanionEntity companion) {
        if (companion.isPassenger() || companion.isVehicle()) throw new RecallFailure("riding");
        Vec3 destination = SoulRecallDestinations.find(player, companion);
        if (destination == null) throw new RecallFailure("unsafe_destination");
        companion.stopFlight();
        var targetLevel = player.level();
        var moved = companion.teleport(new TeleportTransition(targetLevel, destination, Vec3.ZERO,
                player.getYRot(), 0.0F, TeleportTransition.DO_NOTHING));
        // The official cross-dimension API may create a new Java object via restoreFrom, but must retain the UUID.
        if (!(moved instanceof LupusCompanionEntity arrived) || moved.isRemoved()
                || targetLevel.getEntity(record.companionUuid()) != moved
                || !record.matches(arrived.getOwnerUuid(), arrived.getBondUuid(), arrived.getUUID())
                || !SoulboundIdentity.isCanonical(arrived)) throw new RecallFailure("transfer_failed");
        if (!arrived.completeSoulRecall(player)) throw new RecallFailure("transfer_failed");
        var server = targetLevel.getServer();
        var data = SoulboundSavedData.get(server);
        var current = data.find(record.ownerUuid());
        data.put(current.withState(current.companionStatus(), current.resurrectionRequired(), BehaviorMode.FOLLOW_PASSIVE.name())
                .withRecall(current.recall().at(targetLevel.dimension(), arrived.position(), now(server))
                .withCooldown(now(server) + COOLDOWN_TICKS)));
        // Standard SavedData snapshot/write queue, without saveAndJoin on recall or location ticks.
        server.overworld().getDataStorage().scheduleSave();
        targetLevel.sendParticles(ParticleTypes.PORTAL, arrived.getX(), arrived.getY() + 0.5, arrived.getZ(), 30, 0.4, 0.4, 0.4, 0.08);
        targetLevel.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.2F);
        SoulboundIdentity.synchronizePlayer(player);
        tell(player, "success");
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var state = STATES.get(player.level().getServer());
        if (state != null) for (var entry : state.requests.snapshot()) {
            if (entry.getValue().value().session.ownerUuid().equals(player.getUUID())) finish(state, entry.getKey());
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) { clear(event.getServer()); }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) { clear(event.getServer()); }

    private static void clear(MinecraftServer server) {
        var state = STATES.remove(server);
        if (state != null) for (var entry : state.requests.snapshot()) finish(state, entry.getKey());
    }

    private static void report(ServerPlayer player, RuntimeException exception) {
        if (exception instanceof RecallFailure failure) tell(player, failure.key);
        else {
            LogUtils.getLogger().error("Soul recall failed for {}", player.getUUID(), exception);
            tell(player, "load_failed");
        }
    }

    private static void tell(ServerPlayer player, String suffix, Object... args) {
        player.displayClientMessage(Component.translatable("message.soulbound.recall_" + suffix, args), true);
    }
}
