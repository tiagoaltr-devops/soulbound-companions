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
        var f=at(p.level(),pos);if(f==null)return;
        var record=f.owner==null?null:br.com.soulbound.companion.identity.SoulboundSavedData.get(p.level().getServer()).find(UUID.fromString(f.owner));
        String owner=f.owner==null?"—":record==null?f.owner:record.ownerLastKnownName();
        String ritual="activation";var core=AltarInventory.decode(p.level(),f.core);
        if(f.state.equals("ACTIVE"))for(String kind:new String[]{"awakening","ascension","divine","restoration"})if(core.is(ModItems.ALTAR_ITEMS.get(kind+"_core").get()))ritual=kind;
        var costs=ritual.equals("activation")?RitualDefinitions.activation(p.level()):RitualDefinitions.offerings(p.level(),ritual);
        var materials=new ArrayList<Component>();
        for(var cost:costs){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(cost.item()));materials.add(Component.literal(cost.count()+" × ").append(item.getName()));}
        Component coreName=core.isEmpty()?Component.translatable("menu.soulbound.empty"):core.getHoverName();
        p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new ForgeMenu(id,inv,f.id),Component.translatable("menu.soulbound.forge",owner)),buffer->{for(var text:materials)net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer,text);net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer,coreName);});
    }
    public static boolean activate(ServerPlayer p,AltarSavedData.Forge f){
        var level=p.level();var d=AltarSavedData.get(level.getServer());
        int rotation=rotation(level,new BlockPos(f.x,f.y,f.z));
        if(!f.placed || f.state.equals("DESTROYED") || f.state.equals("ACTIVE") || f.owner!=null && !f.owner.equals(p.getStringUUID()) || rotation<0)return false;
        boolean first=f.owner==null;
        var costs=RitualDefinitions.activation(level);
        var keys=offeringKeys(level,f,rotation);
        for(int i=0;i<3;i++)if(!costs.get(i).matches(AltarInventory.decode(level,d.state.slots.get(keys.get(i)))))return false;
        var essence=AltarInventory.decode(level,f.core);
        if(!first && !essence.is(ModItems.ALTAR_ITEMS.get("reactivation_essence").get()))return false;
        var result=AltarPolicy.resolveForge(first,f.attempts,p.getRandom().nextDouble(),p.getRandom().nextDouble(),failure(f.attempts),br.com.soulbound.Config.FORGE_CRITICAL_CHANCE.get());
        boolean destroyed=result.state().equals("DESTROYED");
        // Persist the draw and wear before publishing the result. No subsequent random draw is replayed.
        f.attempts=result.attempts();
        f.owner=first?p.getStringUUID():f.owner;f.state=result.state();
        for(int i=0;i<3;i++){var stack=AltarInventory.decode(level,d.state.slots.get(keys.get(i)));stack.shrink(costs.get(i).count());d.state.slots.put(keys.get(i),AltarInventory.encode(level,stack));}
        if(!first){essence.shrink(1);f.core=AltarInventory.encode(level,essence);}
        d.flush(level.getServer());
        if(destroyed){level.removeBlock(new BlockPos(f.x,f.y,f.z),false);f.placed=false;d.flush(level.getServer());}
        p.displayClientMessage(Component.translatable("message.soulbound.forge."+f.state.toLowerCase(Locale.ROOT)),false);return true;
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
