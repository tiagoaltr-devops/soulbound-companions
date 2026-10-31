package br.com.soulbound.companion;

import br.com.soulbound.Config;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Incremental spherical scan: no chunk requests, block changes, coordinates or ore-position particles. */
public final class MineralInstinct {
    private final LupusCompanionEntity companion;
    private @Nullable BlockPos origin;
    private @Nullable BlockPos bestPosition;
    private int radius;
    private int cursor;
    private int bestRank;
    private long nextScan;

    public MineralInstinct(LupusCompanionEntity companion) { this.companion = companion; }

    public void tick(ServerLevel level) {
        var owner = companion.getOwnerPlayer();
        if (owner == null || !owner.isAlive() || owner.isSpectator() || companion.distanceToSqr(owner) > 32 * 32) {
            origin = null;
            return;
        }
        if (origin != null && (radius != Config.MINERAL_RADIUS.get()
                || origin.distSqr(companion.blockPosition()) > radius * radius / 4.0)) origin = null;
        if (origin == null) {
            if (level.getGameTime() < nextScan) return;
            radius = Config.MINERAL_RADIUS.get();
            origin = companion.blockPosition();
            cursor = 0;
            bestRank = 0;
            bestPosition = null;
            nextScan = level.getGameTime() + Config.MINERAL_INTERVAL.get();
        }
        int side = radius * 2 + 1;
        int volume = side * side * side;
        int budget = Config.MINERAL_BLOCKS_PER_TICK.get();
        // Every visited position consumes budget, even outside the sphere or in an unloaded chunk.
        while (budget > 0 && cursor < volume) {
            budget--;
            int index = cursor++;
            int x = index % side - radius;
            int y = index / side % side - radius;
            int z = index / (side * side) - radius;
            if (x * x + y * y + z * z > radius * radius) continue;
            BlockPos pos = origin.offset(x, y, z);
            BlockState state = loadedState(level, pos);
            int rank = state == null ? 0 : rarity(state);
            if (rank > bestRank) {
                bestRank = rank;
                bestPosition = pos;
            }
        }
        // Revalidation uses its own budget slot, possibly on the following tick.
        if (cursor == volume && budget > 0) {
            if (bestPosition != null && bestPosition.distSqr(companion.blockPosition()) <= radius * radius) {
                BlockState state = loadedState(level, bestPosition);
                if (state != null && rarity(state) == bestRank) {
                    companion.showMineralDiscovery();
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, companion.getX(), companion.getY() + 0.6,
                            companion.getZ(), bestRank * 2, 0.25, 0.2, 0.25, 0.0);
                    companion.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 0.7F + bestRank * 0.2F);
                }
            }
            origin = null;
            // Never start overlapping scans or emit twice on the same tick, even with very low budgets.
            nextScan = Math.max(nextScan, level.getGameTime() + 1);
        }
    }

    private static @Nullable BlockState loadedState(ServerLevel level, BlockPos pos) {
        if (pos.getY() < level.getMinY() || pos.getY() >= level.getMaxY()
                || !level.getWorldBorder().isWithinBounds(pos)) return null;
        var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? null : chunk.getBlockState(pos);
    }

    public static int rarity(BlockState state) {
        if (state.is(Blocks.ANCIENT_DEBRIS)) return 4;
        if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) return 3;
        if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) return 2;
        if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE) || state.is(Blocks.NETHER_GOLD_ORE)) return 1;
        return 0;
    }
}
