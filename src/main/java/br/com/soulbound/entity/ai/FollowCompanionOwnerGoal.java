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
        return owner != null && owner.isAlive() && !owner.isSpectator() && companion.distanceToSqr(owner) > 9.0;
    }

    @Override
    public boolean canContinueToUse() {
        return owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == companion.level() && companion.isOwnedBy(owner)
                && companion.getBehaviorMode() == BehaviorMode.FOLLOW_PASSIVE && companion.distanceToSqr(owner) > 4.0;
    }

    @Override
    public void start() { recalculatePath = 0; }

    @Override
    public void tick() {
        if (owner == null) return;
        companion.getLookControl().setLookAt(owner, 10.0F, companion.getMaxHeadXRot());
        if (--recalculatePath <= 0) {
            recalculatePath = adjustedTickDelay(10);
            companion.getNavigation().moveTo(owner, 1.0);
        }
    }

    @Override
    public void stop() {
        owner = null;
        companion.getNavigation().stop();
    }
}
