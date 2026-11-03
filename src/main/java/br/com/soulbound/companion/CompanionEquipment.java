package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.AltarInventory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

/** Native slots are working copies. Only this bridge commits native wear back to the bond. */
public final class CompanionEquipment {
    public static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND};
    private static final Map<LupusCompanionEntity,List<String>> PROJECTED=new WeakHashMap<>();
    private static final Map<LupusCompanionEntity,ItemStack[]> LAST=new WeakHashMap<>();
    private CompanionEquipment(){}
    /** Vanilla reads Health before native equipment modifiers. Restore the same saved Health after HP modifiers exist. */
    public static void restoreHealthOnLoad(LupusCompanionEntity c,float savedHealth){
        if(!(c.level() instanceof ServerLevel level) || c.getOwnerUuid()==null)return;
        var r=SoulboundSavedData.get(level.getServer()).find(c.getOwnerUuid());if(r==null || !r.matches(c.getOwnerUuid(),c.getBondUuid(),c.getUUID()))return;
        apply(c,r);
        for(var slot:SLOTS)c.getItemBySlot(slot).forEachModifier(slot,(attribute,modifier)->{if(attribute.equals(Attributes.MAX_HEALTH)){var value=c.getAttribute(attribute);value.removeModifier(modifier.id());value.addTransientModifier(modifier);}});
        if(Float.isFinite(savedHealth))c.setHealth(Math.max(0,savedHealth));
    }
    public static boolean allowed(int slot,ItemStack stack,EntityType<?> type){
        if(stack.isEmpty())return true;if(slot<0 || slot>8)return false;
        if(slot>=5)return CompanionFood.healing(stack)>0;
        if(slot<4){var equippable=stack.get(DataComponents.EQUIPPABLE);return equippable!=null && equippable.slot()==SLOTS[slot] && equippable.canBeEquippedBy(type);}
        if(stack.has(DataComponents.WEAPON))return true;
        boolean[] weapon={false};stack.forEachModifier(EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(Attributes.ATTACK_DAMAGE))weapon[0]=true;});return weapon[0];
    }
    public static void apply(LupusCompanionEntity c,SoulboundRecord r){
        if(!(c.level() instanceof ServerLevel level))return;
        var wanted=r.survival().slots().subList(0,5);var projected=PROJECTED.get(c);
        if(wanted.equals(projected))return;
        for(int i=0;i<5;i++){
            if(projected!=null && wanted.get(i).equals(projected.get(i)))continue;
            var stack=AltarInventory.decode(level,wanted.get(i));if(!allowed(i,stack,c.getType()) || stack.getCount()>1)throw new IllegalStateException("Invalid companion equipment slot "+i);
            c.getItemBySlot(SLOTS[i]).forEachModifier(SLOTS[i],(attribute,modifier)->{if(attribute.equals(Attributes.MAX_HEALTH))c.getAttribute(attribute).removeModifier(modifier.id());});
            c.setItemSlot(SLOTS[i],stack.copy());c.setDropChance(SLOTS[i],0);
        }
        c.setCanPickUpLoot(false);PROJECTED.put(c,List.copyOf(wanted));remember(c);
    }
    private static void remember(LupusCompanionEntity c){var items=new ItemStack[5];for(int i=0;i<5;i++)items[i]=c.getItemBySlot(SLOTS[i]).copy();LAST.put(c,items);}
    public static void capture(LupusCompanionEntity c){
        if(!(c.level() instanceof ServerLevel level) || c.getOwnerUuid()==null)return;
        var projected=PROJECTED.get(c);if(projected==null)return;
        var data=SoulboundSavedData.get(level.getServer());var r=data.find(c.getOwnerUuid());
        if(r==null || !r.matches(c.getOwnerUuid(),c.getBondUuid(),c.getUUID()))return;
        if(!r.survival().slots().subList(0,5).equals(projected)){apply(c,r);return;}
        var next=new ArrayList<>(r.survival().slots());boolean changed=false,nativeChanged=false;var last=LAST.get(c);
        for(int i=0;i<5;i++){var item=c.getItemBySlot(SLOTS[i]);if(last!=null && ItemStack.matches(item,last[i]))continue;nativeChanged=true;String value=AltarInventory.encode(level,item);if(!value.equals(projected.get(i))){next.set(i,value);changed=true;}}
        if(changed){data.put(r.withSurvival(r.survival().contents(next)));PROJECTED.put(c,List.copyOf(next.subList(0,5)));}if(nativeChanged)remember(c);
    }
}
