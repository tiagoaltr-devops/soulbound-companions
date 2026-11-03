package br.com.soulbound.ritual;

import br.com.soulbound.registry.ModAltar;
import br.com.soulbound.registry.ModItems;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

@EventBusSubscriber(modid="soulbound")
public final class ForgeService {
    private ForgeService() {}
    public static double failure(int attempts){return switch(attempts){case 0->br.com.soulbound.Config.FORGE_FAILURE_1.get();case 1->br.com.soulbound.Config.FORGE_FAILURE_2.get();case 2->br.com.soulbound.Config.FORGE_FAILURE_3.get();case 3->br.com.soulbound.Config.FORGE_FAILURE_4.get();default->br.com.soulbound.Config.FORGE_FAILURE_5.get();};}
    public static AltarSavedData.Forge at(ServerLevel level,BlockPos pos){
        for(var f:AltarSavedData.get(level.getServer()).state.forges.values())if(f.placed && f.x==pos.getX() && f.y==pos.getY() && f.z==pos.getZ() && f.dimension.equals(level.dimension().identifier().toString()))return f;
        return null;
    }
    public static int rotation(ServerLevel level,BlockPos center){
        if(!level.getBlockState(center).is(ModAltar.FORGE.get()))return -1;
        return AltarPolicy.rotation((x,z,component)->{var pos=center.offset(x,0,z);return level.hasChunkAt(pos)
                && level.getBlockState(pos).is(component==0?ModAltar.OFFERING.get():component==1?ModAltar.PLATFORM.get():ModAltar.STONE.get());});
    }
    public static void open(ServerPlayer p,BlockPos pos){
        open(p,pos,Component.empty());
    }
    private static void open(ServerPlayer p,BlockPos pos,Component feedback){
        var f=at(p.level(),pos);if(f==null)return;
        var record=f.owner==null?null:br.com.soulbound.companion.identity.SoulboundSavedData.get(p.level().getServer()).find(UUID.fromString(f.owner));
        String owner=f.owner==null?"—":record==null?f.owner:record.ownerLastKnownName();
        String ritual=f.state.equals("ACTIVE")?"awakening":"activation";var core=AltarInventory.decode(p.level(),f.core);
        var viewerRecord=br.com.soulbound.companion.identity.SoulboundSavedData.get(p.level().getServer()).find(p.getUUID());
        if(f.state.equals("ACTIVE") && core.isEmpty() && viewerRecord!=null && viewerRecord.evolutionStage().equals("PLENO") && !viewerRecord.unlockedForms().contains("GOD"))ritual="divine";
        if(f.state.equals("ACTIVE") && core.isEmpty() && viewerRecord!=null && viewerRecord.evolutionStage().equals("JUNIOR"))ritual="ascension";
        if(f.state.equals("ACTIVE") && viewerRecord!=null && viewerRecord.canResurrect())ritual="restoration";
        else if(f.state.equals("ACTIVE"))for(String kind:new String[]{"awakening","ascension","divine","restoration"})if(core.is(ModItems.ALTAR_ITEMS.get(kind+"_core").get()))ritual=kind;
        var costs=ritual.equals("activation")?RitualDefinitions.activation(p.level()):RitualDefinitions.offerings(p.level(),ritual);
        var materials=new ArrayList<Component>();
        for(var cost:costs){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(cost.item()));materials.add(Component.literal(cost.count()+" × ").append(item.getName()));}
        Component coreName=core.isEmpty()?Component.translatable("menu.soulbound.empty"):core.getHoverName();
        Component title=f.owner==null?Component.translatable("block.soulbound.soul_forge"):Component.translatable("menu.soulbound.forge",owner);
        p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new ForgeMenu(id,inv,f.id),title),buffer->{buffer.writeVarInt(ForgeActivation.view(f));for(var text:materials)net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer,text);net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer,coreName);net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer,feedback);});
    }
    public static boolean reject(ServerPlayer p,String reason){return reject(p,Component.translatable("message.soulbound.forge_error."+reason));}
    public static boolean reject(ServerPlayer p,Component message){
        p.displayClientMessage(message,false);
        if(p.containerMenu instanceof ForgeMenu menu && menu.stillValid(p)){
            var f=menu.forge(p);open(p,new BlockPos(f.x,f.y,f.z),message);
        }
        return false;
    }
    public static boolean activate(ServerPlayer p,AltarSavedData.Forge f){
        var level=p.level();var d=AltarSavedData.get(level.getServer());
        if(!p.isAlive() || p.isSpectator())return reject(p,"permission");
        String reason=ForgeActivation.rejection(f,p.getStringUUID());if(reason!=null)return reject(p,reason);
        if(RitualService.busy(level,f.id))return reject(p,"busy");
        int rotation=rotation(level,new BlockPos(f.x,f.y,f.z));
        if(rotation<0){
            for(int turn=0;turn<4;turn++){
                var platform=AltarPolicy.rotate(0,2,turn);var center=new BlockPos(f.x,f.y,f.z);
                if(!level.hasChunkAt(center.offset(platform[0],0,platform[1])) || !level.getBlockState(center.offset(platform[0],0,platform[1])).is(ModAltar.PLATFORM.get()))continue;
                for(var offset:new int[][]{{0,-2},{-2,0},{2,0}}){var v=AltarPolicy.rotate(offset[0],offset[1],turn);var pos=center.offset(v[0],0,v[1]);
                    if(!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ModAltar.OFFERING.get()))return reject(p,"altar_missing");}
            }
            return reject(p,"structure");
        }
        boolean first=f.owner==null;
        var costs=RitualDefinitions.activation(level);
        var keys=offeringKeys(level,f,rotation);
        if(keys.stream().anyMatch(key->InventoryTransfers.pending(d,key) || RitualService.locked(level,BlockPos.of(Long.parseLong(key.substring(key.lastIndexOf(':')+1))))) || InventoryTransfers.pending(d,"forge/"+f.id))return reject(p,"transfer");
        for(int i=0;i<3;i++)if(!costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i))))){
            var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(costs.get(i).item()));
            return reject(p,Component.translatable("message.soulbound.forge_error.offering",Component.translatable("menu.soulbound.offering_position."+i),costs.get(i).count(),item.getName()));
        }
        var essence=AltarInventory.decode(level,f.core);
        if(!first && essence.isEmpty() && p.getMainHandItem().is(ModItems.ALTAR_ITEMS.get("reactivation_essence").get())){
            if(!InventoryTransfers.exchange(p,"forge/"+f.id,true))return reject(p,"transfer");essence=AltarInventory.decode(level,f.core);
        }
        if(!first && !essence.is(ModItems.ALTAR_ITEMS.get("reactivation_essence").get()))return reject(p,"essence");
        var result=AltarPolicy.resolveForge(first,f.attempts,p.getRandom().nextDouble(),p.getRandom().nextDouble(),failure(f.attempts),br.com.soulbound.Config.FORGE_CRITICAL_CHANCE.get());
        var a=new AltarSavedData.Activation();a.player=p.getStringUUID();a.end=level.getServer().overworld().getGameTime()+ForgeActivation.DURATION;
        a.rotation=rotation;a.result=result.state();a.attempts=result.attempts();
        var remainders=new HashMap<String,String>();
        for(int i=0;i<3;i++){var stack=AltarInventory.decode(level,d.state.slots.get(keys.get(i)));a.offerings.put(keys.get(i),AltarInventory.encode(level,stack.split(costs.get(i).count())));remainders.put(keys.get(i),AltarInventory.encode(level,stack));}
        if(!first)a.core=AltarInventory.encode(level,essence.copyWithCount(1));
        if(!ForgeActivation.begin(d,f,a,remainders))return reject(p,"busy");
        if(!first){essence.shrink(1);f.core=AltarInventory.encode(level,essence);}
        d.flush(level.getServer());
        a.offerings.keySet().forEach(key->OfferingBlockEntity.refresh(level,key));
        level.playSound(null,new BlockPos(f.x,f.y,f.z),SoundEvents.BEACON_ACTIVATE,SoundSource.BLOCKS,.7f,1.2f);
        p.displayClientMessage(Component.translatable("message.soulbound.forge_start"),false);return true;
    }
    private static void refund(net.minecraft.server.MinecraftServer server,ServerLevel level,AltarSavedData d,AltarSavedData.Forge f){
        var a=f.activation;
        a.offerings.forEach((key,value)->{
            var stack=AltarInventory.decode(level,value);var rest=AltarInventory.decode(level,d.state.slots.get(key));
            if(!rest.isEmpty() && !ItemStack.isSameItemSameComponents(stack,rest))throw new IllegalStateException("Activation escrow slot occupied");
            stack.grow(rest.getCount());d.state.slots.put(key,AltarInventory.encode(level,stack));
        });
        if(!a.core.isEmpty()){
            var stack=AltarInventory.decode(level,a.core);var rest=AltarInventory.decode(level,f.core);
            if(!rest.isEmpty() && !ItemStack.isSameItemSameComponents(stack,rest))throw new IllegalStateException("Activation core occupied");
            stack.grow(rest.getCount());f.core=AltarInventory.encode(level,stack);
        }
        f.activation=null;d.flush(server);a.offerings.keySet().forEach(key->OfferingBlockEntity.refresh(level,key));
        var p=server.getPlayerList().getPlayer(UUID.fromString(a.player));if(p!=null)reject(p,"cancelled");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        var server=event.getServer();var d=AltarSavedData.get(server);long now=server.overworld().getGameTime();
        for(var f:d.state.forges.values()){
            if(f.activation==null)continue;
            var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(f.dimension)));
            var pos=new BlockPos(f.x,f.y,f.z);if(level==null)continue;
            // Resume persisted work when all relevant chunks are available; never force-load them.
            boolean loaded=true;for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)loaded &= level.hasChunkAt(pos.offset(x,0,z));
            if(!loaded)continue;
            if(!f.placed || rotation(level,pos)!=f.activation.rotation){refund(server,level,d,f);continue;}
            if(now<f.activation.end){
                if(now%10==0)level.sendParticles(ParticleTypes.SOUL,f.x+.5,f.y+1.3,f.z+.5,f.activation.end-now<60?8:3,.35,.45,.35,.015);
                continue;
            }
            boolean first=f.owner==null;var completed=ForgeActivation.complete(f,now);if(completed==null)continue;
            d.flush(server);
            boolean success=f.state.equals("ACTIVE"),critical=f.state.equals("DESTROYED");
            if(critical){level.removeBlock(pos,false);f.placed=false;d.flush(server);}
            level.sendParticles(success?ParticleTypes.SOUL_FIRE_FLAME:ParticleTypes.WITCH,f.x+.5,f.y+1.4,f.z+.5,critical?40:24,.6,.6,.6,.04);
            if(success)for(int y=0;y<5;y++)level.sendParticles(ParticleTypes.SOUL,f.x+.5,f.y+1+y*.4,f.z+.5,4,.12,.12,.12,.02);
            level.playSound(null,pos,success?SoundEvents.BEACON_POWER_SELECT:SoundEvents.GLASS_BREAK,SoundSource.BLOCKS,critical?1.5f:.9f,critical?.65f:1f);
            var p=server.getPlayerList().getPlayer(UUID.fromString(completed.player));
            if(p!=null)p.displayClientMessage(Component.translatable(first?"message.soulbound.forge_awakened":"message.soulbound.forge."+f.state.toLowerCase(Locale.ROOT)),false);
            for(var viewer:server.getPlayerList().getPlayers())if(viewer.containerMenu instanceof ForgeMenu menu && menu.isForge(f.id) && menu.stillValid(viewer))open(viewer,pos);
        }
    }
    public static List<String> offeringKeys(ServerLevel level,AltarSavedData.Forge f,int rotation){
        var keys=new ArrayList<String>();for(var offset:new int[][]{{0,-2},{-2,0},{2,0}}){var v=AltarPolicy.rotate(offset[0],offset[1],rotation);keys.add(AltarInventory.key(level,new BlockPos(f.x+v[0],f.y,f.z+v[1])));}return keys;
    }
    @SubscribeEvent public static void breaking(BlockEvent.BreakEvent e){
        if(!(e.getLevel() instanceof ServerLevel level))return;
        if(e.getState().is(ModAltar.OFFERING.get())){
            if(RitualService.locked(level,e.getPos())){e.setCanceled(true);return;}
            String stored=AltarSavedData.get(level.getServer()).state.slots.get(AltarInventory.key(level,e.getPos()));
            if(stored!=null && !stored.isEmpty())e.setCanceled(true);
        }
        if(!e.getState().is(ModAltar.FORGE.get()))return;
        e.setCanceled(true);var f=at(level,e.getPos());
        if(!(e.getPlayer() instanceof ServerPlayer p) || f==null || f.owner==null || !f.owner.equals(p.getStringUUID()) || f.state.equals("DESTROYED"))return;
        int free=p.getInventory().getFreeSlot();if(free<0 || !f.core.isEmpty() || RitualService.busy(level,f.id))return;
        f.state="INACTIVE";f.placed=false;var d=AltarSavedData.get(level.getServer());d.flush(level.getServer());
        var item=new ItemStack(ModItems.SOUL_FORGE.get());AltarTokens.tag(item,f.id);level.removeBlock(e.getPos(),false);p.getInventory().setItem(free,item);level.getServer().getPlayerList().saveAll();
    }
}
