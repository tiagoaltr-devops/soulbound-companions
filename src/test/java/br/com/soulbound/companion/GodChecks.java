package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.*;
import br.com.soulbound.network.*;
import br.com.soulbound.ritual.*;
import com.mojang.serialization.JsonOps;
import java.util.*;

/** Pure policies, compressed disk persistence, HUD wire data. No gameplay simulation. */
public final class GodChecks {
    private static int count;
    private static void check(boolean value){count++;if(!value)throw new AssertionError("God check "+count);}
    public static void run()throws Exception{
        count=0;check(GodRules.COOLDOWN==6000);var r=SoulboundRecord.create(UUID.randomUUID(),"Prime").withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT").unlockJunior().unlockPleno();
        r=r.withForms(r.forms().select("PLENO",0)).withProgression(new ProgressionState(24,10,23,Map.of(),Set.of()));
        check(!GodRules.canUnlock(r));r=r.withProgression(new ProgressionState(25,15,24,Map.of(),Set.of()));check(GodRules.canUnlock(r));
        check(!GodRules.canUnlock(r.withState(CompanionStatus.DEAD,true,"STAY")));
        check(!GodRules.canUnlock(SoulboundRecord.create(UUID.randomUUID(),"Baby").withProgression(r.progression())));
        check(br.com.soulbound.Config.DIVINE_XP.getDefault()==60);check(br.com.soulbound.Config.DIVINE_XP.getDefault()+br.com.soulbound.Config.RITUAL_VISITOR_XP.getDefault()==65);check(GodRules.RITUAL==900);
        check(GodRules.core(r,r.ownerUuid().toString(),r.bondUuid().toString(),false));
        check(!GodRules.core(r,UUID.randomUUID().toString(),r.bondUuid().toString(),false));
        check(!GodRules.core(r,r.ownerUuid().toString(),UUID.randomUUID().toString(),false));check(!GodRules.core(r,r.ownerUuid().toString(),r.bondUuid().toString(),true));
        var unlocked=r.withForms(r.forms().unlockGod());check(unlocked.activeForm().equals("PLENO"));check(!GodRules.canUnlock(unlocked));
        check(unlocked.matches(r.ownerUuid(),r.bondUuid(),r.companionUuid()) && unlocked.progression().equals(r.progression()));
        check(GodRules.canActivate(unlocked,100));check(!GodRules.canActivate(unlocked.withForms(unlocked.forms().select("BABY",100)),200));
        check(!GodRules.canActivate(unlocked.withEvolution("PLENO",0,500),499));
        var forms=unlocked.forms().ultimate(999).plenoUltimate(888).secondary(true,777).secondary(false,666).activateGod(100,20.16f);
        var god=unlocked.withEvolution("PLENO",12,6100).withForms(forms);check(god.forms().god().end()==1300 && god.godCooldownEnd()==6100);
        check(!GodRules.canActivate(god,200));check(!god.forms().canSelect("BABY",500));
        check(god.withRecall(god.recall()).forms().equals(god.forms()));check(god.withName("New").forms().equals(god.forms()));
        var depleted=god.withForms(god.forms().withGod(god.forms().god().absorb(7)));check(Math.abs(depleted.forms().god().shield()-13.16f)<.001);
        check(depleted.forms().god().absorb(100).shield()==0);check(depleted.forms().god().end()==god.forms().god().end());
        var ended=depleted.withForms(depleted.forms().endGod());check(ended.activeForm().equals("PLENO") && ended.forms().god().shield()==0 && ended.forms().god().maximum()==0);
        check(!GodRules.canActivate(ended,6099));check(GodRules.canActivate(ended,6100));
        var cds=god.withForms(god.forms().withGod(god.forms().god().ultimate(2500).secondary(true,300).secondary(false,400)));
        check(cds.forms().activeUltimateEnd()==2500 && cds.forms().ultimateEnd()==999 && cds.forms().plenoUltimateEnd()==888);
        check(cds.forms().chargeEnd()==777 && cds.forms().quakeEnd()==666 && cds.forms().god().chargeEnd()==300);
        var recovered=cds.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath();
        check(recovered.activeForm().equals("BABY") && recovered.unlockedForms().containsAll(Set.of("BABY","JUNIOR","PLENO","GOD")));
        check(recovered.forms().god().shield()==0 && recovered.forms().god().end()==0 && recovered.forms().god().ultimateEnd()==2500 && recovered.godCooldownEnd()==6100);
        check(recovered.progression().equals(cds.progression()) && recovered.matches(cds.ownerUuid(),cds.bondUuid(),cds.companionUuid()));
        check(Math.abs(GodRules.HEALTH-67.2)<.00001 && Math.abs(GodRules.REACH-PlenoRules.REACH*1.35)<.00001);
        float health=12;for(int i=0;i<20;i++){health=CompanionEvolution.proportionalHealth(health,33.6,67.2);health=CompanionEvolution.proportionalHealth(health,67.2,33.6);}check(Math.abs(health-12)<.001);
        check(GodRules.factor(10,0)==1 && GodRules.factor(11,0)==1.5 && GodRules.factor(12,0)==2.25);
        check(GodRules.factor(13,0)==2 && GodRules.factor(14,0)==1.5 && GodRules.factor(16,0)==4.5);
        check(GodRules.CHARGE==200 && GodRules.RUPTURE==300 && GodRules.ULTIMATE==2400 && GodRules.PREPARATION==40);
        check(JuniorCombat.enabled("GOD",BehaviorMode.FOLLOW_COMBAT,false));check(!JuniorCombat.enabled("GOD",BehaviorMode.STAY,false));check(!JuniorCombat.enabled("GOD",BehaviorMode.FOLLOW_PASSIVE,false));
        check(!JuniorCombat.targetAllowed(true,true,false,false,true,false));check(!JuniorCombat.targetAllowed(true,false,true,false,true,false));
        var trained=god.withProgression(new ProgressionState(30,0,29,Map.of("soulbound:poder_primordial",3,"soulbound:essencia_eterna",3,"soulbound:vitalidade_ancestral",3),Set.of()));
        check(TalentEffects.profile(trained).primordial()==3 && Math.abs(TalentEffects.profile(trained).reduction()-.09)<.0001);
        check(TalentEffects.profile(trained.withForms(trained.forms().endGod())).primordial()==0);
        check(trained.withProgression(trained.progression().reset()).forms().equals(trained.forms()) && trained.progression().reset().available()==29);
        for(var talent:TalentDefinitions.ALL)if(talent.branch()==TalentDefinitions.Branch.GOD){check(talent.maximum()==3);check(TalentDefinitions.rejection(r,talent)!=0);}
        var json=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,cds).getOrThrow();check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow().equals(cds));
        var legacy=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,r).getOrThrow().getAsJsonObject();legacy.getAsJsonObject("forms").remove("god");check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,legacy).getOrThrow().forms().god().equals(GodState.EMPTY));
        var tag=new net.minecraft.nbt.CompoundTag();tag.put("record",SoulboundRecord.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,cds).getOrThrow());
        var file=java.nio.file.Files.createTempFile("soulbound-god-",".dat");file.toFile().deleteOnExit();net.minecraft.nbt.NbtIo.writeCompressed(tag,file);
        check(SoulboundRecord.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).get("record")).getOrThrow().equals(cds));
        var hud=CompanionHudData.project(cds,"Lupus",true,30,67.2f).god(cds,100);check(hud.godSeconds()==60 && hud.godCooldownSeconds()==300 && hud.shield()==20.16f);
        check(CompanionHudData.project(ended,"Lupus",true,15,33.6f).god(ended,1300).shield()==0);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());try{CompanionHudPayload.CODEC.encode(buffer,new CompanionHudPayload(hud));check(CompanionHudPayload.CODEC.decode(buffer).data().equals(hud));}finally{buffer.release();}
        System.out.println("God checks passed: "+count);
    }
}
