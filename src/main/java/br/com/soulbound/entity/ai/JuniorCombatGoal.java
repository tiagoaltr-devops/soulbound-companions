package br.com.soulbound.entity.ai;

import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.companion.JuniorCombat;
import br.com.soulbound.Config;
import java.util.EnumSet;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

public final class JuniorCombatGoal extends Goal {
    private final LupusCompanionEntity c;private int path;private long nextSearch,nextAttack;
    public JuniorCombatGoal(LupusCompanionEntity c){this.c=c;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        if(!c.canFight() || c.level().getGameTime()<nextSearch)return false;nextSearch=c.level().getGameTime()+10;
        var owner=c.getOwnerPlayer();if(owner==null || !owner.isAlive() || owner.isSpectator())return false;
        var targets=c.level().getEntitiesOfClass(Mob.class,owner.getBoundingBox().inflate(12),m->JuniorCombat.target(c,m) && c.distanceToSqr(m)<24*24 && c.hasLineOfSight(m));
        targets.sort(java.util.Comparator.<Mob>comparingInt(m->m.getTarget()==owner?0:1).thenComparingDouble(m->m.distanceToSqr(owner)));
        if(targets.isEmpty())return false;c.setTarget(targets.getFirst());return true;
    }
    @Override public boolean canContinueToUse(){var t=c.getTarget();var o=c.getOwnerPlayer();return c.canFight() && t!=null && JuniorCombat.target(c,t) && o!=null && o.isAlive() && !o.isSpectator() && o.distanceToSqr(t)<18*18 && c.distanceToSqr(o)<24*24;}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){path=0;}
    @Override public void tick(){var t=c.getTarget();if(t==null)return;if(c.isPleno() || c.isGod()){c.plenoCombat().tick(t);return;}c.getLookControl().setLookAt(t,30,30);
        if(--path<=0){path=10;c.getNavigation().moveTo(t,1.3);}
        if(c.level().getGameTime()>=nextAttack && c.distanceToSqr(t)<4 && c.hasLineOfSight(t)){nextAttack=c.level().getGameTime()+br.com.soulbound.companion.progression.TalentEffects.interval(c,Config.JUNIOR_ATTACK_INTERVAL.get());c.doHurtTarget((net.minecraft.server.level.ServerLevel)c.level(),t);c.showAction(10,1);}
    }
    @Override public void stop(){c.plenoCombat().stopAuto();c.setTarget(null);c.stopFlight();}
}
