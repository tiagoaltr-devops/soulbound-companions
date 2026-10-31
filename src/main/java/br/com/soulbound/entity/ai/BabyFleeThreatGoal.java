package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Emergency movement takes priority over Follow and Stay without changing the saved mode. */
public final class BabyFleeThreatGoal extends Goal {
    private final LupusCompanionEntity companion;
    private @Nullable Mob danger;
    private @Nullable Path escapePath;
    private int safeTicks;
    private int pathDelay;

    public BabyFleeThreatGoal(LupusCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        danger = companion.getThreatSensor().nearestDanger(4.0);
        return danger != null;
    }

    @Override
    public boolean canContinueToUse() { return safeTicks < 40; }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        safeTicks = 0;
        pathDelay = 0;
        companion.setEmergencyMoving(true);
        companion.getNavigation().stop();
    }

    @Override
    public void tick() {
        danger = companion.getThreatSensor().nearestDanger(6.0);
        if (danger == null) {
            safeTicks++;
            return;
        }
        safeTicks = 0;
        companion.getLookControl().setLookAt(danger, 30.0F, companion.getMaxHeadXRot());
        if (--pathDelay > 0) return;
        pathDelay = 10;
        escapePath = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 position = BabyFlightTargets.awayFrom(companion, danger.position(), 6.0 + companion.getRandom().nextDouble() * 6.0, true);
            if (position == null || position.distanceToSqr(danger.position()) <= companion.distanceToSqr(danger)) continue;
            var destination = net.minecraft.core.BlockPos.containing(position);
            var level = (net.minecraft.server.level.ServerLevel) companion.level();
            var chunk = level.getChunkSource().getChunkNow(destination.getX() >> 4, destination.getZ() >> 4);
            if (chunk == null || !chunk.getBlockState(destination).getFluidState().isEmpty()) continue;
            Path path = companion.getNavigation().createPath(destination, 0);
            if (path != null && path.canReach()) {
                escapePath = path;
                break;
            }
        }
        if (escapePath != null) companion.getNavigation().moveTo(escapePath, 1.4);
        else companion.stopFlight();
    }

    @Override
    public void stop() {
        danger = null;
        escapePath = null;
        companion.stopFlight();
        companion.setEmergencyMoving(false);
    }
}
