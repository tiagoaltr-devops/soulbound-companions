package br.com.soulbound.client;

import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class LupusCompanionRenderer extends MobRenderer<LupusCompanionEntity, BabyCompanionRenderState, net.minecraft.client.model.EntityModel<BabyCompanionRenderState>> {
    private final BabyCompanionModel baby;
    private final GodPrimeModel god;private final IrushiaJuniorModel junior; private final BramblebackPlenoModel pleno;
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("soulbound", "textures/entity/lupus_baby.png");

    public LupusCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyCompanionModel(context.bakeLayer(BabyCompanionModel.LAYER)), 0.2F);
        god=new GodPrimeModel(context.bakeLayer(GodPrimeModel.LAYER));pleno=new BramblebackPlenoModel(context.bakeLayer(BramblebackPlenoModel.LAYER));baby=(BabyCompanionModel)model;junior=new IrushiaJuniorModel(context.bakeLayer(IrushiaJuniorModel.LAYER));
    }

    @Override
    public BabyCompanionRenderState createRenderState() { return new BabyCompanionRenderState(); }

    @Override
    public Identifier getTextureLocation(BabyCompanionRenderState state) { return state.god?Identifier.fromNamespaceAndPath("soulbound","textures/entity/lupus_god_prime.png"):state.pleno?Identifier.fromNamespaceAndPath("soulbound","textures/entity/lupus_pleno_brambleback.png"):state.junior?Identifier.fromNamespaceAndPath("soulbound","textures/entity/lupus_junior_irushia.png"):TEXTURE; }
    @Override public void submit(BabyCompanionRenderState state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.CameraRenderState camera){model=state.god?god:state.pleno?pleno:state.junior?junior:baby;super.submit(state,pose,collector,camera);}

    @Override
    public void extractRenderState(LupusCompanionEntity entity, BabyCompanionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.god=entity.isGod();state.pleno=entity.isPleno();state.junior=entity.getEvolutionStage()==br.com.soulbound.companion.EvolutionStage.JUNIOR;state.actionProgress=1-Mth.clamp((float)(entity.getActionEnd()-entity.level().getGameTime()-partialTick)/Math.max(1,entity.actionDuration()),0,1);state.ritualizing=entity.isRitualizing();state.actionType=entity.actionType();state.actionAmount=Mth.clamp((float)(entity.getActionEnd()-entity.level().getGameTime()-partialTick)/30,0,1);
        state.staying = entity.getBehaviorMode() == BehaviorMode.STAY && !entity.isEmergencyMoving() && !entity.isResonating();
        state.retreating = entity.isEmergencyMoving();
        double visualTime = entity.level().getGameTime() + partialTick;
        state.alerting = entity.getThreatAlertEnd() > visualTime;
        float remaining = (float) (entity.getMineralDiscoveryEnd() - visualTime);
        state.discoveryAmount = remaining > 0 ? Mth.sin(Mth.clamp(1.0F - remaining / 30.0F, 0, 1) * Mth.PI) : 0;
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        state.flightAmount = Mth.clamp((float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 6.0F, 0, 1);
        // The vanilla body controller and renderer interpolate travel orientation; do not snap yaw from velocity.
        if (!state.junior && !state.pleno && !state.god && !state.alerting && state.flightAmount > 0.1F) state.yRot = 0;
    }
}
