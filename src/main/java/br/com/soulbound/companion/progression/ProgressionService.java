package br.com.soulbound.companion.progression;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

/** Only confirmed damage, physical discoveries and completed trials can issue bond XP. */
@EventBusSubscriber(modid="soulbound")
public final class ProgressionService {
    private static final String CONTRIBUTIONS="SoulboundXPContributions";
    private static final String DEATH_TIME="SoulboundXPDeathTime";
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void dying(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event){
        if(event.getEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level && mob instanceof Enemy)
            mob.getPersistentData().putLong(DEATH_TIME,level.getServer().overworld().getGameTime());
    }
    public static boolean recent(long contribution,long death){return contribution>=0 && death>=contribution && death-contribution<=300;}
    private static final Set<String> RELEVANT=Set.of("ancient_city","mansion","monument","fortress","bastion_remnant","stronghold","end_city","pillager_outpost","desert_pyramid","jungle_pyramid","trail_ruins","trial_chambers");
    private ProgressionService() {}
    public static boolean invest(net.minecraft.server.level.ServerPlayer p,SoulboundRecord session,String id){
        var data=SoulboundSavedData.get(p.level().getServer());var current=data.find(p.getUUID());var talent=TalentDefinitions.find(id);
        if(current==null || !current.matches(session.ownerUuid(),session.bondUuid(),session.companionUuid()) || !p.isAlive() || p.isSpectator() || current.companionStatus()!=CompanionStatus.ACTIVE
                || br.com.soulbound.companion.CompanionFormService.ritualBusy(p.level().getServer(),p.getUUID()) || TalentDefinitions.rejection(current,talent)!=0)return false;
        var next=current.withProgression(current.progression().invest(id,talent.maximum()));data.put(next);data.flush(p.level().getServer());
        for(var level:p.level().getServer().getAllLevels())if(level.getEntity(next.companionUuid()) instanceof LupusCompanionEntity c)c.applyIdentity(next);
        SoulboundIdentity.synchronizePlayer(p);return true;
    }
    public static boolean eligibleSpawn(String reason,boolean noAi){return eligibleSpawn(reason,noAi,false);}
    public static boolean eligibleSpawn(String reason,boolean noAi,boolean development){return !noAi && (Set.of("NATURAL","CHUNK_GENERATION","STRUCTURE","PATROL","REINFORCEMENT","EVENT").contains(reason)
            || development && Set.of("COMMAND","SPAWN_ITEM_USE","").contains(reason));}
    private static boolean eligible(Mob mob){return eligibleSpawn(mob.getSpawnType()==null?"":mob.getSpawnType().name(),mob.isNoAi(),br.com.soulbound.Config.DEVELOPMENT_ARTIFICIAL_COMBAT_XP.get());}
    public static void participation(LupusCompanionEntity c,LivingEntity target){
        if(!(target instanceof Mob mob) || !(mob instanceof Enemy) || mob instanceof br.com.soulbound.entity.SpiritGuardianEntity
                || !eligible(mob))return;
        var tags=mob.getPersistentData();var all=tags.getCompound(CONTRIBUTIONS).orElseGet(CompoundTag::new);
        if(all.size()>=16 && !all.contains(c.getOwnerUuid().toString()))return;
        var entry=new CompoundTag();entry.putString("bond",c.getBondUuid().toString());entry.putLong("time",c.level().getServer().overworld().getGameTime());
        all.put(c.getOwnerUuid().toString(),entry);tags.put(CONTRIBUTIONS,all);
    }
    public static boolean reward(MinecraftServer server,UUID owner,String key,int base,boolean discovery){
        var data=SoulboundSavedData.get(server);var r=data.find(owner);if(r==null || r.progression().rewards().contains(key))return false;
        int bonus=5*r.progression().rank("soulbound:sintonia_da_alma");
        if(discovery && r.activeForm().equals("BABY"))bonus+=5*r.progression().rank("soulbound:vinculo_de_exploracao");
        var next=r.progression().rewardAdjusted(key,base,bonus);if(next.equals(r.progression()))return false;
        data.put(r.withProgression(next));data.flush(server);
        publish(server,r.withProgression(next));
        var p=server.getPlayerList().getPlayer(owner);if(p!=null && next.level()>r.progression().level())p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.soulbound.level_up",next.level(),next.available()),true);
        return true;
    }
    /** Called only after the canonical change has been flushed. No health or XP is taken from the client. */
    public static void publish(MinecraftServer server,SoulboundRecord record){
        for(var level:server.getAllLevels())if(level.getEntity(record.companionUuid()) instanceof LupusCompanionEntity c
                && record.matches(c.getOwnerUuid(),c.getBondUuid(),c.getUUID()))c.applyIdentity(record);
        var player=server.getPlayerList().getPlayer(record.ownerUuid());
        if(player!=null){SoulboundIdentity.synchronizePlayer(player);br.com.soulbound.network.CompanionHudSync.refresh(player);}
    }
    @SubscribeEvent public static void death(EntityLeaveLevelEvent event){
        if(!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob) || !(mob instanceof Enemy)
                || mob.getRemovalReason()!=Entity.RemovalReason.KILLED || mob instanceof LupusCompanionEntity || mob instanceof br.com.soulbound.entity.SpiritGuardianEntity || !eligible(mob))return;
        var entries=mob.getPersistentData().getCompound(CONTRIBUTIONS).orElseGet(CompoundTag::new);long now=level.getServer().overworld().getGameTime();
        int xp=mob instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || mob instanceof net.minecraft.world.entity.boss.wither.WitherBoss?200:mob.getMaxHealth()>=40?25:8;
        for(String key:entries.keySet())try{
            var value=entries.getCompound(key).orElseThrow();var owner=UUID.fromString(key);var r=SoulboundSavedData.get(level.getServer()).find(owner);
            long death=mob.getPersistentData().getLongOr(DEATH_TIME,now);long contribution=value.getLongOr("time",Long.MIN_VALUE);
            if(r!=null && r.bondUuid().toString().equals(value.getStringOr("bond","")) && recent(contribution,death)){
                if(r.progression().level()<30)reward(level.getServer(),owner,"combat:"+mob.getUUID(),xp,false);TalentEffects.kill(level.getServer(),owner);
            }
        }catch(IllegalArgumentException ignored){}
    }
    public static void explore(LupusCompanionEntity c){
        if(!(c.level() instanceof ServerLevel level) || !(c.getOwnerPlayer() instanceof net.minecraft.server.level.ServerPlayer p) || !p.isAlive() || p.isSpectator() || c.distanceToSqr(p)>32*32)return;
        var chunk=level.getChunkSource().getChunkNow(p.blockPosition().getX()>>4,p.blockPosition().getZ()>>4);if(chunk==null)return;
        var registry=level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for(var entry:chunk.getAllReferences().entrySet()){
            var type=registry.getKey(entry.getKey());if(type==null || !type.getNamespace().equals("minecraft") || !(RELEVANT.contains(type.getPath()) || type.getPath().startsWith("village_")))continue;
            for(long packed:entry.getValue()){
                var source=new net.minecraft.world.level.ChunkPos(packed);var loaded=level.getChunkSource().getChunkNow(source.x,source.z);if(loaded==null)continue;
                var start=loaded.getStartForStructure(entry.getKey());
                if(start!=null && start.isValid() && start.getBoundingBox().isInside(p.blockPosition()))reward(level.getServer(),p.getUUID(),"structure:"+level.dimension().identifier()+":"+type+":"+packed,30,true);
            }
        }
        for(var be:chunk.getBlockEntities().values())if(be instanceof br.com.soulbound.ritual.AncestralPedestalBlockEntity pedestal && !pedestal.naturalId().isEmpty()){
            var ruin=AltarSavedData.get(level.getServer()).state.ruins.get(pedestal.naturalId());
            if(ruin!=null && ruin.origin.equals("NATURAL_WORLDGEN") && ruin.integrity.equals("INTACT") && p.distanceToSqr(ruin.x+.5,ruin.y,ruin.z+.5)<=32*32
                    && br.com.soulbound.ritual.RuinIndex.validateLoaded(level,ruin))reward(level.getServer(),p.getUUID(),"ruin:"+ruin.id,30,true);
        }
    }
}
