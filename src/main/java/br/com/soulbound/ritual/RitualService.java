package br.com.soulbound.ritual;

import java.util.*;
import br.com.soulbound.Config;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid="soulbound")
public final class RitualService {
    private RitualService() {}
    public static String resurrectionProblem(ServerPlayer p,AltarSavedData.Forge f){
        var level=p.level();var server=level.getServer();var d=AltarSavedData.get(server);
        if(!p.isAlive() || p.isSpectator())return "permission";
        if(!f.placed || !f.state.equals("ACTIVE") || busy(level,f.id) || br.com.soulbound.companion.CompanionFormService.ritualBusy(server,p.getUUID()))return "busy";
        int rotation=ForgeService.rotation(level,new BlockPos(f.x,f.y,f.z));if(rotation<0)return "structure";
        var record=SoulboundSavedData.get(server).find(p.getUUID());if(record==null || !record.canResurrect())return "dead";
        for(var world:server.getAllLevels())if(world.getEntity(record.companionUuid())!=null)return "entity";
        if(!AltarInventory.decode(level,f.core).is(ModItems.ALTAR_ITEMS.get("restoration_core").get()))return "core";
        if(!platform(p,f,rotation))return "platform";
        if(p.experienceLevel<cost("restoration")+(p.getStringUUID().equals(f.owner)?0:Config.RITUAL_VISITOR_XP.get()))return "levels";
        var keys=ForgeService.offeringKeys(level,f,rotation);var costs=RitualDefinitions.offerings(level,"restoration");
        for(int i=0;i<3;i++)if(!costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return "offering_"+i;
        return "ready";
    }
    public static String awakeningProblem(ServerPlayer p,AltarSavedData.Forge f){
        var level=p.level();var d=AltarSavedData.get(level.getServer());
        if(!f.state.equals("ACTIVE") || busy(level,f.id) || br.com.soulbound.companion.CompanionFormService.ritualBusy(level.getServer(),p.getUUID()))return "busy";
        int rotation=ForgeService.rotation(level,new BlockPos(f.x,f.y,f.z));if(rotation<0)return "structure";
        var r=SoulboundSavedData.get(level.getServer()).find(p.getUUID());
        if(!CompanionEvolution.accepts(r,"awakening",d) || !r.activeForm().equals("BABY"))return "baby";
        var c=br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,r);
        if(c==null || c.distanceToSqr(f.x+.5,f.y+1,f.z+.5)>32*32 || br.com.soulbound.companion.CompanionFormService.busy(c))return "companion";
        if(!AltarInventory.decode(level,f.core).is(ModItems.ALTAR_ITEMS.get("awakening_core").get()))return "core";
        if(!platform(p,f,rotation))return "platform";
        if(p.experienceLevel<cost("awakening")+(p.getStringUUID().equals(f.owner)?0:Config.RITUAL_VISITOR_XP.get()))return "levels";
        var keys=ForgeService.offeringKeys(level,f,rotation);var costs=RitualDefinitions.offerings(level,"awakening");
        for(int i=0;i<3;i++)if(InventoryTransfers.pending(d,keys.get(i)) || !costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return "offerings";
        return "ready";
    }
    public static String ascensionProblem(ServerPlayer p,AltarSavedData.Forge f){return evolutionProblem(p,f,false);}
    public static String divineProblem(ServerPlayer p,AltarSavedData.Forge f){return evolutionProblem(p,f,true);}
    private static String evolutionProblem(ServerPlayer p,AltarSavedData.Forge f,boolean god){
        var level=p.level();var d=AltarSavedData.get(level.getServer());
        if(!p.isAlive() || p.isSpectator())return "companion";
        if(!f.placed || !f.state.equals("ACTIVE") || busy(level,f.id) || br.com.soulbound.companion.CompanionFormService.ritualBusy(level.getServer(),p.getUUID()))return "busy";
        int rotation=ForgeService.rotation(level,new BlockPos(f.x,f.y,f.z));if(rotation<0)return "structure";
        var r=SoulboundSavedData.get(level.getServer()).find(p.getUUID());
        if(!(god?br.com.soulbound.companion.GodRules.canUnlock(r):br.com.soulbound.companion.PlenoRules.canUnlock(r)))return "level";String kind=god?"divine":"ascension";
        var c=br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,r);
        if(c==null || c.distanceToSqr(f.x+.5,f.y+1,f.z+.5)>1024 || br.com.soulbound.companion.CompanionFormService.busy(c))return "companion";
        if(!AltarInventory.decode(level,f.core).is(ModItems.ALTAR_ITEMS.get(kind+"_core").get()))return "core";
        if(!platform(p,f,rotation))return "platform";
        if(p.experienceLevel<cost(kind)+(p.getStringUUID().equals(f.owner)?0:Config.RITUAL_VISITOR_XP.get()))return "levels";
        var keys=ForgeService.offeringKeys(level,f,rotation);var costs=RitualDefinitions.offerings(level,kind);
        for(int i=0;i<3;i++)if(InventoryTransfers.pending(d,keys.get(i)) || !costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return "offerings";
        return "ready";
    }
    public static boolean busy(ServerLevel level,String forge){var d=AltarSavedData.get(level.getServer());var f=d.state.forges.get(forge);return f!=null && f.activation!=null || d.state.rituals.values().stream().anyMatch(r->r.forge.equals(forge));}
    public static boolean locked(ServerLevel level,BlockPos pos){String key=AltarInventory.key(level,pos);var d=AltarSavedData.get(level.getServer());return d.state.rituals.values().stream().anyMatch(r->r.offerings.containsKey(key)) || d.state.forges.values().stream().anyMatch(f->f.activation!=null && f.activation.offerings.containsKey(key));}
    static boolean platform(ServerPlayer p,AltarSavedData.Forge f,int rotation){
        var offset=AltarPolicy.rotate(0,2,rotation);
        return p.level().dimension().identifier().toString().equals(f.dimension) && Math.abs(p.getX()-(f.x+offset[0]+.5))<.65
                && Math.abs(p.getZ()-(f.z+offset[1]+.5))<.65 && p.getY()>=f.y+.9 && p.getY()<=f.y+1.5;
    }
    public static boolean start(ServerPlayer p,AltarSavedData.Forge f){
        var level=p.level();var s=level.getServer();var d=AltarSavedData.get(s);
        if(!p.isAlive() || p.isSpectator() || !f.state.equals("ACTIVE") || busy(level,f.id))return false;
        int rotation=ForgeService.rotation(level,new BlockPos(f.x,f.y,f.z));if(rotation<0 || !platform(p,f,rotation))return false;
        var core=AltarInventory.decode(level,f.core);String kind=null;
        for(String k:new String[]{"awakening","ascension","divine","restoration"})if(core.is(ModItems.ALTAR_ITEMS.get(k+"_core").get()))kind=k;
        if(kind==null)return false;
        if(kind.equals("divine") && !divineProblem(p,f).equals("ready"))return false;
        if(kind.equals("ascension") && !ascensionProblem(p,f).equals("ready"))return false;
        if(kind.equals("restoration") && !resurrectionProblem(p,f).equals("ready"))return false;
        if(kind.equals("awakening")){String problem=awakeningProblem(p,f);if(!problem.equals("ready")){p.displayClientMessage(Component.translatable("message.soulbound.junior_ritual."+problem),false);return false;}}
        var record=SoulboundSavedData.get(s).find(p.getUUID());
        if(!CompanionEvolution.accepts(record,kind,d))return false;
        if(br.com.soulbound.companion.CompanionFormService.ritualBusy(s,p.getUUID()))return false;
        if(kind.equals("awakening")){
            var c=br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,record);
            if(c==null || !record.activeForm().equals("BABY") || c.distanceToSqr(f.x+.5,f.y+1,f.z+.5)>32*32
                    || br.com.soulbound.companion.CompanionFormService.busy(c))return false;
        }
        int xp=cost(kind)+(p.getStringUUID().equals(f.owner)?0:Config.RITUAL_VISITOR_XP.get());if(p.experienceLevel<xp)return false;
        var costs=RitualDefinitions.offerings(level,kind);var keys=ForgeService.offeringKeys(level,f,rotation);
        if(InventoryTransfers.pending(d,"forge/"+f.id) || keys.stream().anyMatch(key->InventoryTransfers.pending(d,key) || locked(level,BlockPos.of(Long.parseLong(key.substring(key.lastIndexOf(':')+1))))))return false;
        for(int i=0;i<3;i++)if(!costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return false;
        var r=new AltarSavedData.Ritual();r.id=UUID.randomUUID().toString();r.player=p.getStringUUID();r.forge=f.id;r.kind=kind;
        r.bond=record.bondUuid().toString();r.rotation=rotation;r.end=s.overworld().getGameTime()+(kind.equals("awakening")?400:kind.equals("ascension")?600:kind.equals("divine")?br.com.soulbound.companion.GodRules.RITUAL:Config.RITUAL_TICKS.get());r.core=f.core;
        // XP is persisted before the one-file escrow commit. A crash in this narrow gap is an interrupted paid attempt, with materials still in place.
        p.giveExperienceLevels(-xp);InventoryTransfers.receipt(p,"ritual:"+r.id);InventoryTransfers.saveReceipt(p,"ritual:"+r.id);
        f.core="";
        for(int i=0;i<3;i++){
            var stack=AltarInventory.decode(level,d.state.slots.get(keys.get(i)));
            r.offerings.put(keys.get(i),AltarInventory.encode(level,stack.split(costs.get(i).count())));
            d.state.slots.put(keys.get(i),AltarInventory.encode(level,stack));
        }
        d.state.rituals.put(r.id,r);d.flush(s);r.offerings.keySet().forEach(key->OfferingBlockEntity.refresh(level,key));
        level.playSound(null,new BlockPos(f.x,f.y,f.z),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.BLOCKS,.7f,1.1f);return true;
    }
    public static int cost(String kind){return switch(kind){case "awakening"->Config.AWAKENING_XP.get();case "ascension"->Config.ASCENSION_XP.get();case "divine"->Config.DIVINE_XP.get();case "restoration"->Config.RESTORATION_XP.get();default->throw new IllegalArgumentException(kind);};}
    public static void refund(AltarSavedData d,AltarSavedData.Ritual r,net.minecraft.server.MinecraftServer server){
        var f=d.state.forges.get(r.forge);
        if(f==null)throw new IllegalStateException("Escrow has no forge");
        // These slots are locked for the duration; refund into the canonical storage, even with unloaded chunks or an offline player.
        if(!f.core.isEmpty())throw new IllegalStateException("Escrow core slot occupied");
        var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(f.dimension)));
        if(level==null)throw new IllegalStateException("Missing ritual dimension");
        var returned=new HashMap<String,String>();
        r.offerings.forEach((key,value)->{
            var stack=AltarInventory.decode(level,value);var remainder=AltarInventory.decode(level,d.state.slots.get(key));
            if(!remainder.isEmpty()){if(!ItemStack.isSameItemSameComponents(stack,remainder))throw new IllegalStateException("Escrow item mismatch");stack.grow(remainder.getCount());}
            returned.put(key,AltarInventory.encode(level,stack));
        });
        f.core=r.core;d.state.slots.putAll(returned);
        d.state.rituals.remove(r.id);d.flush(server);r.offerings.keySet().forEach(key->OfferingBlockEntity.refresh(level,key));
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){var d=AltarSavedData.get(e.getServer());for(var r:List.copyOf(d.state.rituals.values()))refund(d,r,e.getServer());}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var s=e.getServer();var d=AltarSavedData.get(s);
        for(var r:List.copyOf(d.state.rituals.values())){
            if(r.kind.equals("talent_reset")){TalentResetService.tick(s,d,r);continue;}
            var f=d.state.forges.get(r.forge);var p=s.getPlayerList().getPlayer(UUID.fromString(r.player));
            var record=SoulboundSavedData.get(s).find(UUID.fromString(r.player));
            if(p==null || !p.isAlive() || p.isSpectator() || !f.placed || !f.state.equals("ACTIVE") || !platform(p,f,r.rotation)
                    || record==null || !record.bondUuid().toString().equals(r.bond) || !CompanionEvolution.accepts(record,r.kind,d)
                    || ForgeService.rotation(p.level(),new BlockPos(f.x,f.y,f.z))!=r.rotation){refund(d,r,s);continue;}
            var c=(r.kind.equals("awakening") || r.kind.equals("ascension") || r.kind.equals("divine"))?br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,record):null;
            if((r.kind.equals("awakening") || r.kind.equals("ascension") || r.kind.equals("divine")) && (c==null || (r.kind.equals("awakening") && !record.activeForm().equals("BABY")) || c.distanceToSqr(f.x+.5,f.y+1,f.z+.5)>32*32)){refund(d,r,s);continue;}
            if(s.overworld().getGameTime()>=r.end){
                if(c!=null && c.distanceToSqr(f.x+.5,f.y+1.6,f.z+.5)>16){refund(d,r,s);continue;}
                var token=AltarTokens.issue(d,r.player,r.kind);if(r.kind.equals("awakening") || r.kind.equals("ascension") || r.kind.equals("divine"))token.bond=record.bondUuid().toString();
                d.state.rituals.remove(r.id);
                if(r.kind.equals("restoration")){
                    if(!CompanionEvolution.completeResurrection(s,d,record,token)){d.state.tokens.remove(token.id);d.state.rituals.put(r.id,r);refund(d,r,s);continue;}
                }else d.flush(s);
                if(c!=null){c.setRitualizing(false);c.stopFlight();}
                AltarTokens.deliver(p,d);p.level().playSound(null,new BlockPos(f.x,f.y,f.z),net.minecraft.sounds.SoundEvents.BEACON_POWER_SELECT,net.minecraft.sounds.SoundSource.BLOCKS,.8f,1.2f);
                p.displayClientMessage(Component.translatable(r.kind.equals("restoration")?"message.soulbound.resurrection_complete":"message.soulbound.ritual_complete"),false);
                if(p.containerMenu instanceof ForgeMenu menu && menu.isForge(f.id))ForgeService.open(p,new BlockPos(f.x,f.y,f.z));}
            else if(s.overworld().getGameTime()%10==0)p.level().sendParticles(ParticleTypes.SOUL,f.x+.5,f.y+1.5,f.z+.5,12,.7,.5,.7,.02);
        }
    }
    @SubscribeEvent public static void dragon(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent e){
        if(e.getEntity() instanceof EnderDragon dragon && e.getLevel() instanceof ServerLevel level && dragon.getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.KILLED){
            var d=AltarSavedData.get(level.getServer());if(d.state.slainDragons.add(dragon.getStringUUID())){
                d.flush(level.getServer());var essence=dragon.spawnAtLocation(level,new ItemStack(ModItems.ALTAR_ITEMS.get("draconic_essence").get(),2));
                if(essence!=null)essence.setPos(dragon.getX()+8,dragon.getY(),dragon.getZ()+8);
            }
        }
    }
}
