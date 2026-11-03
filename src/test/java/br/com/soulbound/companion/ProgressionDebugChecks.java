package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.*;
import br.com.soulbound.network.CompanionHudData;
import com.mojang.serialization.JsonOps;
import java.util.*;

/** Small rule/codec checks, not simulated server commands, networking or combat events. */
public final class ProgressionDebugChecks {
    private static int count;
    private static void check(boolean value){count++;if(!value)throw new AssertionError("Progression debug check "+count);}
    private static void denied(SoulboundRecord r,int level){boolean failed=false;try{ProgressionCommands.level(r,level);}catch(IllegalArgumentException e){failed=true;}check(failed);}
    public static void run(){
        count=0;var owner=UUID.randomUUID();var base=SoulboundRecord.create(owner,"Debug").unlockJunior().withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT");
        var third=base.withProgression(ProgressionCommands.level(base,3));check(third.progression().xp()==0 && third.progression().available()==2);
        var xp=third.progression().gain(8);check(xp.level()==3 && xp.xp()==8);
        xp=xp.gain(102);check(xp.level()==4 && xp.xp()==0 && xp.earnedPoints()==3);
        var fifteen=third.withProgression(ProgressionCommands.level(third,15));check(fifteen.progression().available()==14 && fifteen.progression().xp()==0);
        check(ProgressionCommands.level(fifteen,15).equals(fifteen.progression()));
        check(fifteen.matches(base.ownerUuid(),base.bondUuid(),base.companionUuid()) && fifteen.forms().equals(base.forms()) && fifteen.companionStatus()==base.companionStatus());
        check(PlenoRules.canUnlock(fifteen));check(!PlenoRules.canUnlock(fifteen.withProgression(ProgressionCommands.level(fifteen,14))));
        var invested=fifteen.withProgression(fifteen.progression().invest("soulbound:vitalidade_ancestral",5));
        check(ProgressionCommands.level(invested,15).available()==13);check(ProgressionCommands.level(invested,4).talents().equals(invested.progression().talents()));
        denied(invested,1);denied(invested,0);denied(invested,31);
        var high=fifteen.withProgression(new ProgressionState(15,0,14,Map.of("soulbound:regeneracao_espiritual",1,"soulbound:pele_runica",1,"soulbound:vitalidade_ancestral",3,"soulbound:forca_espiritual",1),Set.of()));
        denied(high,5);check(ProgressionCommands.level(high,15).talents().equals(high.progression().talents()));
        var maximum=third.progression().gain(Long.MAX_VALUE);check(maximum.level()==30 && maximum.xp()==0 && maximum.earnedPoints()==29);check(maximum.gain(8).equals(maximum));
        for(String reason:new String[]{"NATURAL","CHUNK_GENERATION","STRUCTURE","PATROL","REINFORCEMENT","EVENT"}){
            check(ProgressionService.eligibleSpawn(reason,false,false));check(ProgressionService.eligibleSpawn(reason,false,true));
        }
        for(String reason:new String[]{"COMMAND","SPAWN_ITEM_USE",""}){
            check(!ProgressionService.eligibleSpawn(reason,false,false));check(ProgressionService.eligibleSpawn(reason,false,true));check(!ProgressionService.eligibleSpawn(reason,true,true));
        }
        for(String reason:new String[]{"SPAWNER","TRIAL_SPAWNER","DISPENSER","CONVERSION"})check(!ProgressionService.eligibleSpawn(reason,false,true));
        var rewarded=third.progression().rewardAdjusted("combat:mob",8,0);check(rewarded.xp()==8);check(rewarded.rewardAdjusted("combat:mob",8,0).equals(rewarded));
        for(String form:new String[]{"JUNIOR","PLENO"}){
            var r=fifteen.unlockPleno();r=r.withForms(r.forms().select(form,0));
            var next=r.withProgression(r.progression().reward("combat:"+form,8));
            check(next.progression().xp()==8 && next.activeForm().equals(form));
            var hud=CompanionHudData.project(next,"Lupus",true,12,24);check(hud.xp()==8 && hud.level()==15 && hud.form().equals(form));
            var saved=SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,next).getOrThrow()).getOrThrow();check(saved.equals(next));
        }
        var dead=invested.withState(CompanionStatus.DEAD,true,"STAY");var changed=dead.withProgression(ProgressionCommands.level(dead,16));
        check(changed.companionStatus()==CompanionStatus.DEAD && changed.resurrectionRequired() && changed.forms().equals(dead.forms()));
        System.out.println("Progression debug checks passed: "+count);
    }
}
