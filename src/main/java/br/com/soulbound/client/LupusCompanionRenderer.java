package br.com.soulbound.client;

import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class LupusCompanionRenderer extends MobRenderer<LupusCompanionEntity, BabyCompanionRenderState, BabyCompanionModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("soulbound", "textures/entity/lupus_baby.png");

    public LupusCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyCompanionModel(context.bakeLayer(BabyCompanionModel.LAYER)), 0.2F);
    }

    @Override
    public BabyCompanionRenderState createRenderState() { return new BabyCompanionRenderState(); }

    @Override
    public Identifier getTextureLocation(BabyCompanionRenderState state) { return TEXTURE; }

    @Override
    public void extractRenderState(LupusCompanionEntity entity, BabyCompanionRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
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
        if (!state.alerting && state.flightAmount > 0.1F) state.yRot = 0;
    }
}
