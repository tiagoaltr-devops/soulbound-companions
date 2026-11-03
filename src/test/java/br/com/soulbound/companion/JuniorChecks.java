package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import com.mojang.serialization.JsonOps;
import java.util.UUID;

/** Pure canonical rules and real compressed-NBT round trips; gameplay is a separate manual check. */
public final class JuniorChecks {
    private static int count;
    private static void check(boolean b){count++;if(!b)throw new AssertionError("Junior check "+count);}
    public static int run()throws Exception{
        count=0;var owner=UUID.randomUUID();var ready=SoulboundRecord.create(owner,"Owner");
        var record=ready.withState(CompanionStatus.ACTIVE,false,"STAY");
        for(String stage:new String[]{"BABY","JUNIOR","PLENO"}){
            var legacy=record.withEvolution(stage,42,900);
            var json=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,legacy).getOrThrow().getAsJsonObject();json.remove("forms");
            var migrated=SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
            check(migrated.archetypeId().equals("soulbound:lupus"));check(migrated.activeForm().equals(stage));
            check(migrated.evolutionStage().equals(stage) && migrated.evolutionProgress()==42 && migrated.godCooldownEnd()==900);
            check(migrated.matches(record.ownerUuid(),record.bondUuid(),record.companionUuid()));
        }
        check(JuniorCorePolicy.accepts(record,owner.toString(),record.bondUuid().toString(),false));
        check(!JuniorCorePolicy.accepts(record,"other",record.bondUuid().toString(),false));
        check(!JuniorCorePolicy.accepts(record,owner.toString(),UUID.randomUUID().toString(),false));
        check(!JuniorCorePolicy.accepts(record,owner.toString(),record.bondUuid().toString(),true));
        for(var status:CompanionStatus.values())if(status!=CompanionStatus.ACTIVE)check(!JuniorCorePolicy.accepts(record.withState(status,false,"STAY"),owner.toString(),record.bondUuid().toString(),false));
        var unlocked=record.unlockJunior();check(unlocked.evolutionStage().equals("JUNIOR"));check(unlocked.activeForm().equals("BABY"));
        check(unlocked.unlockedForms().containsAll(java.util.Set.of("BABY","JUNIOR")));
        check(!JuniorCorePolicy.accepts(unlocked,owner.toString(),record.bondUuid().toString(),false));
        check(!unlocked.forms().canSelect("PLENO",100));check(!unlocked.forms().canSelect("GOD",100));
        var junior=unlocked.withForms(unlocked.forms().select("JUNIOR",100));
        check(junior.matches(record.ownerUuid(),record.bondUuid(),record.companionUuid()));check(junior.behaviorMode().equals("STAY"));
        check(!junior.forms().canSelect("BABY",159));check(junior.forms().canSelect("BABY",160));
        check(junior.withName("Renamed").forms().equals(junior.forms()));check(junior.withDelivery(true,false).forms().equals(junior.forms()));
        check(junior.withRecall(junior.recall()).forms().equals(junior.forms()));check(junior.withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT").forms().equals(junior.forms()));
        check(junior.withEvolution("JUNIOR",55,999).forms().equals(junior.forms()));
        check(!JuniorCombat.enabled("BABY",BehaviorMode.FOLLOW_COMBAT,false));
        check(JuniorCombat.enabled("JUNIOR",BehaviorMode.FOLLOW_COMBAT,false));
        check(!JuniorCombat.enabled("JUNIOR",BehaviorMode.STAY,false));check(!JuniorCombat.enabled("JUNIOR",BehaviorMode.FOLLOW_PASSIVE,false));
        check(!JuniorCombat.enabled("JUNIOR",BehaviorMode.FOLLOW_COMBAT,true));
        check(JuniorCombat.targetAllowed(true,false,false,false,true,false));
        check(!JuniorCombat.targetAllowed(true,true,false,false,true,false));check(!JuniorCombat.targetAllowed(false,false,false,false,true,false));
        check(!JuniorCombat.targetAllowed(true,false,true,false,true,false));check(!JuniorCombat.targetAllowed(true,false,false,true,true,false));
        check(!JuniorCombat.targetAllowed(true,false,false,false,false,false));check(!JuniorCombat.targetAllowed(true,false,false,false,true,true));
        check(junior.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,true,200));
        check(!junior.forms().canUltimate(BehaviorMode.STAY,true,200));check(!junior.forms().canUltimate(BehaviorMode.FOLLOW_PASSIVE,true,200));
        check(!junior.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,false,200));
        junior=junior.withForms(junior.forms().ultimate(1400));
        check(!junior.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,true,200));check(!junior.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,true,1399));
        check(junior.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,true,1400));
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_COMBAT,true,false,0,0)==0);
        check(JuniorCombat.ultimateState("BABY",BehaviorMode.FOLLOW_COMBAT,true,false,0,0)==1);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.STAY,true,false,0,0)==2);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_PASSIVE,true,false,0,0)==3);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_COMBAT,false,false,0,0)==4);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_COMBAT,true,true,0,0)==5);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_COMBAT,true,false,1200,1199)==6);
        check(JuniorCombat.ultimateState("JUNIOR",BehaviorMode.FOLLOW_COMBAT,true,false,1200,1200)==0);
        var baby=junior.withForms(junior.forms().select("BABY",200));check(baby.forms().ultimateEnd()==1400);
        check(!baby.forms().canUltimate(BehaviorMode.FOLLOW_COMBAT,true,2000));
        var root=new net.minecraft.nbt.CompoundTag();root.put("record",SoulboundRecord.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,junior).getOrThrow());
        var file=java.nio.file.Files.createTempFile("soulbound-junior-",".dat");file.toFile().deleteOnExit();net.minecraft.nbt.NbtIo.writeCompressed(root,file);
        var copy=SoulboundRecord.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).get("record")).getOrThrow();check(copy.equals(junior));
        check(CompanionArchetype.form("BABY").movement()==CompanionArchetype.Movement.FLYING);
        check(CompanionArchetype.form("JUNIOR").movement()==CompanionArchetype.Movement.GROUND);
        try(var stream=JuniorChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_junior.png")){
            var image=javax.imageio.ImageIO.read(stream);check(image!=null && image.getWidth()==128 && image.getHeight()==128);
        }
        System.out.println("Junior checks passed: "+count);return count;
    }
}
