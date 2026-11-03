package br.com.soulbound.entity.ai;

import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Small loaded-space checks shared by Baby flight goals. This never teleports or bypasses navigation. */
public final class BabyFlightTargets {
    private BabyFlightTargets() {}

    public static boolean canOccupy(LupusCompanionEntity companion, Vec3 position) {
        if (!(companion.level() instanceof ServerLevel level)) return false;
        var box = companion.getBoundingBox().move(position.subtract(companion.position()));
        if (box.minY < level.getMinY() || box.maxY >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            var chunk = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk == null || !chunk.getBlockState(pos).getFluidState().isEmpty()) return false;
        }
        return level.noCollision(companion, box);
    }

    public static @Nullable Vec3 nearOwner(LupusCompanionEntity companion, Player owner) {
        if(companion.isGroundForm())return owner.position();
        double y = owner.getY() + Math.min(1.0, owner.getBbHeight() * 0.6);
        var behind = owner.getLookAngle().multiply(-1.5, 0.0, -1.5);
        Vec3 center = new Vec3(owner.getX() + behind.x, y, owner.getZ() + behind.z);
        for (Vec3 offset : new Vec3[] { Vec3.ZERO, new Vec3(1, 0, 0), new Vec3(-1, 0, 0),
                new Vec3(0, 0, 1), new Vec3(0, 0, -1), new Vec3(0, 1, 0), new Vec3(0, -0.5, 0) }) {
            Vec3 target = center.add(offset);
            if (canOccupy(companion, target)) return target;
        }
        return null;
    }

    public static @Nullable Vec3 awayFrom(LupusCompanionEntity companion, Vec3 threat, double distance, boolean randomize) {
        Vec3 away = companion.position().subtract(threat);
        if (away.lengthSqr() < 0.001) away = new Vec3(1, 0.3, 0);
        away = away.normalize();
        if (randomize) away = away.add((companion.getRandom().nextDouble() - 0.5) * 0.8,
                0.15 + companion.getRandom().nextDouble() * 0.5, (companion.getRandom().nextDouble() - 0.5) * 0.8).normalize();
        Vec3 target = companion.position().add(away.scale(distance));
        return canOccupy(companion, target) ? target : null;
    }
}
