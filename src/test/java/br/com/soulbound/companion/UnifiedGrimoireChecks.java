package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.item.SoulEggIdentity;
import br.com.soulbound.menu.SoulCommandMenu;
import br.com.soulbound.registry.ModItems;
import br.com.soulbound.ritual.AltarInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.*;

/** Real native slots/actions in the one grimoire container; isolated server fixture only. */
public final class UnifiedGrimoireChecks {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError("Unified grimoire: "+message);}
    private static int count(FakePlayer p,ItemStack template){int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++)if(ItemStack.isSameItemSameComponents(template,p.getInventory().getItem(i)))count+=p.getInventory().getItem(i).getCount();return count;}
    public static void run(MinecraftServer server,FakePlayer owner,br.com.soulbound.entity.LupusCompanionEntity c){
        checks=0;var data=SoulboundSavedData.get(server);var original=data.find(owner.getUUID());var originalProgress=original.progression();
        var clean=new ArrayList<>(original.survival().slots());for(int i=5;i<9;i++)clean.set(i,"");data.put(original.withSurvival(original.survival().contents(clean)));
        var key=new ItemStack(ModItems.COMPANION_EGG.get());SoulEggIdentity.refresh(key,original);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,key);
        c.setPos(3,140,3);owner.setPos(2,140,3);var previous=owner.containerMenu;
        var menu=new SoulCommandMenu(31,owner.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND,data.find(owner.getUUID()));owner.containerMenu=menu;
        try{
            check(menu.slots.size()==45,"one native 9+36 slot set");check(!menu.slots.getFirst().isActive(),"non-inventory tabs hide slots");
            check(menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+1) && owner.containerMenu==menu && menu.containerId==31,"inventory selected without replacing menu");
            check(menu.slots.stream().allMatch(s->s.isActive()),"all real slots active when authorized");
            var food=new ItemStack(Items.COOKED_CHICKEN,5);food.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("UnifiedInventoryFixture"));owner.getInventory().setItem(9,food.copy());
            menu.clicked(9,0,ClickType.QUICK_MOVE,owner);check(AltarInventory.decode(owner.level(),data.find(owner.getUUID()).survival().slots().get(5)).getCount()==5,"integrated shift-click commits canonical food");
            menu.clicked(5,0,ClickType.PICKUP,owner);check(menu.getCarried().getCount()==5 && data.find(owner.getUUID()).survival().slots().get(5).isEmpty(),"native pickup debits once into cursor");
            menu.clicked(-999,0,ClickType.QUICK_CRAFT,owner);menu.clicked(10,1,ClickType.QUICK_CRAFT,owner);
            check(menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+2) && menu.getCarried().getCount()==5,"tab switch cancels quick-craft while preserving cursor");
            menu.clicked(10,2,ClickType.QUICK_CRAFT,owner);menu.clicked(10,0,ClickType.PICKUP,owner);
            check(menu.getCarried().getCount()==5 && !menu.slots.get(10).isActive(),"hidden slots reject forged cursor movement");
            var r=data.find(owner.getUUID());data.put(r.withProgression(br.com.soulbound.companion.progression.ProgressionCommands.level(r,3)));menu.broadcastChanges();
            check(menu.talents.get(2)==2 && menu.talentRank(0)==0,"existing talent sync exposes canonical budget");
            check(menu.clickMenuButton(owner,SoulCommandMenu.TALENT_FIRST),"integrated first talent investment");
            check(menu.talentRank(0)==1 && menu.talents.get(2)==1 && menu.getCarried().getCount()==5,"investment updates same menu without touching cursor");
            check(menu.clickMenuButton(owner,SoulCommandMenu.TALENT_FIRST) && !menu.clickMenuButton(owner,SoulCommandMenu.TALENT_FIRST),"point budget enforced without duplicate free ranks");
            check(data.find(owner.getUUID()).progression().rank("soulbound:vitalidade_ancestral")==2 && data.find(owner.getUUID()).progression().available()==0,"canonical ranks and spent points match");
            check(!menu.clickMenuButton(owner,SoulCommandMenu.TALENT_FIRST+1),"no-budget investment rejected");
            var stranger=FakePlayerFactory.get(owner.level(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"UnifiedOther"));
            check(!menu.clickMenuButton(stranger,SoulCommandMenu.TALENT_FIRST),"foreign talent request rejected");
            menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+1);check(menu.getCarried().getCount()==5,"cursor survives return to inventory");
            menu.clicked(10,0,ClickType.PICKUP,owner);check(menu.getCarried().isEmpty() && count(owner,food)==5,"native cursor deposit conserves quantity/components");
            menu.clicked(10,0,ClickType.QUICK_MOVE,stranger);check(count(owner,food)==5 && menu.quickMoveStack(stranger,10).isEmpty(),"foreign native movement rejected");
            r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());var extra=food.copyWithCount(2);slots.set(5,AltarInventory.encode(owner.level(),extra));data.put(r.withSurvival(r.survival().contents(slots)));
            menu.clicked(5,0,ClickType.QUICK_MOVE,owner);check(count(owner,food)==5,"stale integrated snapshot rejects first click");
            menu.clicked(5,0,ClickType.QUICK_MOVE,owner);check(count(owner,food)==7 && data.find(owner.getUUID()).survival().slots().get(5).isEmpty(),"fresh integrated shift-click withdraws exactly once");
            c.setPos(20,140,3);menu.broadcastChanges();check(menu.stillValid(owner) && !menu.slots.getFirst().isActive(),"remote hub remains open but physical slots deactivate");
            menu.clicked(10,0,ClickType.QUICK_MOVE,owner);check(count(owner,food)==7,"remote physical click denied even with valid hub session");
            menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST);check(!menu.clickMenuButton(owner,SoulCommandMenu.TALENT_FIRST),"investment outside talent tab rejected");
            check(owner.containerMenu==menu && menu.containerId==31,"all navigation kept one container ID");
            c.setPos(3,140,3);menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+1);menu.clicked(36,0,ClickType.PICKUP,owner);
            check(SoulboundIdentity.isEgg(menu.getCarried()) && menu.stillValid(owner),"moving grimoire itself to cursor preserves bound session");
            check(menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+2) && SoulboundIdentity.isEgg(menu.getCarried()),"grimoire cursor survives navigation to talents");
            menu.clickMenuButton(owner,SoulCommandMenu.TAB_FIRST+1);menu.clicked(36,0,ClickType.PICKUP,owner);
            check(menu.getCarried().isEmpty() && SoulEggIdentity.matches(owner.getMainHandItem(),data.find(owner.getUUID())),"same grimoire returns to hotbar without duplication");
            data.put(data.find(owner.getUUID()).withProgression(originalProgress).withSurvival(original.survival()));c.applyIdentity(data.find(owner.getUUID()));data.flush(server);
            check(data.find(owner.getUUID()).bondUuid().equals(original.bondUuid()) && data.find(owner.getUUID()).companionUuid().equals(original.companionUuid()),"integrated UI preserves identity");
        }finally{menu.removed(owner);owner.containerMenu=previous;}
        System.out.println("Unified grimoire native checks passed: "+checks);
    }
}
