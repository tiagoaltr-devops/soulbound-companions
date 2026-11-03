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
    private br.com.soulbound.companion.progression.TalentEffects.Profile talentProfile=br.com.soulbound.companion.progression.TalentEffects.NONE;
    private java.util.Map<String,Integer> cachedTalents=java.util.Map.of();private String cachedTalentForm="";private long combatUntil;private boolean healthWasGod;private double godHealthScale=2;
    public br.com.soulbound.companion.progression.TalentEffects.Profile talents(){return talentProfile;}
    public void cacheTalents(SoulboundRecord record){if(!cachedTalentForm.equals(record.activeForm()) || !cachedTalents.equals(record.progression().talents())){cachedTalentForm=record.activeForm();cachedTalents=record.progression().talents();talentProfile=br.com.soulbound.companion.progression.TalentEffects.profile(record);}}
    public void markCombat(){if(!level().isClientSide())combatUntil=level().getServer().overworld().getGameTime()+160; br.com.soulbound.companion.CompanionSurvival.markCombat(this);}
    public void healthBase(double base,boolean god){
        var attribute=getAttribute(Attributes.MAX_HEALTH);double old=getMaxHealth();float hp=getHealth();attribute.setBaseValue(base);
        if(healthWasGod!=god){if(god){godHealthScale=getMaxHealth()/old;hp=br.com.soulbound.companion.SurvivalRules.healthAfter(hp,old,getMaxHealth(),true);}else hp=br.com.soulbound.companion.SurvivalRules.endGod(hp,godHealthScale,getMaxHealth());}
        else hp=br.com.soulbound.companion.SurvivalRules.healthAfter(hp,old,getMaxHealth(),false);
        setHealth(hp);healthWasGod=god;
    }
    public long combatUntil(){return combatUntil;}
    private static final EntityDataAccessor<Integer> FORM=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> ACTION_END=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> RITUAL=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACTION_DURATION=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.INT);
    private final br.com.soulbound.entity.ai.PlenoCombatController plenoCombat=new br.com.soulbound.entity.ai.PlenoCombatController(this);
    public br.com.soulbound.entity.ai.PlenoCombatController plenoCombat(){return plenoCombat;}
    public boolean isGod(){return getEvolutionStage()==EvolutionStage.GOD;}
    public boolean isPleno(){return getEvolutionStage()==EvolutionStage.PLENO;}
    private static final EntityDataAccessor<Boolean> RESONATING=SynchedEntityData.defineId(LupusCompanionEntity.class,EntityDataSerializers.BOOLEAN);
    private br.com.soulbound.entity.ai.AncestralResonanceGoal resonanceGoal;
    public br.com.soulbound.entity.ai.AncestralResonanceGoal resonanceGoal(){return resonanceGoal;}
    public boolean isResonating(){return entityData.get(RESONATING);}
    public void setResonating(boolean value){if(!level().isClientSide())entityData.set(RESONATING,value);}
    private static final EntityDataAccessor<Integer> MODE =
            SynchedEntityData.defineId(LupusCompanionEntity.class, EntityDataSerializers.INT);
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
        return createMobAttributes().add(Attributes.MAX_HEALTH, 33.6)
                .add(Attributes.MOVEMENT_SPEED, 0.12).add(Attributes.FLYING_SPEED, 0.12)
                .add(Attributes.FOLLOW_RANGE, 32.0).add(Attributes.SCALE,1.0).add(Attributes.ATTACK_DAMAGE,4.0).add(Attributes.ATTACK_SPEED,4).add(Attributes.ARMOR_TOUGHNESS,0).add(Attributes.ATTACK_KNOCKBACK,0).add(Attributes.ARMOR,0).add(Attributes.KNOCKBACK_RESISTANCE,0);
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
    public void travel(Vec3 input) { if(isGroundForm())super.travel(input);else travelFlying(input, getSpeed()); }
    public boolean isGroundForm(){return br.com.soulbound.companion.CompanionArchetype.form(getEvolutionStage().name()).movement()==br.com.soulbound.companion.CompanionArchetype.Movement.GROUND;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(FORM))refreshDimensions();}
    public long getActionEnd(){return entityData.get(ACTION_END);}
    public boolean isRitualizing(){return entityData.get(RITUAL);}
    public void setRitualizing(boolean value){if(!level().isClientSide())entityData.set(RITUAL,value);}
    public void showAction(int ticks){if(!level().isClientSide()){entityData.set(ACTION_DURATION,ticks);entityData.set(ACTION_END,level().getGameTime()+ticks);}}
    public int actionDuration(){return entityData.get(ACTION_DURATION);}
    public boolean actionLocksMovement(){return getActionEnd()>level().getGameTime() && (actionType()==2 || actionType()==3 || actionType()>=15);}
    public void showAction(int ticks,int type){if(!level().isClientSide()){entityData.set(ACTION,type);showAction(ticks);}}
    public int actionType(){return entityData.get(ACTION);}
    public boolean canFight(){return !level().isClientSide() && isAlive() && SoulboundIdentity.isCanonical(this) && !br.com.soulbound.companion.CompanionSurvival.defensive(this) && br.com.soulbound.companion.JuniorCombat.enabled(getEvolutionStage().name(),getBehaviorMode(),br.com.soulbound.companion.CompanionFormService.busy(this));}
    @Override protected net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose){return (isPleno() || isGod())?net.minecraft.world.entity.EntityDimensions.scalable(1,2.25f):isGroundForm()?net.minecraft.world.entity.EntityDimensions.scalable(.65f,1.85f):super.getDefaultDimensions(pose);}
    private void updateMovement(){
        if(plenoCombat!=null)plenoCombat.cancelAll();
        stopFlight();setTarget(null);
        if(isGroundForm()){navigation=new net.minecraft.world.entity.ai.navigation.GroundPathNavigation(this,level());moveControl=new net.minecraft.world.entity.ai.control.MoveControl(this);}
        else {navigation=createNavigation(level());moveControl=new FlyingMoveControl(this,20,true);}
        setNoGravity(!isGroundForm());refreshDimensions();
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if(isGroundForm())super.checkFallDamage(y,onGround,state,pos);
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) { return isGroundForm() && super.causeFallDamage(distance,multiplier,source); }

    @Override
    public float getAgeScale() { return 1.0F; } // The custom mesh and dimensions are already Baby-sized.

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODE,0);builder.define(FORM,0);builder.define(ACTION_END,0L);builder.define(RITUAL,false);builder.define(ACTION,1);builder.define(ACTION_DURATION,0);
        builder.define(EMERGENCY_MOVING, false);
        builder.define(THREAT_ALERT_END, 0L);
        builder.define(MINERAL_DISCOVERY_END, 0L);
        builder.define(RESONATING,false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0,new br.com.soulbound.entity.ai.RitualFocusGoal(this));
        goalSelector.addGoal(1,new br.com.soulbound.entity.ai.DefensiveRetreatGoal(this));
        goalSelector.addGoal(4,new br.com.soulbound.entity.ai.JuniorCombatGoal(this));
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
        plenoCombat.tickUltimate();
        setNoGravity(!isGroundForm());
        threatSensor.tick(level);
        mineralInstinct.tick(level);
        if (tickCount % 40 == 0) CompanionLocationTracker.update(this);
        if (tickCount % 20 == 0) br.com.soulbound.ritual.CompanionEvolution.refresh(this);
        if(tickCount%20==0)br.com.soulbound.companion.progression.ProgressionService.explore(this);
        br.com.soulbound.companion.progression.TalentEffects.tick(this);
        br.com.soulbound.ritual.CompanionEvolution.tickGod(this);
    }

    @Override public void tick(){br.com.soulbound.companion.CompanionSurvival.tick(this);super.tick();if(!level().isClientSide() && getOwnerUuid()!=null){br.com.soulbound.companion.CompanionEquipment.capture(this);if(getHealth()>getMaxHealth())setHealth(getMaxHealth());}}
    @Override protected void hurtArmor(DamageSource source,float damage){doHurtEquipment(source,damage,net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET);br.com.soulbound.companion.CompanionEquipment.capture(this);}
    @Override protected void hurtHelmet(DamageSource source,float damage){doHurtEquipment(source,damage,net.minecraft.world.entity.EquipmentSlot.HEAD);br.com.soulbound.companion.CompanionEquipment.capture(this);}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){br.com.soulbound.companion.CompanionEquipment.capture(this);}
    @Override protected void dropEquipment(ServerLevel level){}
    public void equipmentKnockback(Entity target,DamageSource source,Vec3 oldVelocity){causeExtraKnockback(target,getKnockback(target,source),oldVelocity);}
    public ThreatSensor getThreatSensor() { return threatSensor; }

    public @Nullable Vec3 getStayAnchor() { return stayAnchor; }

    public @Nullable Vec3 getHoverStayPosition() { return stayAnchor == null ? null : stayAnchor.add(0, isGroundForm()?0:0.45, 0); }

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
        plenoCombat.cancelAll();
        resonanceGoal.cancel();
        for (var goal : goalSelector.getAvailableGoals()) goal.stop();
        stopFlight();
        setNoGravity(!isGroundForm());
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
        record=br.com.soulbound.companion.GodFormService.normalize(level().getServer(),record);
        br.com.soulbound.companion.CompanionEquipment.apply(this,record);cacheTalents(record);var previous=getEvolutionStage();evolutionStage = EvolutionStage.valueOf(record.activeForm());entityData.set(FORM,evolutionStage.ordinal());
        if(previous!=evolutionStage)updateMovement();
        br.com.soulbound.ritual.CompanionEvolution.attributes(this);
        var mode = BehaviorMode.fromSavedName(record.behaviorMode());
        if (getBehaviorMode() != mode || (mode == BehaviorMode.STAY && stayAnchor == null)) setBehaviorMode(mode);
    }

    public boolean isOwnedBy(Player player) { return player.getUUID().equals(ownerUuid); }

    public @Nullable Player getOwnerPlayer() {
        return ownerUuid == null ? null : level().getPlayerByUUID(ownerUuid);
    }

    public EvolutionStage getEvolutionStage() { int i=entityData.get(FORM);return i>=0 && i<EvolutionStage.values().length?EvolutionStage.values()[i]:EvolutionStage.BABY; }

    public BehaviorMode getBehaviorMode() {
        int i=entityData.get(MODE);return i>=0 && i<BehaviorMode.values().length?BehaviorMode.values()[i]:BehaviorMode.FOLLOW_PASSIVE;
    }

    private void setBehaviorMode(BehaviorMode mode) {
        if(plenoCombat!=null && !level().isClientSide())plenoCombat.cancelAll();
        entityData.set(MODE, mode.ordinal());setTarget(null);
        stayAnchor = mode == BehaviorMode.STAY ? position() : null;
        if (mode == BehaviorMode.STAY) stopFlight();
    }

    /** Applies the existing mode transition only after server-side owner/canonical identity checks. */
    public boolean commandBehaviorMode(Player player, BehaviorMode mode) {
        if (level().isClientSide() || !isAlive() || !isOwnedBy(player) || !SoulboundIdentity.isCanonical(this) || br.com.soulbound.companion.CompanionFormService.busy(this)) return false;
        if (getBehaviorMode() != mode) {
            setBehaviorMode(mode);
            SoulboundIdentity.behaviorChanged(this);
        }
        return true;
    }

    @Override
    public boolean isBaby() { return getEvolutionStage() == EvolutionStage.BABY; }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if(target==null || canFight() && br.com.soulbound.companion.JuniorCombat.target(this,target))super.setTarget(target);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) { return target instanceof LivingEntity living && br.com.soulbound.companion.JuniorCombat.hit(this,living,br.com.soulbound.companion.progression.TalentEffects.damage(this,br.com.soulbound.Config.JUNIOR_ATTACK_DAMAGE.get().floatValue()*(isGod()?2.6f:1),false)); }

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
        if(br.com.soulbound.companion.CompanionFood.healing(player.getItemInHand(hand))>0)return player instanceof net.minecraft.server.level.ServerPlayer owner && br.com.soulbound.companion.CompanionFood.manual(this,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;
        if(br.com.soulbound.companion.CompanionFormService.busy(this))return InteractionResult.FAIL;
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
        output.putString("EvolutionStage", evolutionStage.name());output.putDouble("GodHealthScale",godHealthScale);
        output.putString("BehaviorMode", getBehaviorMode().name());
        output.storeNullable("StayAnchor", Vec3.CODEC, stayAnchor);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);godHealthScale=input.getDoubleOr("GodHealthScale",2);if(!Double.isFinite(godHealthScale) || godHealthScale<=0)godHealthScale=2;
        ownerUuid = input.read("OwnerUUID", UUIDUtil.CODEC).orElse(null);
        bondUuid = input.read("SoulboundBond", UUIDUtil.CODEC).orElse(null);
        // Only BABY is supported in phase 1; unsupported values safely fall back to it.
        try {
            evolutionStage = EvolutionStage.valueOf(input.getStringOr("EvolutionStage", "BABY"));healthWasGod=evolutionStage==EvolutionStage.GOD;
        } catch (IllegalArgumentException exception) {
            evolutionStage = EvolutionStage.BABY;
        }
        entityData.set(FORM,evolutionStage.ordinal());updateMovement();
        setBehaviorMode(BehaviorMode.fromSavedName(input.getStringOr("BehaviorMode", "FOLLOW_PASSIVE")));
        br.com.soulbound.companion.CompanionEquipment.restoreHealthOnLoad(this,input.getFloatOr("Health",getHealth()));
        if (getBehaviorMode() == BehaviorMode.STAY) {
            stayAnchor = input.read("StayAnchor", Vec3.CODEC).filter(anchor -> Double.isFinite(anchor.x)
                    && Double.isFinite(anchor.y) && Double.isFinite(anchor.z)).orElse(position());
        }
    }
}
