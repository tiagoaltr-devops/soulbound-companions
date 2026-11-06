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
public class CompanionInventoryMenu extends AbstractContainerMenu {
    private final Player owner;private final UUID bond;private final SimpleContainer contents=new SimpleContainer(9);private List<String> snapshot;private boolean changing;
    public CompanionInventoryMenu(int id,Inventory inventory){this(id,inventory,null);}
    public CompanionInventoryMenu(int id,Inventory inventory,SoulboundRecord session){this(br.com.soulbound.registry.ModMenus.COMPANION_INVENTORY.get(),id,inventory,session);}
    protected CompanionInventoryMenu(MenuType<?> type,int id,Inventory inventory,SoulboundRecord session){
        super(type,id);owner=inventory.player;bond=session==null?null:session.bondUuid();
        if(owner instanceof ServerPlayer p && session!=null)load(p,session);
        for(int i=0;i<45;i++)addSlot(makeSlot(inventory,i,i<5?20+i*28:i<9?34+(i-5)*28:i<36?8+(i-9)%9*18:8+(i-36)*18,i<5?35:i<9?76:i<36?112+(i-9)/9*18:170));
    }
    protected boolean inventoryVisible(){return true;}
    protected final boolean inventoryAuthorized(ServerPlayer p){return inventoryVisible() && stillValid(p) && companion(p)!=null;}
    private Slot makeSlot(Inventory inventory,int index,int x,int y){
        var container=index<9?contents:inventory;int cell=index<9?index:index<36?index: index-36;
        return new Slot(container,cell,x,y){
            @Override public boolean isActive(){return inventoryVisible();}
            @Override public net.minecraft.resources.Identifier getNoItemIcon(){return switch(index){case 0->InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;case 1->InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;case 2->InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;case 3->InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;default->null;};}
            @Override public boolean mayPickup(Player p){return inventoryVisible() && (p.level().isClientSide() || p instanceof ServerPlayer server && inventoryAuthorized(server));}
            @Override public boolean mayPlace(ItemStack stack){
                if(!inventoryVisible())return false;if(index>=9)return true;
                if(owner instanceof ServerPlayer p){var c=companion(p);return c!=null && CompanionEquipment.allowed(index,stack,c.getType());}
                return CompanionEquipment.allowed(index,stack,br.com.soulbound.registry.ModEntities.LUPUS_COMPANION.get());
            }
            @Override public int getMaxStackSize(){return index<5?1:super.getMaxStackSize();}
        };
    }
    /** Client-only presentation coordinates, same containers/slot IDs. No data copies or mutations. */
    public record SlotPosition(int x,int y){}
    public static SlotPosition inventoryPosition(int index,int x,int y){
        if(index<0 || index>=45)throw new IllegalArgumentException("Invalid inventory slot");
        return new SlotPosition(index<5?x+index*18:index<9?x+94+(index-5)*18:x+(index<36?(index-9)%9:index-36)*18,index<9?y:index<36?y+30+(index-9)/9*18:y+88);
    }
    public final void layoutInventory(int x,int y){
        if(!owner.level().isClientSide())return;
        for(int i=0;i<45;i++){
            var pos=inventoryPosition(i,x,y);var slot=makeSlot(owner.getInventory(),i,pos.x(),pos.y());slot.index=i;slots.set(i,slot);
        }
    }
    private SoulboundRecord record(ServerPlayer p){var r=SoulboundSavedData.get(p.level().getServer()).find(p.getUUID());return p==owner && p.isAlive() && !p.isSpectator() && r!=null && r.bondUuid().equals(bond)?r:null;}
    private LupusCompanionEntity companion(ServerPlayer p){var r=record(p);var c=r==null?null:SoulStaffCommands.loadedCompanion(p,r);return c!=null && c.isAlive() && c.distanceToSqr(p)<=64 && !CompanionFormService.ritualBusy(p.level().getServer(),p.getUUID())?c:null;}
    private void load(ServerPlayer p,SoulboundRecord r){changing=true;for(int i=0;i<9;i++)contents.setItem(i,AltarInventory.decode(p.level(),r.survival().slots().get(i)));snapshot=r.survival().slots();changing=false;}
    @Override public boolean stillValid(Player p){return p.level().isClientSide() || p instanceof ServerPlayer server && companion(server)!=null;}
    @Override public void clicked(int slot,int button,ClickType type,Player player){
        // Native visual prediction only. Canonical validation/commit below runs exclusively on the server.
        if(player.level().isClientSide()){if(inventoryVisible())super.clicked(slot,button,type,player);return;}
        if(!(player instanceof ServerPlayer p) || !inventoryAuthorized(p) || changing)return;
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
        if(player.level().isClientSide()){if(!inventoryVisible())return ItemStack.EMPTY;}
        else if(!(player instanceof ServerPlayer p) || !inventoryAuthorized(p))return ItemStack.EMPTY;
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
        if(index<9){if(!moveItemStackTo(stack,9,slots.size(),true))return ItemStack.EMPTY;}
        else {boolean moved=false;for(int i=0;i<9;i++)if(slots.get(i).mayPlace(stack) && moveItemStackTo(stack,i,i+1,false)){moved=true;if(stack.isEmpty())break;}if(!moved)return ItemStack.EMPTY;}
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return copy;
    }
}
