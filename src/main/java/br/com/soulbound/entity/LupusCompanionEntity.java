package br.com.soulbound.entity;

import java.util.UUID;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.EvolutionStage;
import br.com.soulbound.entity.ai.FollowCompanionOwnerGoal;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class LupusCompanionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> STAY =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private @Nullable UUID ownerUuid;
    private EvolutionStage evolutionStage = EvolutionStage.BABY;

    public LupusCompanionEntity(EntityType<? extends LupusCompanionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAY, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new FollowCompanionOwnerGoal(this));
        goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public @Nullable UUID getOwnerUuid() { return ownerUuid; }

    public void assignOwner(Player player) {
        if (!level().isClientSide() && ownerUuid == null) {
            ownerUuid = player.getUUID();
            setBehaviorMode(BehaviorMode.FOLLOW_PASSIVE);
        }
    }

    public boolean isOwnedBy(Player player) { return player.getUUID().equals(ownerUuid); }

    public @Nullable Player getOwnerPlayer() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public EvolutionStage getEvolutionStage() { return evolutionStage; }

    public BehaviorMode getBehaviorMode() {
        return entityData.get(STAY) ? BehaviorMode.STAY : BehaviorMode.FOLLOW_PASSIVE;
    }

    private void setBehaviorMode(BehaviorMode mode) {
        entityData.set(STAY, mode == BehaviorMode.STAY);
        if (mode == BehaviorMode.STAY) getNavigation().stop();
    }

    @Override
    public boolean isBaby() { return true; }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Baby companions never acquire combat targets, including retaliation.
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) { return false; }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        // Handle ownership before vanilla name tags, spawn eggs or other interactions.
        return isAlive() ? mobInteract(player, hand) : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("message.soulbound.not_owner"), true);
            return InteractionResult.SUCCESS;
        }
        setBehaviorMode(getBehaviorMode() == BehaviorMode.STAY ? BehaviorMode.FOLLOW_PASSIVE : BehaviorMode.STAY);
        player.displayClientMessage(Component.translatable("message.soulbound.mode",
                Component.translatable("behavior.soulbound." + getBehaviorMode().name().toLowerCase(java.util.Locale.ROOT))), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("OwnerUUID", UUIDUtil.CODEC, ownerUuid);
        output.putString("EvolutionStage", evolutionStage.name());
        output.putString("BehaviorMode", getBehaviorMode().name());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerUuid = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        // Only BABY is supported in phase 1; unsupported values safely fall back to it.
        try {
            evolutionStage = EvolutionStage.valueOf(input.getStringOr("EvolutionStage", "BABY"));
        } catch (IllegalArgumentException exception) {
            evolutionStage = EvolutionStage.BABY;
        }
        setBehaviorMode(BehaviorMode.fromSavedName(input.getStringOr("BehaviorMode", "FOLLOW_PASSIVE")));
    }
}
