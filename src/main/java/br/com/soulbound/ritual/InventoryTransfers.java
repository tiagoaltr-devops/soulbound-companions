package br.com.soulbound.ritual;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Two-phase inventory hand-off. Player data receipts resolve pending operations after a crash. */
@EventBusSubscriber(modid="soulbound")
public final class InventoryTransfers {
    private static final String RECEIPTS="SoulboundAltarReceipts";
    private InventoryTransfers() {}
    public static boolean received(ServerPlayer p,String id){return p.getPersistentData().getCompound(RECEIPTS).map(t->t.getBooleanOr(id,false)).orElse(false);}
    public static void receipt(ServerPlayer p,String id){var tags=p.getPersistentData().getCompound(RECEIPTS).orElseGet(CompoundTag::new);tags.putBoolean(id,true);p.getPersistentData().put(RECEIPTS,tags);}
    public static void saveReceipt(ServerPlayer p,String id){
        var s=p.level().getServer();s.getPlayerList().saveAll();
        try {
            var file=s.getWorldPath(LevelResource.ROOT).resolve("playerdata/"+p.getStringUUID()+".dat");
            var tag=NbtIo.readCompressed(file,NbtAccounter.unlimitedHeap());
            boolean persisted=tag.getCompound("NeoForgeData").flatMap(t->t.getCompound(RECEIPTS)).map(t->t.getBooleanOr(id,false)).orElse(false);
            if(!persisted)throw new IllegalStateException("Missing player receipt "+id);
        }catch(Exception e){throw new IllegalStateException("Cannot verify altar inventory receipt",e);}
    }
    static String contents(AltarSavedData d,String target){return target.startsWith("forge/")?d.state.forges.get(target.substring(6)).core:d.state.slots.getOrDefault(target,"");}
    static void contents(AltarSavedData d,String target,String stack){if(target.startsWith("forge/"))d.state.forges.get(target.substring(6)).core=stack;else d.state.slots.put(target,stack);}
    public static boolean pending(AltarSavedData d,String target){return d.state.transfers.values().stream().anyMatch(t->t.target.equals(target));}
    public static boolean exchange(ServerPlayer p,String target,boolean single){
        var d=AltarSavedData.get(p.level().getServer());if(pending(d,target))return false;
        var existing=contents(d,target);boolean deposit=existing.isEmpty();
        ItemStack stack=deposit?p.getMainHandItem():AltarInventory.decode(p.level(),existing);
        if(target.startsWith("forge/")){
            var f=d.state.forges.get(target.substring(6));
            if(f==null || RitualService.busy(p.level(),f.id) || !ForgeActivation.coreAllowed(f,p.getStringUUID(),stack.is(br.com.soulbound.registry.ModItems.ALTAR_ITEMS.get("reactivation_essence").get())))return ForgeService.reject(p,"state");
        }else if(RitualService.locked(p.level(),BlockPos.of(Long.parseLong(target.substring(target.lastIndexOf(':')+1))))){return ForgeService.reject(p,"busy");}
        if(stack.isEmpty() || !deposit && p.getInventory().getFreeSlot()<0)return false;
        var t=new AltarSavedData.Transfer();t.id=UUID.randomUUID().toString();t.player=p.getStringUUID();t.target=target;t.deposit=deposit;
        t.stack=deposit?AltarInventory.encode(p.level(),stack.copyWithCount(single?1:stack.getCount())):existing;
        d.state.transfers.put(t.id,t);
        if(!deposit)contents(d,target,"");
        d.flush(p.level().getServer());
        if(deposit){stack.shrink(single?1:stack.getCount());receipt(p,t.id);saveReceipt(p,t.id);}
        finish(p,d,t);p.containerMenu.broadcastChanges();return true;
    }
    private static void finish(ServerPlayer p,AltarSavedData d,AltarSavedData.Transfer t){
        if(t.deposit){
            if(received(p,t.id)){
                if(!contents(d,t.target).isEmpty())throw new IllegalStateException("Deposit destination occupied");
                contents(d,t.target,t.stack);
            }
            // Without a persisted player receipt, the original inventory still owns the input.
        }else if(!received(p,t.id)){
            int free=p.getInventory().getFreeSlot();if(free<0)return;
            p.getInventory().setItem(free,AltarInventory.decode(p.level(),t.stack));receipt(p,t.id);saveReceipt(p,t.id);
        }
        d.state.transfers.remove(t.id);d.flush(p.level().getServer());if(!t.target.startsWith("forge/"))OfferingBlockEntity.refresh(p.level(),t.target);
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)recover(p);}
    public static void recover(ServerPlayer p){var d=AltarSavedData.get(p.level().getServer());for(var t:List.copyOf(d.state.transfers.values()))if(t.player.equals(p.getStringUUID()))finish(p,d,t);}
}
