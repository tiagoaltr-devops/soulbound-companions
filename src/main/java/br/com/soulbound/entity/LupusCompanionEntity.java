package br.com.soulbound.entity;

import java.util.UUID;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.EvolutionStage;
import br.com.soulbound.companion.MineralInstinct;
import br.com.soulbound.companion.ThreatSensor;
import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.companion.recall.CompanionLocationTracker;
import br.com.soulbound.entity.ai.BabyFleeThreatGoal;
import br.com.soulbound.entity.ai.ReturnToStayGoal;
import br.com.soulbound.entity.ai.FollowCompanionOwnerGoal;
import br.com.soulbound.entity.ai.BabyHoverGoal;
import br.com.soulbound.entity.ai.ThreatRecoilGoal;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class LupusCompanionEntity extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> RESONATING=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.BOOLEAN);
    private br.com.soulbound.entity.ai.AncestralResonanceGoal resonanceGoal;
    public br.com.soulbound.entity.ai.AncestralResonanceGoal resonanceGoal(){return resonanceGoal;}
    public boolean isResonating(){return entityData.get(RESONATING);}
    public void setResonating(boolean value){if(!level().isClientSide())entityData.set(RESONATING,value);}
    private static final EntityDataAccessor<Boolean> STAY =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> EMERGENCY_MOVING =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> THREAT_ALERT_END =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> MINERAL_DISCOVERY_END =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.LONG);
    private @Nullable UUID ownerUuid;
    private @Nullable UUID bondUuid;
    private @Nullable Vec3 stayAnchor;
    private EvolutionStage evolutionStage = EvolutionStage.BABY;
    private final ThreatSensor threatSensor = new ThreatSensor(this);
    private final MineralInstinct mineralInstinct = new MineralInstinct(this);

    public LupusCompanionEntity(EntityType<? extends LupusCompanionEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.FLYING_SPEED, 0.12)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.SCALE,1.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        var flight = new FlyingPathNavigation(this, level);
        flight.setCanOpenDoors(false);
        flight.setCanFloat(true);
        flight.setRequiredPathLength(48.0F);
        return flight;
    }

    @Override
    public void travel(Vec3 input) { travelFlying(input, getSpeed()); }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        // Intrinsically flying, like Allay: normal flight never accumulates landing damage.
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return false; }

    @Override
    public float getAgeScale() { return 1.0F; } // The custom mesh and dimensions are already Baby-sized.

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAY, false);
        builder.define(EMERGENCY_MOVING, false);
        builder.define(THREAT_ALERT_END, 0L);
        builder.define(MINERAL_DISCOVERY_END, 0L);
        builder.define(RESONATING,false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new BabyFleeThreatGoal(this));
        goalSelector.addGoal(2, new ThreatRecoilGoal(this));
        resonanceGoal=new br.com.soulbound.entity.ai.AncestralResonanceGoal(this);
        goalSelector.addGoal(3,resonanceGoal);
        goalSelector.addGoal(4, new ReturnToStayGoal(this));
        goalSelector.addGoal(5, new FollowCompanionOwnerGoal(this));
        goalSelector.addGoal(6, new BabyHoverGoal(this));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        if (!SoulboundIdentity.isCanonical(this)) {
            discard();
            return;
        }
        super.customServerAiStep(level);
        setNoGravity(true);
        threatSensor.tick(level);
        mineralInstinct.tick(level);
        if (tickCount % 40 == 0) CompanionLocationTracker.update(this);
        if (tickCount % 20 == 0) br.com.soulbound.ritual.CompanionEvolution.refresh(this);
        br.com.soulbound.ritual.CompanionEvolution.tickGod(this);
    }

    public ThreatSensor getThreatSensor() { return threatSensor; }

    public @Nullable Vec3 getStayAnchor() { return stayAnchor; }

    public @Nullable Vec3 getHoverStayPosition() { return stayAnchor == null ? null : stayAnchor.add(0, 0.45, 0); }

    public void stopFlight() {
        if (level().isClientSide()) return;
        stopInPlace();
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
    }

    public long getThreatAlertEnd() { return entityData.get(THREAT_ALERT_END); }

    public long getMineralDiscoveryEnd() { return entityData.get(MINERAL_DISCOVERY_END); }

    public void showThreatAlert() {
        if (!level().isClientSide()) entityData.set(THREAT_ALERT_END, level().getGameTime() + 40);
    }

    public void showMineralDiscovery() {
        if (!level().isClientSide()) entityData.set(MINERAL_DISCOVERY_END, level().getGameTime() + 30);
    }

    /** Clear transient goals/sensor references from the old area; retain all persisted identity and attributes. */
    public boolean completeSoulRecall(Player player) {
        if (level().isClientSide() || !isOwnedBy(player) || !SoulboundIdentity.isCanonical(this)) return false;
        resonanceGoal.cancel();
        for (var goal : goalSelector.getAvailableGoals()) goal.stop();
        stopFlight();
        setNoGravity(true);
        setEmergencyMoving(false);
        threatSensor.resetAfterRecall();
        entityData.set(THREAT_ALERT_END, 0L);
        entityData.set(MINERAL_DISCOVERY_END, 0L);
        return commandBehaviorMode(player, BehaviorMode.FOLLOW_PASSIVE);
    }

    public boolean isEmergencyMoving() { return entityData.get(EMERGENCY_MOVING); }

    public void setEmergencyMoving(boolean moving) {
        if (!level().isClientSide()) entityData.set(EMERGENCY_MOVING, moving);
    }

    public @Nullable UUID getOwnerUuid() { return ownerUuid; }

    public @Nullable UUID getBondUuid() { return bondUuid; }

    public void applyIdentity(SoulboundRecord record) {
        if (level().isClientSide()) return;
        ownerUuid = record.ownerUuid();
        bondUuid = record.bondUuid();
        evolutionStage = EvolutionStage.valueOf(record.evolutionStage());
        br.com.soulbound.ritual.CompanionEvolution.attributes(this);
        var mode = BehaviorMode.fromSavedName(record.behaviorMode());
        if (getBehaviorMode() != mode || (mode == BehaviorMode.STAY && stayAnchor == null)) setBehaviorMode(mode);
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
        stayAnchor = mode == BehaviorMode.STAY ? position() : null;
        if (mode == BehaviorMode.STAY) stopFlight();
    }

    /** Applies the existing mode transition only after server-side owner/canonical identity checks. */
    public boolean commandBehaviorMode(Player player, BehaviorMode mode) {
        if (level().isClientSide() || !isAlive() || !isOwnedBy(player) || !SoulboundIdentity.isCanonical(this)) return false;
        if (getBehaviorMode() != mode) {
            setBehaviorMode(mode);
            SoulboundIdentity.behaviorChanged(this);
        }
        return true;
    }

    @Override
    public boolean isBaby() { return evolutionStage == EvolutionStage.BABY; }

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
        if (!isOwnedBy(player) || !SoulboundIdentity.isCanonical(this)) {
            player.displayClientMessage(Component.translatable("message.soulbound.not_owner"), true);
            return InteractionResult.SUCCESS;
        }
        if (SoulboundIdentity.isEgg(player.getItemInHand(hand))) {
            return player.getItemInHand(hand).getItem().use(level(), player, hand);
        }
        setBehaviorMode(getBehaviorMode() == BehaviorMode.STAY ? BehaviorMode.FOLLOW_PASSIVE : BehaviorMode.STAY);
        SoulboundIdentity.behaviorChanged(this);
        player.displayClientMessage(Component.translatable("message.soulbound.mode",
                Component.translatable("behavior.soulbound." + getBehaviorMode().name().toLowerCase(java.util.Locale.ROOT))), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        CompanionLocationTracker.update(this);
        super.addAdditionalSaveData(output);
        output.storeNullable("OwnerUUID", UUIDUtil.CODEC, ownerUuid);
        output.storeNullable("SoulboundBond", UUIDUtil.CODEC, bondUuid);
        output.putString("EvolutionStage", evolutionStage.name());
        output.putString("BehaviorMode", getBehaviorMode().name());
        output.storeNullable("StayAnchor", Vec3.CODEC, stayAnchor);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerUuid = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        bondUuid = input.read("SoulboundBond", UUIDUtil.CODEC).orElse(null);
        // Only BABY is supported in phase 1; unsupported values safely fall back to it.
        try {
            evolutionStage = EvolutionStage.valueOf(input.getStringOr("EvolutionStage", "BABY"));
        } catch (IllegalArgumentException exception) {
            evolutionStage = EvolutionStage.BABY;
        }
        setBehaviorMode(BehaviorMode.fromSavedName(input.getStringOr("BehaviorMode", "FOLLOW_PASSIVE")));
        if (getBehaviorMode() == BehaviorMode.STAY) {
            stayAnchor = input.read("StayAnchor", Vec3.CODEC).filter(anchor -> Double.isFinite(anchor.x)
                    && Double.isFinite(anchor.y) && Double.isFinite(anchor.z)).orElse(position());
        }
    }
}
