package br.com.soulbound.companion;

import br.com.soulbound.companion.progression.*;
import br.com.soulbound.companion.identity.*;
import com.mojang.serialization.JsonOps;
import java.util.*;

public final class ProgressionChecks {
    private static int count;
    private static void check(boolean b){count++;if(!b)throw new AssertionError("Progression check "+count);}
    private static long total(ProgressionState p){long sum=p.xp();for(int i=1;i<p.level();i++)sum+=ProgressionState.required(i);return sum;}
    public static void run()throws Exception{
        var initial=ProgressionState.INITIAL;check(initial.level()==1 && initial.xp()==0 && initial.available()==0);
        check(ProgressionState.required(1)==50);check(ProgressionState.required(2)==75);check(ProgressionState.required(3)==110);
        check(initial.gain(49).level()==1);check(initial.gain(50).level()==2 && initial.gain(50).available()==1);
        var multi=initial.gain(235);check(multi.level()==4 && multi.xp()==0 && multi.earnedPoints()==3);
        var capped=initial.gain(Long.MAX_VALUE);check(capped.level()==30 && capped.earnedPoints()==29 && capped.xp()==0);check(capped.gain(100).equals(capped));
        var rewards=initial.reward("combat:one",8);check(rewards.reward("combat:one",1000).equals(rewards));
        rewards=rewards.reward("structure:one",30).reward("trial:one",100);check(total(rewards)==138);
        check(rewards.reward("structure:one",30).reward("trial:one",100).equals(rewards));
        var fractions=initial;for(int i=0;i<100;i++)fractions=fractions.rewardAdjusted("combat:"+i,8,5);check(total(fractions)==840 && fractions.bonusCarry()==0);
        check(ProgressionService.eligibleSpawn("NATURAL",false));check(!ProgressionService.eligibleSpawn("SPAWNER",false));
        check(!ProgressionService.eligibleSpawn("COMMAND",false));check(!ProgressionService.eligibleSpawn("SPAWN_ITEM_USE",false));check(!ProgressionService.eligibleSpawn("NATURAL",true));
        var r=SoulboundRecord.create(UUID.randomUUID(),"Progression").withState(CompanionStatus.ACTIVE,false,"FOLLOW_PASSIVE");
        var legacy=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,r.unlockJunior()).getOrThrow().getAsJsonObject();legacy.remove("progression");
        var migrated=SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,legacy).getOrThrow();check(migrated.progression().equals(initial) && migrated.unlockedForms().contains("JUNIOR"));
        r=r.withProgression(capped);var vital=TalentDefinitions.find("soulbound:vitalidade_ancestral");check(TalentDefinitions.rejection(r,vital)==0);
        r=r.withProgression(r.progression().invest(vital.id(),vital.maximum()));check(r.progression().available()==28);
        check(TalentDefinitions.rejection(r,TalentDefinitions.find("soulbound:golpe_aprimorado"))==4);
        check(TalentDefinitions.rejection(r,null)==8);
        check(TalentDefinitions.rejection(r,new TalentDefinitions.Talent("other","soulbound:drakos",TalentDefinitions.Branch.MAIN,1,1,0,null))==7);
        var fullVital=new ProgressionState(30,0,29,Map.of(vital.id(),5),Set.of());check(TalentDefinitions.rejection(r.withProgression(fullVital),vital)==2);
        var early=r.withProgression(initial.gain(50));check(TalentDefinitions.rejection(early,TalentDefinitions.find("soulbound:regeneracao_espiritual"))==3);
        check(TalentDefinitions.rejection(r,TalentDefinitions.find("soulbound:regeneracao_espiritual"))==5);
        var noPrevious=new ProgressionState(30,0,29,Map.of("soulbound:forca_espiritual",5),Set.of());check(TalentDefinitions.rejection(r.withProgression(noPrevious),TalentDefinitions.find("soulbound:pele_runica"))==6);
        var noFree=new ProgressionState(2,0,1,Map.of(vital.id(),1),Set.of());check(TalentDefinitions.rejection(r.withProgression(noFree),TalentDefinitions.find("soulbound:forca_espiritual"))==1);
        boolean denied=false;try{noFree.invest("soulbound:forca_espiritual",5);}catch(IllegalArgumentException ex){denied=true;}check(denied);
        var ranks=Map.of("soulbound:vitalidade_ancestral",5,"soulbound:forca_espiritual",5,"soulbound:golpe_aprimorado",5,"soulbound:instinto_predador",3,"soulbound:ruptura_potencializada",3,"soulbound:sintonia_de_combate",3,"soulbound:pele_runica",3,"soulbound:furia_ancestral",1);
        r=r.unlockJunior().withProgression(new ProgressionState(30,0,29,ranks,Set.of("structure:kept")));
        r=r.withForms(r.forms().select("JUNIOR",100));var profile=TalentEffects.profile(r);
        check(Math.abs(profile.health()-1.2)<.00001);check(Math.abs(profile.damage()-1.15)<.00001);check(Math.abs(profile.basic()-1.2)<.00001);
        check(Math.abs(profile.ultimate()-1.24)<.00001);check(Math.abs(profile.attackSpeed()-1.09)<.00001);check(Math.abs(profile.reduction()-.09)<.00001);
        check(profile.ultimateCooldown()==1056);check(TalentEffects.profile(r).equals(profile));
        var baby=r.withForms(r.forms().select("BABY",200));check(TalentEffects.profile(baby).damage()==1 && TalentEffects.profile(baby).basic()==1 && TalentEffects.profile(baby).fury()==0);
        check(TalentEffects.profile(baby).health()==profile.health());
        var reset=r.withProgression(r.progression().reset());check(reset.progression().available()==29 && reset.progression().spent()==0);
        check(reset.progression().level()==r.progression().level() && reset.progression().xp()==r.progression().xp());
        check(reset.forms().equals(r.forms()) && reset.matches(r.ownerUuid(),r.bondUuid(),r.companionUuid()));check(reset.progression().rewards().equals(r.progression().rewards()));
        check(reset.progression().reset().equals(reset.progression()));check(TalentEffects.profile(reset).health()==1);
        check(r.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath().progression().equals(r.progression()));
        check(r.withName("NewName").withDelivery(true,false).withRecall(r.recall()).progression().equals(r.progression()));
        check(r.withEvolution("JUNIOR",7,999).progression().equals(r.progression()));
        var record=r.withProgression(r.progression().fury(500,900));
        var root=new net.minecraft.nbt.CompoundTag();root.put("record",SoulboundRecord.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,record).getOrThrow());
        var file=java.nio.file.Files.createTempFile("soulbound-progression-",".dat");file.toFile().deleteOnExit();net.minecraft.nbt.NbtIo.writeCompressed(root,file);
        var copy=SoulboundRecord.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).get("record")).getOrThrow();check(copy.equals(record));
        var ledger=new br.com.soulbound.ritual.AltarSavedData();var forge=new br.com.soulbound.ritual.AltarSavedData.Forge();forge.id="reset";ledger.state.forges.put(forge.id,forge);
        var token=br.com.soulbound.ritual.AltarTokens.issue(ledger,record.ownerUuid().toString(),"talent_reset");token.spent=true;token.applicationPending=true;token.applicationOwner=record.ownerUuid().toString();token.applicationBond=record.bondUuid().toString();
        var stored=br.com.soulbound.ritual.AltarSavedData.CODEC.parse(JsonOps.INSTANCE,br.com.soulbound.ritual.AltarSavedData.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow()).getOrThrow();
        check(stored.state.tokens.get(token.id).applicationPending && stored.state.tokens.get(token.id).spent);
        check(br.com.soulbound.ritual.TalentResetService.LEVELS==10 && br.com.soulbound.ritual.TalentResetService.AMETHYST==8 && br.com.soulbound.ritual.TalentResetService.DURATION==200);
        for(String lang:new String[]{"pt_br","en_us"})try(var stream=ProgressionChecks.class.getResourceAsStream("/assets/soulbound/lang/"+lang+".json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var talent:TalentDefinitions.ALL)check(json.has(talent.nameKey()) && json.has(talent.nameKey()+".description"));
        }
        System.out.println("Progression checks passed: "+count);
    }
}
