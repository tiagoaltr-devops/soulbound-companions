package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Returns to the persisted Stay anchor after emergency movement, with bounded retry attempts. */
public final class ReturnToStayGoal extends Goal {
    private final LupusCompanionEntity companion;
    private @Nullable Path path;
    private int remainingTicks;
    private long retryAfter;

    public ReturnToStayGoal(LupusCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        Vec3 anchor = companion.getHoverStayPosition();
        if (companion.getBehaviorMode() != BehaviorMode.STAY || anchor == null
                || companion.position().distanceToSqr(anchor) <= 1.0
                || companion.level().getGameTime() < retryAfter
                || companion.getThreatSensor().nearestDanger(6.0) != null) return false;
        retryAfter = companion.level().getGameTime() + 40;
        path = companion.getNavigation().createPath(anchor.x, anchor.y, anchor.z, 0);
        return path != null && path.canReach();
    }

    @Override
    public boolean canContinueToUse() {
        Vec3 anchor = companion.getHoverStayPosition();
        return remainingTicks > 0 && companion.getBehaviorMode() == BehaviorMode.STAY
                && anchor != null && companion.position().distanceToSqr(anchor) > 1.0
                && !companion.getNavigation().isDone() && companion.getThreatSensor().nearestDanger(6.0) == null;
    }

    @Override
    public void start() {
        remainingTicks = 200;
        companion.setEmergencyMoving(true);
        companion.getNavigation().moveTo(path, 1.0);
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() { remainingTicks--; }

    @Override
    public void stop() {
        companion.stopFlight();
        companion.setEmergencyMoving(false);
        retryAfter = companion.level().getGameTime() + 40;
    }
}
