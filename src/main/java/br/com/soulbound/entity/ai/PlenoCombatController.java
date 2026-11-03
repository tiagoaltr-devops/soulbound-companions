package br.com.soulbound.entity.ai;

import br.com.soulbound.companion.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Server-only bounded ground combat. No teleport, terrain edits, explosions or damage on client animation. */
public final class PlenoCombatController {
    private final LupusCompanionEntity c;private UUID targetId,charging;
    private int combo;private long attackAt,pathAt,scanAt,chargeUntil,catAt;
    private UUID catOwner,catBond;private String catForm;
    public PlenoCombatController(LupusCompanionEntity c){this.c=c;}
    public void stopAuto(){combo=0;targetId=null;charging=null;}
    public void cancelAll(){stopAuto();catAt=0;catOwner=null;catBond=null;if(!c.level().isClientSide() && c.actionType()>=10)c.showAction(0,1);}
    public boolean preparing(){return catAt!=0;}
    private SoulboundRecord record(){return SoulboundSavedData.get(c.level().getServer()).find(c.getOwnerUuid());}
    private void reserve(boolean charge,long now){var data=SoulboundSavedData.get(c.level().getServer());var r=record();data.put(r.withForms(c.isGod()?r.forms().withGod(r.forms().god().secondary(charge,now+(charge?GodRules.CHARGE:GodRules.RUPTURE))):r.forms().secondary(charge,now+(charge?PlenoRules.CHARGE:PlenoRules.QUAKE))));data.flush(c.level().getServer());}
    private float damage(int action){var p=c.talents();if(c.isGod()){int rank=switch(action){case 12->p.primordial();case 13->p.celestial();case 14->p.divine();case 16->p.apocalypse();default->0;};return (float)(c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*p.damage()*GodRules.factor(action,rank));}int rank=switch(action){case 12->p.dominance();case 13->p.charge();case 14->p.quake();case 16->p.cataclysm();default->0;};
        return (float)(c.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)*p.damage()*PlenoRules.factor(action,rank));}
    private boolean hit(LivingEntity target,int action){return c.hasLineOfSight(target) && JuniorCombat.hit(c,target,damage(action));}
    private void pulse(int count,double radius){var level=(ServerLevel)c.level();for(int i=0;i<count;i++){double a=i*Math.PI*2/count;level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.getX()+Math.cos(a)*radius,c.getY()+.3,c.getZ()+Math.sin(a)*radius,1,0,.1,0,.01);}}
    private List<LivingEntity> nearby(double radius){return c.level().getEntitiesOfClass(LivingEntity.class,c.getBoundingBox().inflate(radius),t->JuniorCombat.target(c,t) && c.distanceToSqr(t)<=radius*radius && c.hasLineOfSight(t));}
    public void tick(LivingEntity target){
        if(!(c.isPleno() || c.isGod()) || !c.canFight() || !JuniorCombat.target(c,target)){stopAuto();return;}
        long now=c.level().getServer().overworld().getGameTime();
        if(c.isGod() && now>=record().forms().god().end()){cancelAll();return;}
        if(!target.getUUID().equals(targetId)){combo=0;charging=null;targetId=target.getUUID();}
        c.getLookControl().setLookAt(target,30,30);double distance=c.distanceTo(target);
        if(charging!=null){
            if(now>chargeUntil || !charging.equals(target.getUUID())){charging=null;combo=0;c.stopFlight();return;}
            if(distance<=(c.isGod()?GodRules.REACH:PlenoRules.REACH) && c.hasLineOfSight(target)){
                if(hit(target,13)){c.showAction(14,13);pulse(12,1);}
                charging=null;c.stopFlight();attackAt=now+br.com.soulbound.companion.progression.TalentEffects.interval(c,br.com.soulbound.Config.JUNIOR_ATTACK_INTERVAL.get());return;
            }
            if(c.getNavigation().isDone()){charging=null;c.stopFlight();return;}
            if(now%5==0)((ServerLevel)c.level()).sendParticles(ParticleTypes.SOUL,c.getX(),c.getY()+.7,c.getZ(),2,.15,.3,.15,.01);return;
        }
        var r=record();
        if(now>=(c.isGod()?r.forms().god().chargeEnd():r.forms().chargeEnd()) && now>=pathAt && PlenoRules.chargeDistance(distance) && safeCorridor(target)){
            pathAt=now+10;var path=c.getNavigation().createPath(target,0);
            if(path!=null && path.canReach() && path.getNodeCount()<=24){reserve(true,now);charging=target.getUUID();chargeUntil=now+40;c.showAction(40,13);c.getNavigation().moveTo(path,2.0);combo=0;return;}
        }
        if(now>=(c.isGod()?r.forms().god().ruptureEnd():r.forms().quakeEnd()) && now>=scanAt && now>=attackAt){
            scanAt=now+10;var targets=nearby(c.isGod()?5:4);
            if(targets.size()>=2){reserve(false,now);boolean landed=false;for(var t:targets)landed|=hit(t,14);
                if(landed){c.showAction(18,14);pulse(24,c.isGod()?5:4);c.playSound(SoundEvents.IRON_GOLEM_ATTACK,.8f,.6f);}combo=0;attackAt=now+br.com.soulbound.companion.progression.TalentEffects.interval(c,br.com.soulbound.Config.JUNIOR_ATTACK_INTERVAL.get());return;}
        }
        if(now>=pathAt){pathAt=now+10;c.getNavigation().moveTo(target,1.3);}
        if(distance>(c.isGod()?GodRules.REACH:PlenoRules.REACH) || !c.hasLineOfSight(target)){combo=0;return;}
        if(now<attackAt)return;attackAt=now+br.com.soulbound.companion.progression.TalentEffects.interval(c,br.com.soulbound.Config.JUNIOR_ATTACK_INTERVAL.get());int action=10+combo;
        if(hit(target,action)){
            c.showAction(14,action);if(combo==2){target.knockback(c.isGod()?.5:.3,c.getX()-target.getX(),c.getZ()-target.getZ());if(c.isGod()){pulse(16,2.5);for(var other:nearby(2.5))if(other!=target)hit(other,10);}}combo=(combo+1)%3;
        }else combo=0;
    }
    private boolean safeCorridor(LivingEntity target){
        if(Math.abs(target.getY()-c.getY())>1 || !c.hasLineOfSight(target))return false;
        var level=(ServerLevel)c.level();var from=c.position();var delta=target.position().subtract(from);double distance=delta.length();if(distance<4)return false;
        var end=from.add(delta.normalize().scale(distance-1.5));int steps=(int)Math.ceil(distance*2);
        for(int i=1;i<=steps;i++){
            Vec3 point=from.lerp(end,(double)i/steps);var box=c.getBoundingBox().move(point.subtract(from));
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY-1,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))if(!level.hasChunkAt(pos))return false;
            var floor=BlockPos.containing(point).below();if(!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP) || !level.getFluidState(BlockPos.containing(point)).isEmpty() || !level.noCollision(c,box))return false;
        }
        return true;
    }
    public boolean beginCataclysm(ServerPlayer owner){
        if(preparing() || !(c.isPleno() || c.isGod()) || !c.canFight() || !c.isOwnedBy(owner))return false;
        long now=c.level().getServer().overworld().getGameTime();var r=record();if(now<r.forms().activeUltimateEnd())return false;
        var data=SoulboundSavedData.get(c.level().getServer());data.put(r.withForms(c.isGod()?r.forms().withGod(r.forms().god().ultimate(now+GodRules.ULTIMATE)):r.forms().plenoUltimate(now+PlenoRules.ULTIMATE)));data.flush(c.level().getServer());
        stopAuto();c.stopFlight();catOwner=owner.getUUID();catBond=r.bondUuid();catForm=r.activeForm();int preparation=c.isGod()?GodRules.PREPARATION:PlenoRules.PREPARATION;catAt=now+preparation;c.showAction(preparation,15);
        c.playSound(SoundEvents.BEACON_ACTIVATE,.8f,.65f);return true;
    }
    public void tickUltimate(){
        if(!preparing())return;var owner=c.getOwnerPlayer();var r=record();long now=c.level().getServer().overworld().getGameTime();
        if(!c.isAlive() || !(c.isPleno() || c.isGod()) || !(owner instanceof ServerPlayer p) || !p.isAlive() || p.isSpectator() || p.hasDisconnected() || c.distanceToSqr(p)>32*32
                || r==null || !r.ownerUuid().equals(catOwner) || !r.bondUuid().equals(catBond) || !r.activeForm().equals(catForm) || c.isGod() && now>=r.forms().god().end() || !SoulboundIdentity.isCanonical(c)
                || c.getBehaviorMode()!=BehaviorMode.FOLLOW_COMBAT || !r.behaviorMode().equals("FOLLOW_COMBAT") || c.actionType()!=15
                || CompanionFormService.ritualBusy(c.level().getServer(),catOwner) || br.com.soulbound.companion.recall.SoulRecallService.inProgress(c.level().getServer(),catBond) || c.isResonating()){
            catAt=0;catOwner=null;catBond=null;return;
        }
        c.stopFlight();if(now<catAt){if(now%5==0){((ServerLevel)c.level()).sendParticles(ParticleTypes.SOUL,c.getX(),c.getY()+1.2,c.getZ(),4,.4,.7,.4,.02);if(c.isGod())((ServerLevel)c.level()).sendParticles(ParticleTypes.ENCHANT,c.getX(),c.getY()+1.5,c.getZ(),6,.8,.8,.8,.04);}return;}
        catAt=0;catOwner=null;catBond=null;c.showAction(0,1);for(var target:nearby(c.isGod()?8:6))hit(target,16);
        pulse(c.isGod()?48:36,c.isGod()?8:6);
        if(c.isGod()){var level=(ServerLevel)c.level();for(int i=0;i<24;i++){double angle=i*Math.PI/12;level.sendParticles(ParticleTypes.END_ROD,c.getX()+Math.cos(angle)*4,c.getY()+1.5,c.getZ()+Math.sin(angle)*4,1,0,.3,0,.03);}c.playSound(SoundEvents.BEACON_POWER_SELECT,1f,.6f);}
        c.showAction(16,16);c.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE,1f,.5f);
    }
}
