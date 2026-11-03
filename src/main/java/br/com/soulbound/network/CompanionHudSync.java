package br.com.soulbound.network;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** O(loaded dimensions) UUID lookups, no chunk loading, entity scans or persisted health copies. */
@EventBusSubscriber(modid="soulbound")
public final class CompanionHudSync {
    private static final Map<ServerPlayer,CompanionHudData> SENT=new WeakHashMap<>();
    private CompanionHudSync() {}
    public static CompanionHudData snapshot(ServerPlayer p){
        var server=p.level().getServer();var r=SoulboundSavedData.get(server).find(p.getUUID());if(r==null)return CompanionHudData.EMPTY;
        if(r.companionStatus()==CompanionStatus.ACTIVE)for(var level:server.getAllLevels()){
            if(level.getEntity(r.companionUuid()) instanceof LupusCompanionEntity c && !c.isRemoved()
                    && r.matches(c.getOwnerUuid(),c.getBondUuid(),c.getUUID()) && c.getEvolutionStage().name().equals(r.activeForm()))
                return CompanionHudData.project(r,c.hasCustomName()?c.getCustomName().getString():"Lupus",true,c.getHealth(),c.getMaxHealth()).god(r,server.overworld().getGameTime());
        }
        return CompanionHudData.project(r,"Lupus",false,0,0).god(r,server.overworld().getGameTime());
    }
    private static void update(ServerPlayer p,boolean force){
        var next=snapshot(p);if(force || !next.equals(SENT.get(p))){SENT.put(p,next);PacketDistributor.sendToPlayer(p,new CompanionHudPayload(next));}
    }
    public static void refresh(ServerPlayer player){update(player,false);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(event.getEntity() instanceof ServerPlayer p && p.tickCount%2==0)update(p,false);}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer p)update(p,true);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer p)update(p,true);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer p)update(p,true);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)SENT.remove(p);}
}
