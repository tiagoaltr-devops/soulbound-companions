package br.com.soulbound.companion.recall;

import java.util.function.Predicate;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class SoulRecallDestinations {
    private SoulRecallDestinations() {}

    /** Bounded search; injectable safety predicate also exercises destination selection without a live world. */
    public static @Nullable Vec3 selectNear(Vec3 player, Predicate<Vec3> safe) {
        for (double height : new double[] { 0.7, 1.3, 0.1, 2.0 }) {
            for (int radius = 2; radius <= 4; radius++) {
                for (int direction = 0; direction < 8; direction++) {
                    double angle = direction * Math.PI / 4;
                    Vec3 position = player.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
                    if (safe.test(position)) return position;
                }
            }
        }
        return null;
    }

    public static @Nullable Vec3 find(ServerPlayer player, LupusCompanionEntity companion) {
        return selectNear(player.position(), position -> safeAt(player.level(), companion, position));
    }

    public static boolean safeAt(ServerLevel level, LupusCompanionEntity companion, Vec3 position) {
        var box = companion.getBoundingBox().move(position.subtract(companion.position())).inflate(0.15);
        if (box.minY <= level.getMinY() || box.maxY >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (var pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.3, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null || dangerous(chunk.getBlockState(pos))) return false;
        }
        return level.noCollision(companion, box);
    }

    private static boolean dangerous(BlockState state) {
        return !state.getFluidState().isEmpty() || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY);
    }
}
