package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Nutritional healing only: never calls finishUsingItem or applies food's special effects. */
public final class CompanionFood {
    private CompanionFood(){}
    public static float healing(ItemStack stack){var food=stack.get(DataComponents.FOOD);return food!=null && Float.isFinite(food.saturation()) && food.saturation()>0?food.saturation():0;}
    private static ItemStack remainder(ItemStack stack){var value=stack.get(DataComponents.USE_REMAINDER);return value==null?ItemStack.EMPTY:value.convertInto().copy();}
    private static boolean room(ServerPlayer p,ItemStack item){
        if(item.isEmpty())return true;int remaining=item.getCount();for(int i=0;i<36;i++){var current=p.getInventory().getItem(i);if(current.isEmpty())remaining-=item.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(current,item))remaining-=Math.max(0,current.getMaxStackSize()-current.getCount());if(remaining<=0)return true;}return false;
    }
    public static boolean manual(LupusCompanionEntity c,ServerPlayer player,InteractionHand hand){
        if(!c.isAlive() || !c.isOwnedBy(player) || !SoulboundIdentity.isCanonical(c) || c.getHealth()>=c.getMaxHealth() || c.actionLocksMovement() || c.isRitualizing())return false;
        var stack=player.getItemInHand(hand);float heal=healing(stack);if(heal<=0)return false;var output=remainder(stack);
        if(stack.getCount()>1 && !room(player,output))return false;
        float before=c.getHealth();c.heal(heal);if(c.getHealth()<=before)return false;
        stack.shrink(1);if(!output.isEmpty()){if(stack.isEmpty())player.setItemInHand(hand,output);else player.getInventory().add(output);}
        player.containerMenu.broadcastChanges();br.com.soulbound.network.CompanionHudSync.refresh(player);return true;
    }
    public static int choose(List<ItemStack> stacks,float missing){
        int best=-1;float score=Float.POSITIVE_INFINITY;for(int i=0;i<stacks.size();i++){float hp=healing(stacks.get(i));if(hp<=0)continue;float cost=hp<=missing?missing-hp:100000+(hp-missing);if(cost<score){best=i;score=cost;}}return best;
    }
    public static boolean automatic(LupusCompanionEntity c){
        if(!c.isAlive() || c.getHealth()>=c.getMaxHealth() || !SoulboundIdentity.isCanonical(c) || CompanionFormService.busy(c))return false;
        var level=(ServerLevel)c.level();var server=level.getServer();var data=SoulboundSavedData.get(server);var r=data.find(c.getOwnerUuid());long now=server.overworld().getGameTime();
        if(now<r.survival().foodEnd())return false;
        var foods=new ArrayList<ItemStack>();for(int i=5;i<9;i++)foods.add(AltarInventory.decode(level,r.survival().slots().get(i)));
        int chosen=choose(foods,c.getMaxHealth()-c.getHealth());if(chosen<0)return false;
        var stack=foods.get(chosen);var output=remainder(stack);var owner=server.getPlayerList().getPlayer(r.ownerUuid());
        if(!output.isEmpty() && (owner==null || !room(owner,output)))return false;
        float amount=healing(stack);stack.shrink(1);var slots=new ArrayList<>(r.survival().slots());slots.set(5+chosen,AltarInventory.encode(level,stack));
        data.put(r.withSurvival(r.survival().contents(slots).fed(now+100)));data.flush(server);
        float before=c.getHealth();c.heal(amount);
        if(c.getHealth()<=before){var current=data.find(c.getOwnerUuid());var restored=new ArrayList<>(current.survival().slots());restored.set(5+chosen,r.survival().slots().get(5+chosen));data.put(current.withSurvival(current.survival().contents(restored).fed(r.survival().foodEnd())));data.flush(server);return false;}
        if(!output.isEmpty())owner.getInventory().add(output);
        if(owner!=null){owner.containerMenu.broadcastChanges();br.com.soulbound.network.CompanionHudSync.refresh(owner);}return true;
    }
}
