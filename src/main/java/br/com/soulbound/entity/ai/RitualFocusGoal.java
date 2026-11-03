package br.com.soulbound.entity.ai;

import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarSavedData;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;

/** Temporary movement override; never replaces the persisted mode or Stay anchor. */
public final class RitualFocusGoal extends Goal {
    private final LupusCompanionEntity c;
    public RitualFocusGoal(LupusCompanionEntity c){this.c=c;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){return c.getOwnerUuid()!=null && (br.com.soulbound.companion.CompanionFormService.ritualBusy(c.level().getServer(),c.getOwnerUuid()) || c.actionLocksMovement());}
    @Override public boolean canContinueToUse(){return canUse();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){c.setTarget(null);c.resonanceGoal().cancel();c.stopFlight();}
    @Override public void tick(){
        var d=AltarSavedData.get(c.level().getServer());
        var ritual=d.state.rituals.values().stream().filter(r->r.player.equals(c.getOwnerUuid().toString())).findFirst().orElse(null);
        c.setRitualizing(ritual!=null);if(ritual==null){c.stopFlight();return;}
        var f=d.state.forges.get(ritual.forge);if(f==null)return;
        double y=f.y+(c.isGroundForm()?1:1.6);c.getLookControl().setLookAt(f.x+.5,y,f.z+.5,20,20);
        if(c.distanceToSqr(f.x+.5,y,f.z+.5)>2)c.getNavigation().moveTo(f.x+.5,y,f.z+.5,1);else c.stopFlight();
    }
    @Override public void stop(){c.setRitualizing(false);c.stopFlight();}
}
