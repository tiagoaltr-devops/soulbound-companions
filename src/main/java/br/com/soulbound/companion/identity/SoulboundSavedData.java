package br.com.soulbound.companion.identity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/** One registry across all dimensions. Invalid on-disk registries fail closed instead of resetting identities. */
public final class SoulboundSavedData extends SavedData {
    public static final Codec<SoulboundSavedData> CODEC = SoulboundRecord.CODEC.listOf().flatXmap(records -> {
        var data = new SoulboundSavedData();
        var bonds = new HashSet<UUID>();
        var companions = new HashSet<UUID>();
        for (var record : records) {
            if (data.records.putIfAbsent(record.ownerUuid(), record) != null
                    || !bonds.add(record.bondUuid()) || !companions.add(record.companionUuid())) {
                return DataResult.error(() -> "Duplicate owner, bond or companion UUID in soulbound registry");
            }
        }
        return DataResult.success(data);
    }, data -> DataResult.success(List.copyOf(data.records.values())));
    public static final SavedDataType<SoulboundSavedData> TYPE =
            new SavedDataType<>("soulbound_identity", SoulboundSavedData::new, CODEC);
    private final Map<UUID, SoulboundRecord> records = new HashMap<>();
    private boolean verifiedLoad;

    public static SoulboundSavedData get(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Identity access requires the server thread");
        var storage = server.overworld().getDataStorage();
        SoulboundSavedData data = storage.get(TYPE);
        if (data == null) {
            if (Files.exists(file(server))) throw new IllegalStateException("Cannot load soulbound_identity.dat; refusing to reset bonds");
            data = storage.computeIfAbsent(TYPE);
        }
        if (!data.verifiedLoad) {
            if (Files.exists(file(server))) data.verifyDisk(server);
            data.verifiedLoad = true;
        }
        return data;
    }

    public @Nullable SoulboundRecord find(UUID owner) { return records.get(owner); }

    public void put(SoulboundRecord record) {
        SoulboundRecord previous = records.get(record.ownerUuid());
        if (previous != null && !previous.bondUuid().equals(record.bondUuid())) {
            throw new IllegalArgumentException("A permanent bond cannot be replaced");
        }
        if (previous != null && !previous.companionUuid().equals(record.companionUuid())
                && previous.companionStatus() != CompanionStatus.READY) {
            throw new IllegalArgumentException("A reserved companion UUID cannot be replaced");
        }
        // Location/mode/cooldown updates retain immutable IDs: uniqueness need not rescan every owner.
        if (previous == null || !previous.companionUuid().equals(record.companionUuid())) {
            for (var other : records.values()) {
                if (!other.ownerUuid().equals(record.ownerUuid()) && (other.bondUuid().equals(record.bondUuid())
                        || other.companionUuid().equals(record.companionUuid()))) {
                    throw new IllegalArgumentException("Bond and companion UUIDs must be globally unique");
                }
            }
        }
        if (!record.equals(records.put(record.ownerUuid(), record))) setDirty();
    }

    /** Used at issuance/reservation boundaries, not per tick. Vanilla logs write errors, so verify the written snapshot. */
    public void flush(MinecraftServer server) {
        setDirty();
        server.overworld().getDataStorage().saveAndJoin();
        verifyDisk(server);
    }

    private void verifyDisk(MinecraftServer server) {
        try {
            var root = NbtIo.readCompressed(file(server), NbtAccounter.unlimitedHeap());
            var decoded = CODEC.parse(NbtOps.INSTANCE, root.get("data")).getOrThrow();
            if (!records.equals(decoded.records)) throw new IllegalStateException("Identity persistence verification failed");
        } catch (IOException | RuntimeException exception) {
            setDirty();
            throw new IllegalStateException("Soulbound identity could not be persisted/validated", exception);
        }
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data/soulbound_identity.dat");
    }
}
