package br.com.soulbound.companion.identity;

import java.util.List;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtAccounter;

/** Dependency-free executable checks; no claims about live Minecraft gameplay. */
public final class SoulboundIdentityChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var initial = SoulboundRecord.create(UUID.randomUUID(), "FirstName");
        require(initial.canSummon(), "A new identity must be ready exactly once");
        require(!initial.eggDelivered() && initial.eggDeliveryPending(), "Initial delivery must remain pending");
        var progressed = new SoulboundRecord(initial.ownerUuid(), "FirstName", initial.bondUuid(), initial.companionUuid(),
                "LUPUS", "BABY", "STAY", CompanionStatus.ACTIVE, false, 73.5, 987654321L, true, false);
        var renamed = progressed.withName("DifferentName").withDelivery(true, true);
        require(renamed.ownerUuid().equals(initial.ownerUuid()) && renamed.bondUuid().equals(initial.bondUuid()), "A name change must not change identity");
        require(renamed.evolutionProgress() == 73.5 && renamed.godCooldownEnd() == 987654321L, "Egg recovery must not reset progression");
        require(renamed.companionUuid().equals(progressed.companionUuid()) && renamed.behaviorMode().equals("STAY"), "Egg recovery must preserve the companion and mode");
        for (var status : List.of(CompanionStatus.SUMMONING, CompanionStatus.ACTIVE, CompanionStatus.DEAD, CompanionStatus.MISSING)) {
            require(!initial.withState(status, false, "FOLLOW_PASSIVE").canSummon(), "Duplicate summon must be denied for " + status);
        }
        require(!initial.withState(CompanionStatus.READY, true, "FOLLOW_PASSIVE").canSummon(), "Recovery must not resurrect a companion");
        require(!progressed.matches(UUID.randomUUID(), progressed.bondUuid(), progressed.companionUuid()), "Another owner must not match");
        require(!progressed.matches(progressed.ownerUuid(), UUID.randomUUID(), progressed.companionUuid()), "A forged bond must not match");
        require(!progressed.matches(progressed.ownerUuid(), progressed.bondUuid(), UUID.randomUUID()), "A second companion must not match");
        var encoded = SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE, renamed).getOrThrow();
        require(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(renamed), "All record fields must round trip");
        var data = new SoulboundSavedData();
        data.put(renamed);
        var second = SoulboundRecord.create(UUID.randomUUID(), "OtherPlayer");
        data.put(second);
        var disk = SoulboundSavedData.CODEC.encodeStart(JsonOps.INSTANCE, data).getOrThrow();
        var restored = SoulboundSavedData.CODEC.parse(JsonOps.INSTANCE, disk).getOrThrow();
        require(restored.find(renamed.ownerUuid()).equals(renamed), "Restart must preserve the first identity");
        require(restored.find(second.ownerUuid()).equals(second), "Restart must preserve the second identity");
        var root = new CompoundTag();
        root.put("data", SoulboundSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow());
        var folder = Path.of("build", "identity-checks");
        Files.createDirectories(folder);
        var path = folder.resolve("soulbound_identity.dat");
        NbtIo.writeCompressed(root, path);
        var read = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        var fromDisk = SoulboundSavedData.CODEC.parse(NbtOps.INSTANCE, read.get("data")).getOrThrow();
        require(fromDisk.find(renamed.ownerUuid()).equals(renamed), "Compressed NBT on disk must preserve progression and identity");
        require(fromDisk.find(second.ownerUuid()).equals(second), "Compressed NBT on disk must preserve pending initial delivery");
        require(data.isDirty(), "Canonical mutations must mark SavedData dirty");
        var duplicated = SoulboundRecord.CODEC.listOf().encodeStart(JsonOps.INSTANCE, List.of(renamed, renamed)).getOrThrow();
        require(SoulboundSavedData.CODEC.parse(JsonOps.INSTANCE, duplicated).result().isEmpty(), "Duplicate owners on disk must be rejected");
        rejects(() -> data.put(SoulboundRecord.create(renamed.ownerUuid(), "Replacement")), "Replacing a permanent bond must fail");
        var duplicateEntity = second.adoptLegacy(renamed.companionUuid(), "BABY", "STAY");
        rejects(() -> data.put(duplicateEntity), "Companion UUIDs must be globally unique");
        var changedCompanion = renamed.adoptLegacy(UUID.randomUUID(), "BABY", "STAY");
        rejects(() -> data.put(changedCompanion), "An active companion identity cannot be replaced");
        var legacy = initial.adoptLegacy(UUID.randomUUID(), "BABY", "STAY");
        require(legacy.bondUuid().equals(initial.bondUuid()) && legacy.companionStatus() == CompanionStatus.ACTIVE
                && legacy.behaviorMode().equals("STAY"), "Legacy adoption must preserve bond and behavior");
        System.out.println("Soulbound identity checks passed: " + checks);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action, String message) {
        boolean rejected = false;
        try { action.run(); } catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, message);
    }
}
