package br.com.soulbound.ritual;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

public final class TrialMenu extends AbstractContainerMenu {
    private final String ruinId;
    public final ContainerData info=new SimpleContainerData(5);
    public TrialMenu(int id,Inventory inv){this(id,inv,null);}
    public TrialMenu(int id,Inventory inv,String ruin){super(br.com.soulbound.registry.ModMenus.ANCESTRAL_TRIAL.get(),id);ruinId=ruin;addDataSlots(info);}
    @Override public boolean stillValid(Player player){
        if(!(player instanceof ServerPlayer p))return true;
        var r=AltarSavedData.get(p.level().getServer()).state.ruins.get(ruinId);
        return r!=null && p.distanceToSqr(r.x+.5,r.y+.5,r.z+.5)<64;
    }
    @Override public void broadcastChanges(){
        if(ruinId!=null){var s=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();if(s!=null){
            var r=AltarSavedData.get(s).state.ruins.get(ruinId);if(r!=null){
                info.set(0,r.completions+1);info.set(1,r.trial==null?-1:r.trial.phase);info.set(2,r.trial==null?0:r.trial.cursor);
                info.set(3,r.trial!=null && r.trial.phase==0?r.trial.sequence[r.trial.cursor]:-1);info.set(4,r.trial==null?0:r.trial.ticks);
            }
        }}super.broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer p)||!stillValid(p))return false;
        var r=AltarSavedData.get(p.level().getServer()).state.ruins.get(ruinId);if(r==null)return false;
        boolean result=button==7?TrialEngine.start(p,r):TrialEngine.symbol(p,r,button);
        if(button==7 && !result)p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.soulbound.trial_denied"),false);
        return result;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
