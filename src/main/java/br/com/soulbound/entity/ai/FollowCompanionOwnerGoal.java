package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

public class FollowCompanionOwnerGoal extends Goal {
    private final LupusCompanionEntity companion;
    private @Nullable Player owner;
    private int recalculatePath;

    public FollowCompanionOwnerGoal(LupusCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (companion.level().isClientSide() || companion.getBehaviorMode() != BehaviorMode.FOLLOW_PASSIVE) return false;
        owner = companion.getOwnerPlayer();
        return owner != null && owner.isAlive() && !owner.isSpectator() && needsFollow();
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == companion.level() && companion.isOwnedBy(owner)
                && companion.getBehaviorMode() == BehaviorMode.FOLLOW_PASSIVE && needsFollow();
    }

    private boolean needsFollow() {
        return owner != null && (companion.distanceToSqr(owner) > 6.25 || Math.abs(companion.getY() - (owner.getY() + 1.0)) > 0.65);
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() { recalculatePath = 0; }

    @Override
    public void tick() {
        if (owner == null) return;
        companion.getLookControl().setLookAt(owner, 10.0F, companion.getMaxHeadXRot());
        if (--recalculatePath <= 0) {
            recalculatePath = 10;
            var destination = BabyFlightTargets.nearOwner(companion, owner);
            if (destination != null) companion.getNavigation().moveTo(destination.x, destination.y, destination.z,
                    companion.distanceToSqr(owner) > 64.0 ? 1.5 : 1.2);
            else companion.stopFlight();
        }
    }

    @Override
    public void stop() {
        owner = null;
        companion.stopFlight();
    }
}
