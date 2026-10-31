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
        boolean eggDelivered, boolean eggDeliveryPending, SoulRecallState recall) {
    public static final Codec<SoulboundRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
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
            SoulRecallState.MAP_CODEC.forGetter(SoulboundRecord::recall)
    ).apply(instance, SoulboundRecord::new));

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
                behaviorMode, companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, state);
    }

    public static SoulboundRecord create(UUID owner, String name) {
        return new SoulboundRecord(owner, name, UUID.randomUUID(), UUID.randomUUID(), "LUPUS", "BABY",
                "FOLLOW_PASSIVE", CompanionStatus.READY, false, 0.0, 0L, false, true);
    }

    public SoulboundRecord withName(String name) {
        return new SoulboundRecord(ownerUuid, name, bondUuid, companionUuid, species, evolutionStage, behaviorMode,
                companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall);
    }

    public SoulboundRecord withDelivery(boolean delivered, boolean pending) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage,
                behaviorMode, companionStatus, resurrectionRequired, evolutionProgress, godCooldownEnd, delivered, pending, recall);
    }

    public SoulboundRecord withState(CompanionStatus status, boolean resurrection, String mode) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, companionUuid, species, evolutionStage,
                mode, status, resurrection, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall);
    }

    public SoulboundRecord adoptLegacy(UUID entityUuid, String stage, String mode) {
        return new SoulboundRecord(ownerUuid, ownerLastKnownName, bondUuid, entityUuid, species, stage, mode,
                CompanionStatus.ACTIVE, false, evolutionProgress, godCooldownEnd, eggDelivered, eggDeliveryPending, recall);
    }

    public boolean matches(UUID owner, UUID bond, UUID companion) {
        return ownerUuid.equals(owner) && bondUuid.equals(bond) && companionUuid.equals(companion);
    }

    public boolean canSummon() {
        return companionStatus == CompanionStatus.READY && !resurrectionRequired
                && species.equals("LUPUS") && evolutionStage.equals("BABY");
    }
    public SoulboundRecord withEvolution(String stage,double progress,long godCooldown) {
        return new SoulboundRecord(ownerUuid,ownerLastKnownName,bondUuid,companionUuid,species,stage,behaviorMode,
                companionStatus,resurrectionRequired,progress,godCooldown,eggDelivered,eggDeliveryPending,recall);
    }
}
