package br.com.soulbound.companion.target;

import java.util.*;
import br.com.soulbound.Config;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.network.TargetStatusPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ephemeral UUID-only focus. Saved behavior is never overwritten, including during shutdown. */
@EventBusSubscriber(modid="soulbound")
public final class TargetCommandService {
    public record Focus(UUID owner,UUID companion,UUID target,String dimension,String form,long end) {}
    private static final Map<MinecraftServer,Map<UUID,Focus>> FOCUS=new WeakHashMap<>();
    private static final Map<ServerPlayer,Long> REQUESTS=new WeakHashMap<>();
    private TargetCommandService(){}
    private static Map<UUID,Focus> focuses(MinecraftServer s){return FOCUS.computeIfAbsent(s,k->new HashMap<>());}
    public static boolean has(TargetCommandActor c){return !c.commandActor().level().isClientSide() && focuses(c.commandActor().level().getServer()).containsKey(c.getBondUuid());}
    public static LivingEntity target(TargetCommandActor c){
        if(c.commandActor().level().isClientSide())return null;
        var f=focuses(c.commandActor().level().getServer()).get(c.getBondUuid());
        return f!=null && ((ServerLevel)c.commandActor().level()).getEntity(f.target()) instanceof LivingEntity t && authorized(c,t)?t:null;
    }
    public static boolean legal(ServerPlayer owner,Mob actor,LivingEntity t){
        if(t==owner || t==actor || !t.isAlive() || t.isRemoved() || t.isInvulnerable() || t.isSpectator() || t.level()!=actor.level()
            || t instanceof TargetCommandActor || t instanceof br.com.soulbound.entity.SpiritGuardianEntity
            || t instanceof TamableAnimal pet && pet.isTame() || owner.isAlliedTo(t) || actor.isAlliedTo(t))return false;
        return !(t instanceof Player p) || owner.level().isPvpAllowed() && owner.canHarmPlayer(p);
    }
    public static boolean authorized(TargetCommandActor c,LivingEntity t){
        if(c.commandActor().level().isClientSide())return false;
        var level=(ServerLevel)c.commandActor().level();var s=level.getServer();var f=focuses(s).get(c.getBondUuid());
        var owner=level.getPlayerByUUID(c.getOwnerUuid());var r=SoulboundSavedData.get(s).find(c.getOwnerUuid());
        return f!=null && f.target().equals(t.getUUID()) && f.companion().equals(c.commandActor().getUUID())
            && f.dimension().equals(level.dimension().identifier().toString()) && s.overworld().getGameTime()<f.end()
            && owner instanceof ServerPlayer p && p.isAlive() && !p.isSpectator() && r!=null && r.companionStatus()==CompanionStatus.ACTIVE
            && r.activeForm().equals(f.form()) && r.matches(p.getUUID(),c.getBondUuid(),c.commandActor().getUUID()) && c.acceptsTargetCommand()
            && legal(p,c.commandActor(),t) && p.distanceToSqr(t)<=Math.pow(Config.TARGET_RANGE.get()*2.0,2)
            && p.distanceToSqr(c.commandActor())<=Math.pow(Config.TARGET_RANGE.get()*2.0,2);
    }
    public static boolean request(ServerPlayer p,int id){
        var s=p.level().getServer();var r=SoulboundSavedData.get(s).find(p.getUUID());
        if(r==null || !p.isAlive() || p.isSpectator())return fail(p,"invalid");
        if(id<0){cancel(p);return true;}
        long now=s.overworld().getGameTime();
        var entity=p.level().getEntity(r.companionUuid());
        if(!(entity instanceof TargetCommandActor c) || !r.matches(p.getUUID(),c.getBondUuid(),entity.getUUID()) || !c.acceptsTargetCommand())return fail(p,"companion");
        var selected=p.level().getEntity(id);
        if(!(selected instanceof LivingEntity t))return fail(p,"invalid");
        var prior=focuses(s).get(r.bondUuid());if(prior!=null && prior.target().equals(t.getUUID())){cancel(p);return true;}
        if(now<REQUESTS.getOrDefault(p,0L))return false;REQUESTS.put(p,now+5);
        if(t instanceof Player && !p.level().isPvpAllowed())return fail(p,"pvp");
        if(!legal(p,c.commandActor(),t))return fail(p,"invalid");
        double range=Config.TARGET_RANGE.get();
        if(p.distanceToSqr(t)>range*range || p.distanceToSqr(entity)>range*range)return fail(p,"range");
        if(!p.hasLineOfSight(t) || t.getBoundingBox().inflate(.35).clip(p.getEyePosition(),p.getEyePosition().add(p.getViewVector(1).scale(range))).isEmpty())return fail(p,"sight");
        c.cancelTargetActions();focuses(s).put(r.bondUuid(),new Focus(p.getUUID(),entity.getUUID(),t.getUUID(),p.level().dimension().identifier().toString(),r.activeForm(),now+Config.TARGET_TIMEOUT.get()));
        c.commandActor().setTarget(t);send(p,t);return true;
    }
    public static void cancel(ServerPlayer p){REQUESTS.remove(p);var r=SoulboundSavedData.get(p.level().getServer()).find(p.getUUID());if(r!=null)clear(p.level().getServer(),r.bondUuid(),p);}
    public static void cancel(TargetCommandActor c){if(!c.commandActor().level().isClientSide())clear(c.commandActor().level().getServer(),c.getBondUuid(),null);}
    private static void clear(MinecraftServer s,UUID bond,ServerPlayer p){
        var f=focuses(s).remove(bond);if(f!=null)for(var level:s.getAllLevels()){
            if(level.getEntity(f.companion()) instanceof TargetCommandActor c){c.cancelTargetActions();c.commandActor().setTarget(null);c.commandActor().getNavigation().stop();}
            if(p==null && level.getPlayerByUUID(f.owner()) instanceof ServerPlayer owner)p=owner;
        }
        if(p!=null)send(p,null);
    }
    private static boolean fail(ServerPlayer p,String reason){p.displayClientMessage(Component.translatable("target.soulbound.error."+reason),true);return false;}
    private static void send(ServerPlayer p,LivingEntity t){PacketDistributor.sendToPlayer(p,new TargetStatusPayload(t==null?-1:t.getId(),t==null?Component.empty():t.getDisplayName(),Config.TARGET_RANGE.get()));}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre e){
        var s=e.getServer();for(var entry:List.copyOf(focuses(s).entrySet())){
            var f=entry.getValue();TargetCommandActor actor=null;for(var level:s.getAllLevels())if(level.getEntity(f.companion()) instanceof TargetCommandActor c){actor=c;break;}
            if(actor==null || target(actor)==null)clear(s,entry.getKey(),null);
        }
    }
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)send(p,null);}
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)cancel(p);}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);REQUESTS.remove(p);}}
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){FOCUS.remove(e.getServer());}
}
