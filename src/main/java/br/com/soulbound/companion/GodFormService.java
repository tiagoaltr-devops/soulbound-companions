package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** One canonical transformation, including unloaded entities. Offline server time does not elapse. */
@EventBusSubscriber(modid="soulbound")
public final class GodFormService {
    private static final Map<MinecraftServer,Set<UUID>> ACTIVE=new WeakHashMap<>();
    private GodFormService(){}
    private static Set<UUID> active(MinecraftServer server){return ACTIVE.computeIfAbsent(server,s->new HashSet<>());}
    public static boolean activate(ServerPlayer p){
        var server=p.level().getServer();var data=SoulboundSavedData.get(server);var r=data.find(p.getUUID());long now=server.overworld().getGameTime();
        if(!p.isAlive() || p.isSpectator() || !GodRules.canActivate(r,now))return CompanionFormService.reject(p);
        var c=SoulStaffCommands.loadedCompanion(p,r);
        if(c==null || c.distanceToSqr(p)>1024 || CompanionFormService.busy(c) || !p.level().noCollision(c,net.minecraft.world.entity.EntityDimensions.scalable(1,2.25f).makeBoundingBox(c.position())))return CompanionFormService.reject(p);
        var next=r.withEvolution(r.evolutionStage(),r.evolutionProgress(),now+GodRules.COOLDOWN).withForms(r.forms().activateGod(now,0));
        // Reserve first; an interrupted activation can lose its barrier, never grant a second one.
        data.put(next);data.flush(server);active(server).add(r.ownerUuid());c.applyIdentity(next);
        float shield=c.getMaxHealth()*.3f;var g=next.forms().god();
        next=next.withForms(next.forms().withGod(new GodState(g.end(),shield,shield,g.ultimateEnd(),g.chargeEnd(),g.ruptureEnd())));
        data.put(next);data.flush(server);
        br.com.soulbound.companion.progression.ProgressionService.publish(server,next);c.showAction(20,3);
        p.level().sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.getX(),c.getY()+1,c.getZ(),32,.5,1,.5,.03);return true;
    }
    public static SoulboundRecord normalize(MinecraftServer server,SoulboundRecord r){
        if(!r.activeForm().equals("GOD"))return r;
        if(r.companionStatus()==CompanionStatus.ACTIVE && server.overworld().getGameTime()<r.forms().god().end()){active(server).add(r.ownerUuid());return r;}
        return finish(server,r);
    }
    private static SoulboundRecord finish(MinecraftServer server,SoulboundRecord r){
        var next=r.withForms(r.forms().endGod());var data=SoulboundSavedData.get(server);data.put(next);data.flush(server);active(server).remove(r.ownerUuid());
        br.com.soulbound.companion.progression.ProgressionService.publish(server,next);
        for(var world:server.getAllLevels())if(world.getEntity(next.companionUuid()) instanceof LupusCompanionEntity c){c.plenoCombat().cancelAll();c.showAction(20,3);}
        return next;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event){
        var s=event.getServer();active(s).clear();var data=SoulboundSavedData.get(s);var old=AltarSavedData.get(s);boolean changed=false;
        for(var r:data.snapshot()){
            if(old.progress(r.ownerUuid()).godUnlocked && !r.unlockedForms().contains("GOD")){r=r.withForms(r.forms().unlockGod());data.put(r);changed=true;}
            if(old.progress(r.ownerUuid()).godEnd!=0){old.progress(r.ownerUuid()).godEnd=0;old.setDirty();}
            if(r.activeForm().equals("GOD"))active(s).add(r.ownerUuid());
        }
        if(changed)data.flush(s);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var data=SoulboundSavedData.get(server);
        for(var owner:List.copyOf(active(server))){
            var r=data.find(owner);if(r==null || !r.activeForm().equals("GOD")){active(server).remove(owner);continue;}
            boolean dead=false;for(var world:server.getAllLevels())if(world.getEntity(r.companionUuid()) instanceof LupusCompanionEntity c){dead=!c.isAlive();break;}
            if(dead || r.companionStatus()!=CompanionStatus.ACTIVE || server.overworld().getGameTime()>=r.forms().god().end())finish(server,r);
        }
    }
    /** After invulnerability/cancellation and armor, before vanilla absorption. No second damage invocation. */
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void barrier(LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof LupusCompanionEntity c) || c.level().isClientSide() || !c.isAlive() || !c.isGod() || !SoulboundIdentity.isCanonical(c)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) || !Float.isFinite(event.getNewDamage()) || event.getNewDamage()<=0)return;
        var server=c.level().getServer();var data=SoulboundSavedData.get(server);var r=normalize(server,data.find(c.getOwnerUuid()));
        if(!r.activeForm().equals("GOD"))return;var state=r.forms().god();float absorbed=Math.min(state.shield(),event.getNewDamage());if(absorbed<=0)return;
        var next=r.withForms(r.forms().withGod(state.absorb(absorbed)));data.put(next);data.flush(server);
        event.setNewDamage(Math.max(0,event.getNewDamage()-absorbed));if(c.getOwnerPlayer() instanceof ServerPlayer owner)br.com.soulbound.network.CompanionHudSync.refresh(owner);
    }
}
