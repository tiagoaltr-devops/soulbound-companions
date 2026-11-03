package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.ProgressionService;
import com.mojang.serialization.JsonOps;
import java.util.*;

public final class Phase69Checks {
    private static int count;
    private static void check(boolean b){count++;if(!b)throw new AssertionError("Phase69 check "+count);}
    public static void run()throws Exception{
        count=0;for(var form:List.of("BABY","JUNIOR","PLENO"))check(SurvivalRules.health(form)==33.6);
        check(SurvivalRules.health("GOD")==67.2);
        check(SurvivalRules.healthAfter(10,33.6,40,false)==10);check(SurvivalRules.healthAfter(35,40,33.6,false)==33.6f);
        check(SurvivalRules.endGod(20,2,53.6)==10);check(SurvivalRules.endGod(10,2,33.6)==5);check(SurvivalRules.endGod(30,2,33.6)==15);
        float hp=10;for(int i=0;i<30;i++){hp=SurvivalRules.healthAfter(hp,33.6,67.2,true);hp=SurvivalRules.healthAfter(hp,67.2,33.6,true);}check(Math.abs(hp-10)<.001);
        check(SurvivalRules.regeneration("BABY",0)==.5f);check(SurvivalRules.regeneration("JUNIOR",0)==1);check(SurvivalRules.regeneration("PLENO",0)==1.5f);check(SurvivalRules.regeneration("GOD",0)==2);
        check(Math.abs(SurvivalRules.regeneration("BABY",3)-1.1f)<.001);
        check(ProgressionService.recent(0,295));check(!ProgressionService.recent(0,315));check(!ProgressionService.recent(0,301));
        var r=SoulboundRecord.create(UUID.randomUUID(),"Survival");var state=SurvivalState.EMPTY.combat(160).fed(100).regenerated(40).defense(true);
        r=r.withSurvival(state);check(r.survival().slots().size()==9);check(r.withName("New").survival().equals(state));check(r.withDelivery(true,false).survival().equals(state));
        check(r.unlockJunior().unlockPleno().withForms(r.forms()).survival().equals(state));
        check(r.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath().survival().slots().equals(state.slots()));
        var json=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,r).getOrThrow().getAsJsonObject();check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().equals(r));
        json.remove("survival");check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().survival().equals(SurvivalState.EMPTY));
        for(String stage:List.of("BABY","JUNIOR","PLENO")){
            var legacy=SoulboundRecord.create(UUID.randomUUID(),"Legacy");if(!stage.equals("BABY"))legacy=legacy.unlockJunior();if(stage.equals("PLENO"))legacy=legacy.unlockPleno();
            var old=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,legacy).getOrThrow().getAsJsonObject();
            old.remove("survival");old.remove("forms");old.remove("progression");
            var loaded=SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();
            check(loaded.companionUuid().equals(legacy.companionUuid()) && loaded.bondUuid().equals(legacy.bondUuid()));
            check(loaded.activeForm().equals(stage) && loaded.survival().equals(SurvivalState.EMPTY));
        }
        check(SurvivalRules.defensive(false,50,100));check(SurvivalRules.defensive(true,64,100));check(!SurvivalRules.defensive(true,65,100));

        System.out.println("Phase 6.9 checks passed: "+count);
    }
    public static void nativeEquipment(net.minecraft.core.HolderLookup.Provider lookup)throws Exception{
        count=0;

        var ops=lookup.createSerializationContext(JsonOps.INSTANCE);
        var chicken=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_CHICKEN);
        var beef=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF);
        var carrot=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_CARROT);
        check(Math.abs(CompanionFood.healing(chicken)-7.2)<.001);check(Math.abs(CompanionFood.healing(beef)-12.8)<.001);check(Math.abs(CompanionFood.healing(carrot)-14.4)<.001);
        check(Math.abs(CompanionFood.healing(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLDEN_APPLE))-9.6)<.001);
        check(CompanionFood.choose(List.of(chicken,beef,carrot),10)==0);check(CompanionFood.choose(List.of(chicken,beef,carrot),14)==1);
        var sword=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        var helmet=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET);
        check(CompanionEquipment.allowed(0,helmet,net.minecraft.world.entity.EntityType.ZOMBIE));check(!CompanionEquipment.allowed(1,helmet,net.minecraft.world.entity.EntityType.ZOMBIE));
        check(CompanionEquipment.allowed(4,sword,net.minecraft.world.entity.EntityType.ZOMBIE));check(!CompanionEquipment.allowed(4,chicken,net.minecraft.world.entity.EntityType.ZOMBIE));
        for(int i=5;i<9;i++){check(CompanionEquipment.allowed(i,beef,net.minecraft.world.entity.EntityType.ZOMBIE));check(!CompanionEquipment.allowed(i,sword,net.minecraft.world.entity.EntityType.ZOMBIE));}
        helmet.enchant(lookup.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),2);
        helmet.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Preserved"));
        var encoded=net.minecraft.world.item.ItemStack.CODEC.encodeStart(ops,helmet).getOrThrow();
        check(net.minecraft.world.item.ItemStack.matches(helmet,net.minecraft.world.item.ItemStack.CODEC.parse(ops,encoded).getOrThrow()));
        var record=SoulboundRecord.create(UUID.randomUUID(),"Gear");var slots=new ArrayList<>(SurvivalState.EMPTY.slots());slots.set(0,encoded.toString());
        record=record.withSurvival(record.survival().contents(slots).fed(100));
        var recovered=record.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath();check(recovered.survival().equals(record.survival()));
        check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,record).getOrThrow()).getOrThrow().equals(record));
        var attack=new net.minecraft.world.entity.ai.attributes.AttributeInstance(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,a->{});attack.setBaseValue(4);
        for(int i=0;i<2;i++)sword.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)){attack.removeModifier(modifier.id());attack.addTransientModifier(modifier);}});
        double value=attack.getValue();check(value>4);sword.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND,(attribute,modifier)->{if(attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))attack.removeModifier(modifier.id());});check(attack.getValue()==4);
        System.out.println("Phase 6.9 native item checks passed: "+count);
    }
}
