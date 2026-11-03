package br.com.soulbound.entity.ai;

import br.com.soulbound.companion.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import java.util.*;

/** Bounded retreat only in Follow/Combat. No food/path means follow remains available. */
public final class DefensiveRetreatGoal extends Goal {
    private final LupusCompanionEntity c;private Path path;private long retry,end;
    public DefensiveRetreatGoal(LupusCompanionEntity c){this.c=c;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    @Override public boolean canUse(){
        long now=c.level().getGameTime();if(c.isBaby() || now<retry || !CompanionSurvival.defensive(c) || c.getBehaviorMode()!=BehaviorMode.FOLLOW_COMBAT || CompanionFormService.busy(c))return false;
        retry=now+40;var threats=c.level().getEntitiesOfClass(Mob.class,c.getBoundingBox().inflate(8),m->JuniorCombat.target(c,m));
        var threat=threats.stream().min(Comparator.comparingDouble(c::distanceToSqr)).orElse(null);if(threat==null)return false;
        var away=c.position().subtract(threat.position());double angle=Math.atan2(away.z,away.x);path=null;
        var origin=c.blockPosition();for(int cx=(origin.getX()-8)>>4;cx<=(origin.getX()+8)>>4;cx++)for(int cz=(origin.getZ()-8)>>4;cz<=(origin.getZ()+8)>>4;cz++)if(!c.level().hasChunkAt(new BlockPos(cx*16,origin.getY(),cz*16)))return false;
        for(double radius:new double[]{6,3})for(double turn:new double[]{0,.7,-.7,1.3,-1.3}){
            var center=BlockPos.containing(c.getX()+Math.cos(angle+turn)*radius,c.getY(),c.getZ()+Math.sin(angle+turn)*radius);
            for(int dy:new int[]{0,1,-1,2,-2}){var pos=center.offset(0,dy,0);if(!c.level().getBlockState(pos.below()).isFaceSturdy(c.level(),pos.below(),Direction.UP) || !c.level().getFluidState(pos).isEmpty())continue;
                if(!c.level().noCollision(c,c.getBoundingBox().move(pos.getCenter().subtract(c.position()).add(0,-.5,0))))continue;
                var candidate=plan(pos);if(candidate!=null && candidate.canReach() && candidate.getNodeCount()<=24){path=candidate;return true;}
            }
        }
        return false;
    }
    private Path plan(BlockPos pos){
        boolean ground=c.onGround();boolean supported=false;
        if(!ground && c.getDeltaMovement().y<=0){var below=BlockPos.containing(c.getX(),c.getY()-.05,c.getZ());var block=c.level().getBlockState(below);var shape=block.getCollisionShape(c.level(),below);supported=!shape.isEmpty() && block.isFaceSturdy(c.level(),below,Direction.UP) && Math.abs(below.getY()+shape.max(Direction.Axis.Y)-c.getY())<.001;}
        // Native GroundPathNavigation refuses planning before its ground flag settles. No movement or teleport occurs here.
        if(supported)c.setOnGround(true);try{return c.getNavigation().createPath(pos,0);}finally{if(supported)c.setOnGround(ground);}
    }
    @Override public void start(){c.plenoCombat().cancelAll();c.setTarget(null);c.getNavigation().moveTo(path,1.4);end=c.level().getGameTime()+40;}
    @Override public boolean canContinueToUse(){return c.level().getGameTime()<end && CompanionSurvival.defensive(c) && c.getBehaviorMode()==BehaviorMode.FOLLOW_COMBAT && !CompanionFormService.busy(c) && !c.getNavigation().isDone();}
    @Override public void stop(){c.getNavigation().stop();}
}
