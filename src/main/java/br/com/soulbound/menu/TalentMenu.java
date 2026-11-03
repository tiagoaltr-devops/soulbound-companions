package br.com.soulbound.menu;

import br.com.soulbound.companion.SoulStaffCommands;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.companion.progression.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Slotless authoritative talent menu; clients send a definition index, never a balance or owner. */
public final class TalentMenu extends AbstractContainerMenu {
    public final SimpleContainerData data=new SimpleContainerData(9+TalentDefinitions.ALL.size()*2);
    private final Player viewer;private final InteractionHand hand;private final SoulboundRecord session;
    public TalentMenu(int id,Inventory inv){this(id,inv,InteractionHand.MAIN_HAND,null);}
    public TalentMenu(int id,Inventory inv,InteractionHand hand,SoulboundRecord session){super(br.com.soulbound.registry.ModMenus.TALENTS.get(),id);viewer=inv.player;this.hand=hand;this.session=session;addDataSlots(data);refresh();}
    private SoulboundRecord record(ServerPlayer p){
        if(session==null || p!=viewer)return null;var r=SoulStaffCommands.authorize(p,p.getItemInHand(hand));
        return r!=null && r.matches(session.ownerUuid(),session.bondUuid(),session.companionUuid())?r:null;
    }
    private void refresh(){
        if(!(viewer instanceof ServerPlayer p))return;var r=record(p);if(r==null)return;
        data.set(0,r.progression().level());data.set(1,(int)r.progression().xp());data.set(2,r.progression().available());
        data.set(3,r.progression().earnedPoints());data.set(4,r.activeForm().equals("BABY")?0:r.activeForm().equals("JUNIOR")?1:2);
        data.set(5,r.unlockedForms().contains("JUNIOR")?1:0);data.set(6,r.progression().level()==30?0:(int)ProgressionState.required(r.progression().level()));
        data.set(7+TalentDefinitions.ALL.size()*2,r.unlockedForms().contains("PLENO")?1:0);data.set(8+TalentDefinitions.ALL.size()*2,r.unlockedForms().contains("GOD")?1:0);
        boolean busy=br.com.soulbound.companion.CompanionFormService.ritualBusy(p.level().getServer(),p.getUUID());
        for(int i=0;i<TalentDefinitions.ALL.size();i++){var t=TalentDefinitions.ALL.get(i);data.set(7+i,r.progression().rank(t.id()));data.set(7+TalentDefinitions.ALL.size()+i,busy?9:TalentDefinitions.rejection(r,t));}
    }
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p.level().isClientSide() || p instanceof ServerPlayer server && record(server)!=null;}
    @Override public boolean clickMenuButton(Player p,int id){
        if(!(p instanceof ServerPlayer server) || server.containerMenu!=this || id<0 || id>=TalentDefinitions.ALL.size())return false;
        var r=record(server);if(r==null)return false;return ProgressionService.invest(server,r,TalentDefinitions.ALL.get(id).id());
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    public int rank(int index){return data.get(7+index);}
    public int reason(int index){return data.get(7+TalentDefinitions.ALL.size()+index);}
    public boolean godUnlocked(){return data.get(8+TalentDefinitions.ALL.size()*2)==1;}
    public boolean plenoUnlocked(){return data.get(7+TalentDefinitions.ALL.size()*2)==1;}
}
