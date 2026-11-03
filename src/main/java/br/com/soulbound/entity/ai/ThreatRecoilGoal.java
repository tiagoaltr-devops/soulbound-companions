package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import org.jspecify.annotations.Nullable;

/** One short retreat per audible alert, preserving the owner's saved Follow/Stay choice. */
public final class ThreatRecoilGoal extends Goal {
    private final LupusCompanionEntity companion;
    private long lastAlert;
    private int ticks;
    private @Nullable Mob threat;
    private @Nullable Path path;

    public ThreatRecoilGoal(LupusCompanionEntity companion) {
        this.companion = companion;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if(!companion.isBaby())return false;
        long alert = companion.getThreatAlertEnd();
        if (alert == lastAlert || alert <= companion.level().getGameTime()) return false;
        lastAlert = alert;
        threat = companion.getThreatSensor().getAlertThreat();
        if (threat == null) return false;
        var destination = BabyFlightTargets.awayFrom(companion, threat.position(), 1.5, false);
        if (destination == null) return false;
        path = companion.getNavigation().createPath(destination.x, destination.y, destination.z, 0);
        return path != null && path.canReach();
    }

    @Override
    public boolean canContinueToUse() {
        return ticks > 0 && threat != null && threat.isAlive() && !companion.getNavigation().isDone();
    }

    @Override
    public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void start() {
        ticks = 20;
        companion.setEmergencyMoving(true);
        companion.getNavigation().moveTo(path, 1.1);
    }

    @Override
    public void tick() {
        ticks--;
        if (threat != null) companion.getLookControl().setLookAt(threat, 30.0F, 50.0F);
    }

    @Override
    public void stop() {
        companion.stopFlight();
        companion.setEmergencyMoving(false);
        threat = null;
        path = null;
    }
}
