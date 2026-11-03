package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.ritual.CompanionEvolution;
import com.mojang.serialization.JsonOps;

public final class SurvivalChecks {
    private static int count;
    private static void check(boolean b){count++;if(!b)throw new AssertionError("Survival check "+count);}
    public static void run()throws Exception{
        var r=SoulboundRecord.create(java.util.UUID.randomUUID(),"Survival").unlockJunior().withEvolution("JUNIOR",73,9000);
        r=r.withForms(r.forms().select("JUNIOR",100).ultimate(2000)).withState(CompanionStatus.DEAD,true,"FOLLOW_COMBAT");
        check(r.canResurrect());check(!r.canSummon());var recovered=r.recoverAfterDeath();
        check(recovered.companionStatus()==CompanionStatus.READY && !recovered.resurrectionRequired());
        check(recovered.activeForm().equals("BABY") && recovered.evolutionStage().equals("JUNIOR"));check(recovered.canSummon());
        check(recovered.matches(r.ownerUuid(),r.bondUuid(),r.companionUuid()));check(recovered.archetypeId().equals(r.archetypeId()));
        check(recovered.unlockedForms().equals(r.unlockedForms()));check(recovered.evolutionProgress()==73 && recovered.godCooldownEnd()==9000);
        check(recovered.forms().ultimateEnd()==2000);check(recovered.recall().equals(r.recall()));
        check(recovered.forms().canSelect("JUNIOR",2001));check(!recovered.canResurrect());
        for(var status:CompanionStatus.values())if(status!=CompanionStatus.DEAD){
            var denied=r.withState(status,true,"STAY");check(!denied.canResurrect());boolean rejected=false;
            try{denied.recoverAfterDeath();}catch(IllegalStateException e){rejected=true;}check(rejected);
        }
        var encoded=SoulboundRecord.CODEC.encodeStart(JsonOps.INSTANCE,recovered).getOrThrow();check(SoulboundRecord.CODEC.parse(JsonOps.INSTANCE,encoded).getOrThrow().equals(recovered));
        var root=new net.minecraft.nbt.CompoundTag();root.put("record",SoulboundRecord.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,recovered).getOrThrow());
        var file=java.nio.file.Files.createTempFile("soulbound-recovery-",".dat");file.toFile().deleteOnExit();net.minecraft.nbt.NbtIo.writeCompressed(root,file);
        var copy=SoulboundRecord.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.unlimitedHeap()).get("record")).getOrThrow();check(copy.equals(recovered));
        check(CompanionEvolution.proportionalHealth(6,12,24)==12);check(CompanionEvolution.proportionalHealth(12,24,12)==6);
        float health=6;for(int i=0;i<20;i++){health=CompanionEvolution.proportionalHealth(health,12,24);health=CompanionEvolution.proportionalHealth(health,24,12);}check(health==6);
        check(CompanionEvolution.proportionalHealth(30,24,12)==12);check(CompanionEvolution.proportionalHealth(0,12,24)==0);
        System.out.println("Survival checks passed: "+count);
    }
}
