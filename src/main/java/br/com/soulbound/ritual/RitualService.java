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
    public static boolean busy(ServerLevel level,String forge){return AltarSavedData.get(level.getServer()).state.rituals.values().stream().anyMatch(r->r.forge.equals(forge));}
    public static boolean locked(ServerLevel level,BlockPos pos){String key=AltarInventory.key(level,pos);return AltarSavedData.get(level.getServer()).state.rituals.values().stream().anyMatch(r->r.offerings.containsKey(key));}
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
        var record=SoulboundSavedData.get(s).find(p.getUUID());
        if(!CompanionEvolution.accepts(record,kind,d))return false;
        int xp=cost(kind)+(p.getStringUUID().equals(f.owner)?0:Config.RITUAL_VISITOR_XP.get());if(p.experienceLevel<xp)return false;
        var costs=RitualDefinitions.offerings(level,kind);var keys=ForgeService.offeringKeys(level,f,rotation);
        for(int i=0;i<3;i++)if(!costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return false;
        var r=new AltarSavedData.Ritual();r.id=UUID.randomUUID().toString();r.player=p.getStringUUID();r.forge=f.id;r.kind=kind;
        r.bond=record.bondUuid().toString();r.rotation=rotation;r.end=s.overworld().getGameTime()+Config.RITUAL_TICKS.get();r.core=f.core;
        // XP is persisted before the one-file escrow commit. A crash in this narrow gap is an interrupted paid attempt, with materials still in place.
        p.giveExperienceLevels(-xp);InventoryTransfers.receipt(p,"ritual:"+r.id);InventoryTransfers.saveReceipt(p,"ritual:"+r.id);
        f.core="";
        for(int i=0;i<3;i++){
            var stack=AltarInventory.decode(level,d.state.slots.get(keys.get(i)));
            r.offerings.put(keys.get(i),AltarInventory.encode(level,stack.split(costs.get(i).count())));
            d.state.slots.put(keys.get(i),AltarInventory.encode(level,stack));
        }
        d.state.rituals.put(r.id,r);d.flush(s);return true;
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
        d.state.rituals.remove(r.id);d.flush(server);
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){var d=AltarSavedData.get(e.getServer());for(var r:List.copyOf(d.state.rituals.values()))refund(d,r,e.getServer());}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var s=e.getServer();var d=AltarSavedData.get(s);
        for(var r:List.copyOf(d.state.rituals.values())){
            var f=d.state.forges.get(r.forge);var p=s.getPlayerList().getPlayer(UUID.fromString(r.player));
            var record=SoulboundSavedData.get(s).find(UUID.fromString(r.player));
            if(p==null || !p.isAlive() || p.isSpectator() || !f.placed || !f.state.equals("ACTIVE") || !platform(p,f,r.rotation)
                    || record==null || !record.bondUuid().toString().equals(r.bond) || !CompanionEvolution.accepts(record,r.kind,d)
                    || ForgeService.rotation(p.level(),new BlockPos(f.x,f.y,f.z))!=r.rotation){refund(d,r,s);continue;}
            if(s.overworld().getGameTime()>=r.end){AltarTokens.issue(d,r.player,r.kind);d.state.rituals.remove(r.id);d.flush(s);AltarTokens.deliver(p,d);p.displayClientMessage(Component.translatable("message.soulbound.ritual_complete"),false);}
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
