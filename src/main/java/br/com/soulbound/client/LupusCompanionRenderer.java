package br.com.soulbound.client;

import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.resources.Identifier;

public class LupusCompanionRenderer extends MobRenderer<LupusCompanionEntity, WolfRenderState, WolfModel> {
    public LupusCompanionRenderer(EntityRendererProvider.Context context) {
        super(context, new WolfModel(context.bakeLayer(ModelLayers.WOLF_BABY)), 0.25F);
    }

    @Override
    public WolfRenderState createRenderState() { return new WolfRenderState(); }

    @Override
    public Identifier getTextureLocation(WolfRenderState state) { return state.texture; }

    @Override
    public void extractRenderState(LupusCompanionEntity entity, WolfRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.isSitting = entity.getBehaviorMode() == BehaviorMode.STAY;
    }
}
