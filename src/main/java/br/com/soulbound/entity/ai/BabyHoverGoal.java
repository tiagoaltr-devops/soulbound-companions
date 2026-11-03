package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

/** Physical position is held by the server; the small sinusoidal bob is render-only. */
public final class BabyHoverGoal extends Goal {
    private final LupusCompanionEntity companion;
    private Vec3 center = Vec3.ZERO;

    public BabyHoverGoal(LupusCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() { return !companion.isGroundForm() && !companion.isPassenger(); }
    @Override public boolean canContinueToUse(){return canUse();}

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        var below = companion.blockPosition().below();
        boolean supported = companion.onGround() || !companion.level().getBlockState(below)
                .getCollisionShape(companion.level(), below).isEmpty();
        center = companion.position().add(0.0, supported ? 0.6 : 0.0, 0.0);
    }

    @Override
    public void tick() {
        Vec3 stay = companion.getHoverStayPosition();
        Vec3 target = companion.getBehaviorMode() == BehaviorMode.STAY && stay != null ? stay : center;
        // Only correct a small local drift directly. Long returns go through ReturnToStayGoal's pathfinder.
        double distance = companion.position().distanceToSqr(target);
        if (distance > 0.0225 && distance < 1.0 && BabyFlightTargets.canOccupy(companion, target)) {
            companion.getMoveControl().setWantedPosition(target.x, target.y, target.z, 0.6);
        } else companion.stopFlight();
    }

    @Override
    public void stop() { companion.stopFlight(); }
}
