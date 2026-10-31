package br.com.soulbound.companion.identity;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Grouped internally to keep RecordCodecBuilder within its arity limit; fields remain flat on disk. */
public record SoulRecallState(Optional<ResourceKey<Level>> lastKnownDimension, Optional<Vec3> lastKnownPosition,
        long lastKnownUpdate, long recallCooldownEnd) {
    public static final SoulRecallState EMPTY = new SoulRecallState(Optional.empty(), Optional.empty(), 0L, 0L);
    private static final Codec<Vec3> POSITION_CODEC = Vec3.CODEC.validate(position -> validPosition(position)
            ? DataResult.success(position) : DataResult.error(() -> "Invalid companion recall position"));
    public static final MapCodec<SoulRecallState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("lastKnownDimension").forGetter(SoulRecallState::lastKnownDimension),
            POSITION_CODEC.optionalFieldOf("lastKnownPosition").forGetter(SoulRecallState::lastKnownPosition),
            Codec.LONG.optionalFieldOf("lastKnownUpdate", 0L).forGetter(SoulRecallState::lastKnownUpdate),
            Codec.LONG.optionalFieldOf("recallCooldownEnd", 0L).forGetter(SoulRecallState::recallCooldownEnd)
    ).apply(instance, SoulRecallState::new));

    public static boolean validPosition(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)
                && Math.abs(position.x) < 30_000_000 && Math.abs(position.z) < 30_000_000
                && Math.abs(position.y) < 20_000_000;
    }

    public SoulRecallState at(ResourceKey<Level> dimension, Vec3 position, long time) {
        if (!validPosition(position)) return this;
        return new SoulRecallState(Optional.of(dimension), Optional.of(position), time, recallCooldownEnd);
    }

    public SoulRecallState withCooldown(long end) {
        return new SoulRecallState(lastKnownDimension, lastKnownPosition, lastKnownUpdate, end);
    }

    public int remainingSeconds(long now) {
        if (recallCooldownEnd <= now) return 0;
        return (int) Math.min(Integer.MAX_VALUE, (recallCooldownEnd - now - 1) / 20 + 1);
    }
}
