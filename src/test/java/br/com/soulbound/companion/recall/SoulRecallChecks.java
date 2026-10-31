package br.com.soulbound.companion.recall;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import br.com.soulbound.companion.SoulStaffAccess;
import br.com.soulbound.companion.identity.CompanionStatus;
import br.com.soulbound.companion.identity.SoulRecallState;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.companion.identity.SoulboundSavedData;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Tests real migration/codecs, queue and search policy. Live chunk loading/teleportation remain integration tests. */
public final class SoulRecallChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var nether = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_nether"));
        var end = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace("the_end"));
        var base = SoulboundRecord.create(UUID.randomUUID(), "RecallOwner");
        var active = new SoulboundRecord(base.ownerUuid(), base.ownerLastKnownName(), base.bondUuid(), base.companionUuid(),
                "LUPUS", "BABY", "STAY", CompanionStatus.ACTIVE, false, 73.5, 123456789L, true, false);
        var location = SoulRecallState.EMPTY.at(nether, new Vec3(123.25, 70.5, -48.5), 1000).withCooldown(1600);
        var tracked = active.withRecall(location);
        var json = SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE, tracked).getOrThrow().getAsJsonObject();
        for (String key : new String[] { "lastKnownDimension", "lastKnownPosition", "lastKnownUpdate", "recallCooldownEnd" }) {
            require(json.has(key), "Canonical recall field must be flat on disk: " + key);
        }
        require(!json.has("recall"), "Internal grouping must not change the requested disk schema");
        require(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow().equals(tracked), "Recall fields must round trip");
        for (String key : new String[] { "lastKnownDimension", "lastKnownPosition", "lastKnownUpdate", "recallCooldownEnd" }) json.remove(key);
        var migrated = SoulboundRecord.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        require(migrated.recall().equals(SoulRecallState.EMPTY), "Old records must migrate without inventing a location or cooldown");
        require(migrated.ownerUuid().equals(active.ownerUuid()) && migrated.bondUuid().equals(active.bondUuid())
                && migrated.companionUuid().equals(active.companionUuid()) && migrated.evolutionProgress() == 73.5,
                "Migration must preserve existing identity and progression");
        require(tracked.withName("Renamed").recall().equals(location), "Name update must retain recall state");
        require(tracked.withDelivery(true, true).recall().equals(location), "Item recovery must retain recall state");
        require(tracked.withState(CompanionStatus.ACTIVE, false, "FOLLOW_PASSIVE").recall().equals(location), "Follow/Stay must retain recall state");
        var afterRecall = tracked.withState(CompanionStatus.ACTIVE, false, "FOLLOW_PASSIVE")
                .withRecall(location.at(end, new Vec3(5.0, 80.0, 7.0), 2000).withCooldown(2600));
        require(afterRecall.companionStatus() == CompanionStatus.ACTIVE && afterRecall.behaviorMode().equals("FOLLOW_PASSIVE"), "Recall from Stay must finish Active/Follow");
        require(afterRecall.matches(tracked.ownerUuid(), tracked.bondUuid(), tracked.companionUuid()), "Cross-dimension recall must retain all canonical UUIDs");
        require(afterRecall.species().equals(tracked.species()) && afterRecall.evolutionStage().equals(tracked.evolutionStage())
                && afterRecall.evolutionProgress() == tracked.evolutionProgress() && afterRecall.godCooldownEnd() == tracked.godCooldownEnd(),
                "Recall must preserve species, stage, progress and GOD cooldown");
        require(afterRecall.lastKnownDimension().orElseThrow().equals(end) && afterRecall.lastKnownUpdate() == 2000,
                "Confirmed recall must store its new dimension and timestamp");
        require(location.remainingSeconds(1000) == 30, "Default recall cooldown must be thirty seconds");
        require(location.remainingSeconds(1001) == 30 && location.remainingSeconds(1599) == 1, "Remaining cooldown must round up");
        require(location.remainingSeconds(1600) == 0 && location.remainingSeconds(1700) == 0, "Cooldown must end at its deadline");
        require(SoulRecallState.EMPTY.remainingSeconds(1000) == 0, "Failed/uncommitted recalls must not gain a cooldown");
        for (var status : CompanionStatus.values()) {
            var candidate = active.withState(status, false, "STAY");
            require(SoulStaffAccess.allows(candidate, candidate.ownerUuid(), candidate.ownerUuid(), candidate.bondUuid())
                    == (status == CompanionStatus.ACTIVE), "Only ACTIVE may request recall: " + status);
        }
        require(!SoulStaffAccess.allows(active, UUID.randomUUID(), active.ownerUuid(), active.bondUuid()), "Another owner must be refused");
        require(!SoulStaffAccess.allows(active, active.ownerUuid(), active.ownerUuid(), UUID.randomUUID()), "An invalid bond must be refused");
        require(!active.matches(active.ownerUuid(), active.bondUuid(), UUID.randomUUID()), "Another entity must never substitute the canonical companion");
        var queue = new RecallQueue<String>();
        require(queue.offer(active.bondUuid(), "existing-entity", 100), "First recall should enter the queue");
        require(!queue.offer(active.bondUuid(), "replacement", 120), "Concurrent recalls for the same bond must be refused");
        var first = queue.snapshot().getFirst().getValue();
        require(first.value().equals("existing-entity") && first.deadline() == 300, "Repeated requests must not overwrite or extend the operation");
        require(!first.expired(299) && first.expired(300), "Loading must have a bounded timeout");
        for (int i = 1; i < RecallQueue.MAX_PENDING; i++) require(queue.offer(UUID.randomUUID(), "other", 100), "Capacity should accept distinct bonds");
        require(!queue.offer(UUID.randomUUID(), "overload", 100), "Global pending work must be bounded");
        require(queue.remove(active.bondUuid()).value().equals("existing-entity") && !queue.contains(active.bondUuid()), "Finishing must release the bond lock");
        require(queue.offer(active.bondUuid(), "retry", 400), "Failure cleanup must permit a subsequent attempt");
        for (var entry : queue.snapshot()) queue.remove(entry.getKey());
        require(queue.snapshot().isEmpty(), "Shutdown/logout cleanup must be able to drain every request");
        var ownerPosition = new Vec3(100, 64, -100);
        var inspected = new AtomicInteger();
        require(SoulRecallDestinations.selectNear(ownerPosition, position -> { inspected.incrementAndGet(); return false; }) == null,
                "An unsafe destination set must cancel selection");
        require(inspected.get() == 96, "Destination search must remain bounded");
        var allowed = ownerPosition.add(0, 0.7, 3);
        var destination = SoulRecallDestinations.selectNear(ownerPosition, position -> position.distanceToSqr(allowed) < 0.000001);
        require(destination != null && destination.distanceToSqr(allowed) < 0.000001, "Selection must use a safe nearby candidate");
        require(destination.distanceToSqr(ownerPosition) < 25, "Recall must remain near the owner");
        require(!SoulRecallState.validPosition(new Vec3(Double.NaN, 64, 0))
                && !SoulRecallState.validPosition(new Vec3(30_000_001, 64, 0)), "Corrupt/out-of-world locations must not trigger loading");
        var data = new SoulboundSavedData();
        data.put(afterRecall);
        var root = new CompoundTag();
        root.put("data", SoulboundSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow());
        var folder = Path.of("build", "recall-checks");
        Files.createDirectories(folder);
        var path = folder.resolve("soulbound_identity.dat");
        NbtIo.writeCompressed(root, path);
        var disk = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        var restored = SoulboundSavedData.CODEC.parse(NbtOps.INSTANCE, disk.get("data")).getOrThrow().find(active.ownerUuid());
        require(restored.equals(afterRecall), "Restart must retain location, cooldown and all original fields");
        require(restored.recall().remainingSeconds(2010) == 30, "Persisted cooldown must continue after restart");
        System.out.println("Soul recall checks passed: " + checks);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
