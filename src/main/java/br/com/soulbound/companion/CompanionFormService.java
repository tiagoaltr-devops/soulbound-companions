package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.recall.SoulRecallService;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.*;

public final class CompanionFormService {
    private static final java.util.Map<ServerPlayer,Long> NEXT_FEEDBACK=new java.util.WeakHashMap<>();
    private CompanionFormService() {}
    public static boolean ritualBusy(net.minecraft.server.MinecraftServer server,java.util.UUID owner){return AltarSavedData.get(server).state.rituals.values().stream().anyMatch(r->r.player.equals(owner.toString()));}
    public static boolean busy(LupusCompanionEntity c){return c.isRitualizing() || c.actionLocksMovement() || c.isResonating() || c.getOwnerUuid()==null || SoulRecallService.inProgress(c.level().getServer(),c.getBondUuid()) || ritualBusy(c.level().getServer(),c.getOwnerUuid());}
    public static boolean reject(ServerPlayer p){p.displayClientMessage(Component.translatable("message.soulbound.form_denied"),true);return false;}
    public static boolean select(ServerPlayer p,InteractionHand hand,String form){
        var r=SoulStaffCommands.authorize(p,p.getItemInHand(hand));var server=p.level().getServer();long now=server.overworld().getGameTime();
        if(r==null || !r.forms().canSelect(form,now))return reject(p);var c=SoulStaffCommands.loadedCompanion(p,r);
        if(c==null || c.distanceToSqr(p)>32*32 || busy(c))return reject(p);
        var dimensions=form.equals("PLENO")?net.minecraft.world.entity.EntityDimensions.scalable(1,2.25f):form.equals("JUNIOR")?net.minecraft.world.entity.EntityDimensions.scalable(.65f,1.85f):net.minecraft.world.entity.EntityDimensions.scalable(.45f,.9f);
        if(!p.level().noCollision(c,dimensions.makeBoundingBox(c.position())))return reject(p);
        var next=r.withForms(r.forms().select(form,now));var data=SoulboundSavedData.get(server);data.put(next);data.flush(server);
        c.applyIdentity(next);c.showAction(20,3);SoulboundIdentity.synchronizePlayer(p);
        p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.getX(),c.getY()+1,c.getZ(),25,.4,.8,.4,.03);
        p.level().playSound(null,c.blockPosition(),SoundEvents.BEACON_POWER_SELECT,SoundSource.PLAYERS,.8f,1.2f);return true;
    }
    public static boolean ultimate(ServerPlayer p){
        var server=p.level().getServer();var r=SoulboundSavedData.get(server).find(p.getUUID());
        var c=r==null?null:SoulStaffCommands.loadedCompanion(p,r);long now=server.overworld().getGameTime();
        boolean available=r!=null && p.isAlive() && !p.isSpectator() && r.companionStatus()==CompanionStatus.ACTIVE && !r.resurrectionRequired() && c!=null && c.distanceToSqr(p)<=32*32;
        int state=r==null?4:JuniorCombat.ultimateState(r.activeForm(),BehaviorMode.fromSavedName(r.behaviorMode()),available,c!=null && busy(c),r.forms().activeUltimateEnd(),now);
        if(state!=0){
            if(now>=NEXT_FEEDBACK.getOrDefault(p,Long.MIN_VALUE)){NEXT_FEEDBACK.put(p,now+20);
                p.displayClientMessage(Component.translatable("message.soulbound.ultimate."+state,r==null?0:Math.max(0,(r.forms().activeUltimateEnd()-now+19)/20)),true);}
            return false;
        }
        if(r.activeForm().equals("PLENO") || r.activeForm().equals("GOD"))return c.plenoCombat().beginCataclysm(p);
        var data=SoulboundSavedData.get(p.level().getServer());data.put(r.withForms(r.forms().ultimate(now+c.talents().ultimateCooldown())));data.flush(p.level().getServer());
        for(var target:p.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,c.getBoundingBox().inflate(5),t->JuniorCombat.target(c,t)))
            if(c.distanceToSqr(target)<=25 && c.hasLineOfSight(target))JuniorCombat.hit(c,target,br.com.soulbound.companion.progression.TalentEffects.damage(c,br.com.soulbound.Config.JUNIOR_ULTIMATE_DAMAGE.get().floatValue(),true));
        c.showAction(30,2);for(int i=0;i<40;i++){double a=i*Math.PI/20;p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.getX()+Math.cos(a)*5,c.getY()+.5,c.getZ()+Math.sin(a)*5,1,0,.1,0,.01);}
        p.level().playSound(null,c.blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,.8f,.7f);return true;
    }
}
