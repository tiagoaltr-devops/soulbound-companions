package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.progression.*;
import com.mojang.serialization.JsonOps;
import java.util.*;

/** Canonical policy and persistence checks; does not simulate combat or rendering. */
public final class PlenoChecks {
    private static int count;
    private static void check(boolean value){if(!value)throw new AssertionError("Pleno check "+count);count++;}
    public static void run()throws Exception{
        count=0;
        var base=SoulboundRecord.create(UUID.randomUUID(),"Owner").withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT").unlockJunior();
        check(!PlenoRules.canUnlock(base));
        var junior=base.withProgression(new ProgressionState(15,12,14,Map.of(),Set.of()));
        check(PlenoRules.canUnlock(junior));
        check(!PlenoRules.canUnlock(junior.withState(CompanionStatus.DEAD,true,"STAY")));
        var unlocked=junior.unlockPleno();check(unlocked.activeForm().equals("BABY"));check(!PlenoRules.canUnlock(unlocked));
        check(unlocked.matches(base.ownerUuid(),base.bondUuid(),base.companionUuid()));
        check(unlocked.progression().equals(junior.progression()));check(unlocked.recall().equals(junior.recall()));
        var forms=unlocked.forms().ultimate(999).plenoUltimate(1800).secondary(true,240).secondary(false,360).select("PLENO",0);
        check(forms.activeUltimateEnd()==1800);check(forms.ultimateEnd()==999);
        check(forms.chargeEnd()==240 && forms.quakeEnd()==360);
        check(forms.select("JUNIOR",60).activeUltimateEnd()==999);
        check(forms.recoverBaby().plenoUltimateEnd()==1800 && forms.recoverBaby().chargeEnd()==240);
        var record=unlocked.withForms(forms);var copy=SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,record).getOrThrow()).getOrThrow();
        check(record.equals(copy));check(copy.withName("Renamed").forms().equals(forms));
        check(copy.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath().forms().plenoUltimateEnd()==1800);
        var old=CompanionForms.CODEC.encodeStart(JsonOps.INSTANCE,forms).getOrThrow().getAsJsonObject();
        old.remove("plenoUltimateEnd");old.remove("chargeEnd");old.remove("quakeEnd");
        var legacy=CompanionForms.CODEC.parse(JsonOps.INSTANCE,old).getOrThrow();check(legacy.plenoUltimateEnd()==0 && legacy.ultimateEnd()==999);
        check(Math.abs(PlenoRules.HEALTH-33.6)<.00001);check(Math.abs(PlenoRules.SPEED-.266)<.00001);check(Math.abs(PlenoRules.REACH-2.3)<.00001);
        check(PlenoRules.factor(10,0)==1 && PlenoRules.factor(11,0)==1 && PlenoRules.factor(12,0)==1.5);
        check(PlenoRules.factor(13,0)==1.3 && PlenoRules.factor(14,0)==.8 && PlenoRules.factor(16,0)==2.5);
        check(!PlenoRules.chargeDistance(3.99) && PlenoRules.chargeDistance(4) && PlenoRules.chargeDistance(10) && !PlenoRules.chargeDistance(10.01));
        for(var talent:TalentDefinitions.ALL)if(talent.branch()==TalentDefinitions.Branch.PLENO){
            check(talent.maximum()==3);check(TalentDefinitions.rejection(junior,talent)!=0);
            check(PlenoRules.factor(12,3)>=PlenoRules.factor(12,0));
        }
        var trained=record.withProgression(new ProgressionState(18,0,17,Map.of("soulbound:dominio_ancestral",3,"soulbound:guardiao_inabalavel",2),Set.of()));
        check(TalentEffects.profile(trained).dominance()==3 && TalentEffects.profile(trained).guardian()==2);
        var baby=trained.withForms(forms.select("BABY",60));check(TalentEffects.profile(baby).dominance()==0 && TalentEffects.profile(baby).guardian()==0);
        check(trained.progression().reset().available()==17 && trained.withProgression(trained.progression().reset()).forms().equals(forms));
        check(ProgressionState.MAX_LEVEL==30);
        try(var in=PlenoChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_pleno.png")){check(in!=null);var image=javax.imageio.ImageIO.read(in);check(image.getWidth()==128 && image.getHeight()==128);}
        System.out.println("Pleno checks passed: "+count);
    }
}
