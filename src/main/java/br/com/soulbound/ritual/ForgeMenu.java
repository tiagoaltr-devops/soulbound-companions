package br.com.soulbound.ritual;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ForgeMenu extends AbstractContainerMenu {
    private final String forgeId;
    public final ContainerData info=new SimpleContainerData(7);
    private final Player viewer;
    public java.util.List<net.minecraft.network.chat.Component> materials=java.util.List.of();
    public net.minecraft.network.chat.Component coreName=net.minecraft.network.chat.Component.empty();
    public ForgeMenu(int id,Inventory inv){this(id,inv,(String)null);}
    public ForgeMenu(int id,Inventory inv,net.minecraft.network.RegistryFriendlyByteBuf buffer){
        this(id,inv,(String)null);var texts=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        for(int i=0;i<3;i++)texts.add(net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buffer));materials=java.util.List.copyOf(texts);
        coreName=net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.decode(buffer);
    }
    public ForgeMenu(int id,Inventory inv,String forge){super(br.com.soulbound.registry.ModMenus.SOUL_FORGE.get(),id);forgeId=forge;viewer=inv.player;addDataSlots(info);}
    public AltarSavedData.Forge forge(ServerPlayer p){return AltarSavedData.get(p.level().getServer()).state.forges.get(forgeId);}
    @Override public boolean stillValid(Player player){
        if(!(player instanceof ServerPlayer p))return true;var f=forge(p);
        return f!=null && f.placed && f.dimension.equals(p.level().dimension().identifier().toString()) && p.distanceToSqr(f.x+.5,f.y+.5,f.z+.5)<64;
    }
    @Override public void broadcastChanges(){
        if(forgeId!=null){var s=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();if(s!=null){var f=AltarSavedData.get(s).state.forges.get(forgeId);if(f!=null){
            info.set(0,f.owner==null?0:f.state.equals("ACTIVE")?2:f.state.equals("DESTROYED")?3:1);info.set(1,f.attempts);
            info.set(2,f.owner==null?0:(int)Math.round(ForgeService.failure(f.attempts)*10000));info.set(3,f.core.isEmpty()?0:1);
            info.set(4,(int)Math.round(br.com.soulbound.Config.FORGE_CRITICAL_CHANCE.get()*10000));
            info.set(5,0);info.set(6,br.com.soulbound.Config.RITUAL_TICKS.get()/20);
            if(viewer instanceof ServerPlayer p){var stack=AltarInventory.decode(p.level(),f.core);
                for(String kind:new String[]{"awakening","ascension","divine","restoration"})if(stack.is(br.com.soulbound.registry.ModItems.ALTAR_ITEMS.get(kind+"_core").get()))info.set(5,RitualService.cost(kind)+(p.getStringUUID().equals(f.owner)?0:br.com.soulbound.Config.RITUAL_VISITOR_XP.get()));}
        }}}super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(!(player instanceof ServerPlayer p)||!stillValid(p))return false;var f=forge(p);
        if(id==0){boolean changed=ForgeService.activate(p,f);if(changed)ForgeService.open(p,new net.minecraft.core.BlockPos(f.x,f.y,f.z));return changed;}
        if(id==2)return RitualService.start(p,f);
        if(id==1){if(RitualService.busy(p.level(),f.id))return false;boolean exchanged=InventoryTransfers.exchange(p,"forge/"+f.id,true);if(exchanged)ForgeService.open(p,new net.minecraft.core.BlockPos(f.x,f.y,f.z));return exchanged;}
        return false;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
