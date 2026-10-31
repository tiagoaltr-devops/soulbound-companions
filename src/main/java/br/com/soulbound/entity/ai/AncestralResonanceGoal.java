package br.com.soulbound.entity.ai;

import java.util.EnumSet;
import java.util.UUID;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.recall.SoulRecallService;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/** Temporary aerial guidance. Does not write the saved mode, Stay anchor, ownership or UUID. */
public final class AncestralResonanceGoal extends Goal {
    private final LupusCompanionEntity companion;
    private final ResonanceFlightState flight=new ResonanceFlightState();
    private UUID identity,bond;
    private BehaviorMode mode;
    private Vec3 destination;
    private Path prepared;
    private int replan;
    private boolean outboundStarted;
    public AncestralResonanceGoal(LupusCompanionEntity c){companion=c;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
    public boolean active(){return flight.active();}
    public boolean begin(Vec3 direction){
        if(!(companion.level() instanceof ServerLevel level) || active())return false;
        for(double distance:new double[]{10,8,12})for(double height:new double[]{.5,1.2,2}){
            var point=companion.position().add(direction.scale(distance)).add(0,height,0);
            if(!BabyFlightTargets.canOccupy(companion,point))continue;
            var path=companion.getNavigation().createPath(BlockPos.containing(point),0);
            if(path==null || !path.canReach())continue;
            destination=point;prepared=path;outboundStarted=false;identity=companion.getUUID();bond=companion.getBondUuid();mode=companion.getBehaviorMode();
            flight.begin(level.getGameTime());companion.setResonating(true);return true;
        }
        return false;
    }
    @Override public boolean canUse(){return active();}
    @Override public boolean canContinueToUse(){return active();}
    @Override public boolean requiresUpdateEveryTick(){return true;}
    @Override public void start(){companion.stopFlight();replan=0;outboundStarted=false;}
    @Override public void tick(){
        var level=(ServerLevel)companion.level();
        if(!(companion.getOwnerPlayer() instanceof ServerPlayer owner) || !owner.isAlive() || owner.isSpectator() || owner.hasDisconnected()
                || !SoulboundIdentity.isCanonical(companion) || !companion.getUUID().equals(identity) || !java.util.Objects.equals(companion.getBondUuid(),bond)
                || companion.getBehaviorMode()!=mode || SoulRecallService.inProgress(level.getServer(),bond)) {cancel();return;}
        long now=level.getGameTime();flight.tick(now,companion.distanceToSqr(destination)<1,companion.distanceToSqr(owner)<9);
        if(!active()){cancel();return;}
        if(flight.phase()==ResonanceFlightState.Phase.OUTBOUND && !outboundStarted){outboundStarted=true;companion.getNavigation().moveTo(prepared,1.15);}
        if(now%4==0)level.sendParticles(ParticleTypes.SOUL,companion.getX(),companion.getY()+.5,companion.getZ(),3,.15,.2,.15,.01);
        if(flight.phase()==ResonanceFlightState.Phase.HOLD){companion.stopFlight();return;}
        if(flight.phase()==ResonanceFlightState.Phase.RETURN && --replan<=0){
            replan=10;var target=BabyFlightTargets.nearOwner(companion,owner);
            if(target==null){cancel();return;}
            var path=companion.getNavigation().createPath(BlockPos.containing(target),0);
            if(path==null || !path.canReach()){cancel();return;}
            companion.getNavigation().moveTo(path,1.15);
        }
        companion.getLookControl().setLookAt(flight.phase()==ResonanceFlightState.Phase.RETURN?owner.getX():destination.x,
                flight.phase()==ResonanceFlightState.Phase.RETURN?owner.getEyeY():destination.y,
                flight.phase()==ResonanceFlightState.Phase.RETURN?owner.getZ():destination.z,30,30);
    }
    public void cancel(){flight.cancel();prepared=null;companion.setResonating(false);companion.stopFlight();}
    @Override public void stop(){cancel();}
}
