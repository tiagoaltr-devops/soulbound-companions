package br.com.soulbound.companion;

import java.util.List;
import br.com.soulbound.Config;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.WolfSoundVariants;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** Observes mobs without changing their targets or AI. All calls run on the server. */
public final class ThreatSensor {
    private final LupusCompanionEntity companion;
    private List<Mob> threats = List.of();
    private @Nullable Mob alertThreat;
    private int scanDelay;
    private int lookTicks;
    private long nextAlert;

    public ThreatSensor(LupusCompanionEntity companion) { this.companion = companion; }

    public void tick(ServerLevel level) {
        Player owner = companion.getOwnerPlayer();
        if (--scanDelay <= 0) {
            scanDelay = 10;
            int radius = Config.THREAT_RADIUS.get();
            threats = level.getEntitiesOfClass(Mob.class, companion.getBoundingBox().inflate(radius),
                    mob -> mob != companion && mob.isAlive() && companion.distanceToSqr(mob) <= radius * radius
                            && (mob instanceof Enemy || (owner != null && mob.getTarget() == owner)));
            alertThreat = null;
            for (Mob threat : threats) {
                if (alertThreat == null || isPreferred(threat, alertThreat, owner)) alertThreat = threat;
            }
            if (owner != null && owner.isAlive() && !owner.isSpectator() && companion.distanceToSqr(owner) <= 32 * 32
                    && alertThreat != null && level.getGameTime() >= nextAlert) {
                companion.playSound(SoundEvents.WOLF_SOUNDS.get(WolfSoundVariants.SoundSet.CLASSIC).growlSound().value(), 0.8F, 1.4F);
                lookTicks = 40;
                companion.showThreatAlert();
                nextAlert = level.getGameTime() + Config.ALERT_COOLDOWN.get();
            }
        }
        // customServerAiStep runs after goals, so this short alert look overrides idle/follow looking.
        if (lookTicks > 0) {
            lookTicks--;
            if (alertThreat != null && alertThreat.isAlive() && !companion.isEmergencyMoving()) {
                companion.getLookControl().setLookAt(alertThreat, 30.0F, companion.getMaxHeadXRot());
            }
        }
    }

    private boolean isPreferred(Mob candidate, Mob current, @Nullable Player owner) {
        boolean candidateTargetsOwner = owner != null && candidate.getTarget() == owner;
        boolean currentTargetsOwner = owner != null && current.getTarget() == owner;
        return candidateTargetsOwner != currentTargetsOwner ? candidateTargetsOwner
                : companion.distanceToSqr(candidate) < companion.distanceToSqr(current);
    }

    public @Nullable Mob getAlertThreat() {
        return alertThreat != null && alertThreat.isAlive() && alertThreat.level() == companion.level() ? alertThreat : null;
    }

    public void resetAfterRecall() {
        threats = List.of();
        alertThreat = null;
        lookTicks = 0;
        scanDelay = 0;
    }

    public @Nullable Mob nearestDanger(double radius) {
        Mob nearest = null;
        double distance = radius * radius;
        for (Mob threat : threats) {
            double current = companion.distanceToSqr(threat);
            if (threat.isAlive() && !threat.isRemoved() && threat.level() == companion.level() && current < distance) {
                nearest = threat;
                distance = current;
            }
        }
        return nearest;
    }
}
