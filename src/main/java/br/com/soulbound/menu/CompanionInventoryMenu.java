package br.com.soulbound.menu;

import java.util.*;
import br.com.soulbound.companion.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarInventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Native nine-slot container. Each click completes on the server thread before its canonical commit. */
public final class CompanionInventoryMenu extends AbstractContainerMenu {
    private final Player owner;private final UUID bond;private final SimpleContainer contents=new SimpleContainer(9);private List<String> snapshot;private boolean changing;
    public CompanionInventoryMenu(int id,Inventory inventory){this(id,inventory,null);}
    public CompanionInventoryMenu(int id,Inventory inventory,SoulboundRecord session){
        super(br.com.soulbound.registry.ModMenus.COMPANION_INVENTORY.get(),id);owner=inventory.player;bond=session==null?null:session.bondUuid();
        if(owner instanceof ServerPlayer p && session!=null)load(p,session);
        for(int i=0;i<9;i++){final int index=i;int x=i<5?20+i*28:34+(i-5)*28;int y=i<5?35:76;
            addSlot(new Slot(contents,i,x,y){@Override public boolean mayPlace(ItemStack stack){if(owner instanceof ServerPlayer p){var c=companion(p);return c!=null && CompanionEquipment.allowed(index,stack,c.getType());}return CompanionEquipment.allowed(index,stack,br.com.soulbound.registry.ModEntities.LUPUS_COMPANION.get());}@Override public int getMaxStackSize(){return index<5?1:super.getMaxStackSize();}});
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,112+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,170));
    }
    private SoulboundRecord record(ServerPlayer p){var r=SoulboundSavedData.get(p.level().getServer()).find(p.getUUID());return p==owner && r!=null && r.bondUuid().equals(bond)?r:null;}
    private LupusCompanionEntity companion(ServerPlayer p){var r=record(p);var c=r==null?null:SoulStaffCommands.loadedCompanion(p,r);return c!=null && c.isAlive() && c.distanceToSqr(p)<=64 && !CompanionFormService.ritualBusy(p.level().getServer(),p.getUUID())?c:null;}
    private void load(ServerPlayer p,SoulboundRecord r){changing=true;for(int i=0;i<9;i++)contents.setItem(i,AltarInventory.decode(p.level(),r.survival().slots().get(i)));snapshot=r.survival().slots();changing=false;}
    @Override public boolean stillValid(Player p){return p.level().isClientSide() || p instanceof ServerPlayer server && companion(server)!=null;}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        if(!(player instanceof ServerPlayer p) || !stillValid(p) || changing)return;
        CompanionEquipment.capture(companion(p));var r=record(p);if(!r.survival().slots().equals(snapshot)){load(p,r);broadcastChanges();return;}
        changing=true;try{super.clicked(slot,button,type,player);commit(p);}finally{changing=false;}
    }
    private void commit(ServerPlayer p){
        var data=SoulboundSavedData.get(p.level().getServer());var r=record(p);var c=companion(p);if(r==null || c==null)throw new IllegalStateException("Companion inventory session lost authority");
        var next=new ArrayList<String>();for(int i=0;i<9;i++){var stack=contents.getItem(i);if(!CompanionEquipment.allowed(i,stack,c.getType()) || stack.getCount()>(i<5?1:stack.getMaxStackSize()))throw new IllegalStateException("Invalid companion slot movement");next.add(AltarInventory.encode(p.level(),stack));}
        if(!next.equals(snapshot)){var updated=r.withSurvival(r.survival().contents(next));data.put(updated);data.flush(p.level().getServer());snapshot=List.copyOf(next);CompanionEquipment.apply(c,updated);}
    }
    @Override public void broadcastChanges(){if(!changing && owner instanceof ServerPlayer p){var r=record(p);if(r!=null && !r.survival().slots().equals(snapshot))load(p,r);}super.broadcastChanges();}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(!(player instanceof ServerPlayer p) || !stillValid(p))return ItemStack.EMPTY;
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<9){if(!moveItemStackTo(stack,9,slots.size(),true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<9;i++)if(slots.get(i).mayPlace(stack) && moveItemStackTo(stack,i,i+1,false)){moved=true;if(stack.isEmpty())break;}if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
