package br.com.soulbound.companion.identity;

import java.util.UUID;
import java.util.Optional;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Immutable canonical state. Item components and entity NBT are projections, never the authority. */
public record SoulboundRecord(UUID ownerUuid, String ownerLastKnownName, UUID bondUuid, UUID companionUuid,
        String species, String evolutionStage, String behaviorMode, CompanionStatus companionStatus,
        boolean resurrectionRequired, double evolutionProgress, long godCooldownEnd,
        boolean eggDelivered, boolean eggDeliveryPending, SoulRecallState recall, br.com.soulbound.companion.CompanionForms forms,
        br.com.soulbound.companion.progression.ProgressionState progression,br.com.soulbound.companion.SurvivalState survival) {
    private static final com.mojang.serialization.MapCodec<SoulboundRecord> BASE_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("ownerUuid").forGetter(SoulboundRecord::ownerUuid),
            Codec.STRING.fieldOf("ownerLastKnownName").forGetter(SoulboundRecord::ownerLastKnownName),
            UUIDUtil.CODEC.fieldOf("bondUuid").forGetter(SoulboundRecord::bondUuid),
            UUIDUtil.CODEC.fieldOf("companionUuid").forGetter(SoulboundRecord::companionUuid),
            Codec.STRING.fieldOf("species").forGetter(SoulboundRecord::species),
            Codec.STRING.fieldOf("evolutionStage").forGetter(SoulboundRecord::evolutionStage),
            Codec.STRING.fieldOf("behaviorMode").forGetter(SoulboundRecord::behaviorMode),
            CompanionStatus.CODEC.fieldOf("companionStatus").forGetter(SoulboundRecord::companionStatus),
            Codec.BOOL.fieldOf("resurrectionRequired").forGetter(SoulboundRecord::resurrectionRequired),
            Codec.doubleRange(0.0, Double.MAX_VALUE).fieldOf("evolutionProgress").forGetter(SoulboundRecord::evolutionProgress),
            Codec.LONG.fieldOf("godCooldownEnd").forGetter(SoulboundRecord::godCooldownEnd),
            Codec.BOOL.optionalFieldOf("eggDelivered", false).forGetter(SoulboundRecord::eggDelivered),
            Codec.BOOL.optionalFieldOf("eggDeliveryPending", true).forGetter(SoulboundRecord::eggDeliveryPending),
            SoulRecallState.MAP_CODEC.forGetter(SoulboundRecord::recall),
            br.com.soulbound.companion.CompanionForms.CODEC.optionalFieldOf("forms",br.com.soulbound.companion.CompanionForms.LEGACY).forGetter(SoulboundRecord::forms),
            br.com.soulbound.companion.progression.ProgressionState.CODEC.optionalFieldOf("progression",br.com.soulbound.companion.progression.ProgressionState.INITIAL).forGetter(SoulboundRecord::progression)
    ).apply(instance, SoulboundRecord::new));

    public static final Codec<SoulboundRecord> CODEC=Codec.mapPair(BASE_CODEC,br.com.soulbound.companion.SurvivalState.CODEC.optionalFieldOf("survival",br.com.soulbound.companion.SurvivalState.EMPTY)).xmap(pair->pair.getFirst().withSurvival(pair.getSecond()),r->com.mojang.datafixers.util.Pair.of(r,r.survival())).codec();
    public SoulboundRecord(UUID owner,String name,UUID bond,UUID entity,String species,String stage,String mode,CompanionStatus status,boolean resurrection,double progress,long godCooldown,boolean delivered,boolean pending,SoulRecallState recall,br.com.soulbound.companion.CompanionForms forms,br.com.soulbound.companion.progression.ProgressionState progression){this(owner,name,bond,entity,species,stage,mode,status,resurrection,progress,godCooldown,delivered,pending,recall,forms,progression,br.com.soulbound.companion.SurvivalState.EMPTY);}
    public SoulboundRecord withSurvival(br.com.soulbound.companion.SurvivalState state){return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,evolutionStage,behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,forms,progression,state);}
    public SoulboundRecord {if(forms.unlockedForms().isEmpty())forms=br.com.soulbound.companion.CompanionForms.migrate(evolutionStage);}
    public String archetypeId(){return forms.archetypeId();}
    public java.util.Set<String> unlockedForms(){return forms.unlockedForms();}
    public String activeForm(){return forms.activeForm();}
    public SoulboundRecord withProgression(br.com.soulbound.companion.progression.ProgressionState value){return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,evolutionStage,behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,forms,value,survival);}
    public SoulboundRecord withForms(br.com.soulbound.companion.CompanionForms value){return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,evolutionStage,behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,value,progression,survival);}
    public SoulboundRecord unlockJunior(){return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,"JUNIOR",behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,forms.unlockJunior(),progression,survival);}
    public SoulboundRecord unlockPleno(){return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,"PLENO",behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,forms.unlockPleno(),progression,survival);}
    public SoulboundRecord(UUID ownerUuid,String ownerLastKnownName,UUID bondUuid,UUID companionUuid,String species,String evolutionStage,String behaviorMode,CompanionStatus companionStatus,boolean resurrectionRequired,double evolutionProgress,long godCooldownEnd,boolean eggDelivered,boolean eggDeliveryPending,SoulRecallState recall,br.com.soulbound.companion.CompanionForms forms){
        this(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,evolutionStage,behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,forms,br.com.soulbound.companion.progression.ProgressionState.INITIAL);
    }
    public SoulboundRecord(UUID ownerUuid,String ownerLastKnownName,UUID bondUuid,UUID companionUuid,String species,String evolutionStage,String behaviorMode,CompanionStatus companionStatus,boolean resurrectionRequired,double evolutionProgress,long godCooldownEnd,boolean eggDelivered,boolean eggDeliveryPending,SoulRecallState recall){
        this(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,evolutionStage,behaviorMode,companionStatus,resurrectionRequired,evolutionProgress,godCooldownEnd,eggDelivered,eggDeliveryPending,recall,br.com.soulbound.companion.CompanionForms.migrate(evolutionStage));
    }
    public SoulboundRecord(UUID ownerUuid, String ownerLastKnownName, UUID bondUuid, UUID companionUuid,
            String species, String evolutionStage, String behaviorMode, CompanionStatus companionStatus,
            boolean resurrectionRequired, double evolutionProgress, long godCooldownEnd, boolean eggDelivered, boolean eggDeliveryPending) {
        this(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage, behaviorMode,
                companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, SoulRecallState.EMPTY);
    }

    public Optional<ResourceKey<Level>> lastKnownDimension() { return recall.lastKnownDimension(); }
    public Optional<Vec3> lastKnownPosition() { return recall.lastKnownPosition(); }
    public long lastKnownUpdate() { return recall.lastKnownUpdate(); }
    public long recallCooldownEnd() { return recall.recallCooldownEnd(); }

    public SoulboundRecord withRecall(SoulRecallState state) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage,
                behaviorMode, companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, state,forms,progression,survival);
    }

    public static SoulboundRecord create(UUID owner, String name) {
        return new SoulboundRecord(owner, name, UUID.randomUUID(), UUID.randomUUID(), "LUPUS", "BABY",
                "FOLLOW_PASSIVE", CompanionStatus.READY, false, 0.0, 0L, false, true);
    }

    public SoulboundRecord withName(String name) {
        return new SoulboundRecord(ownerUuid, name, bondUuid, companionUuid, species, evolutionStage, behaviorMode,
                companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall,forms,progression,survival);
    }

    public SoulboundRecord withDelivery(boolean delivered, boolean pending) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage,
                behaviorMode, companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, delivered, pending, recall,forms,progression,survival);
    }

    public SoulboundRecord withState(CompanionStatus status, boolean resurrection, String mode) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage,
                mode, status, resurrection, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall,
                (status==CompanionStatus.DEAD || status==CompanionStatus.MISSING)?forms.endGod():forms,progression,survival);
    }

    public SoulboundRecord adoptLegacy(UUID entityUuid, String stage, String mode) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, entityUuid, species, stage, mode,
                CompanionStatus.ACTIVE, false, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall,br.com.soulbound.companion.CompanionForms.migrate(stage),progression,survival);
    }

    public boolean matches(UUID owner, UUID bond, UUID companion) {
        return ownerUuid.equals(owner) && bondUuid.equals(bond) && companionUuid.equals(companion);
    }

    public boolean canSummon() {
        return companionStatus == CompanionStatus.READY && !resurrectionRequired
                && species.equals("LUPUS") && activeForm().equals("BABY");
    }
    public boolean canResurrect(){return companionStatus==CompanionStatus.DEAD && resurrectionRequired;}
    public SoulboundRecord recoverAfterDeath(){
        if(!canResurrect())throw new IllegalStateException("Only DEAD permits resurrection");
        var baby=forms.recoverBaby();
        return withState(CompanionStatus.READY,false,"FOLLOW_PASSIVE").withForms(baby);
    }
    public SoulboundRecord withEvolution(String stage,double progress,long godCooldown) {
        return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,stage,behaviorMode,
                companionStatus,resurrectionRequired,progress,godCooldown,eggDelivered,eggDeliveryPending,recall,stage.equals(evolutionStage)?forms:br.com.soulbound.companion.CompanionForms.migrate(stage),progression,survival);
    }
}
