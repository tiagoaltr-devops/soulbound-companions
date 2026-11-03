package br.com.soulbound.client;

import br.com.soulbound.ritual.OfferingBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.*;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** One rendered stack, no entities and no client inventory authority. */
public final class OfferingRenderer implements BlockEntityRenderer<OfferingBlockEntity,OfferingRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        final ItemStackRenderState item=new ItemStackRenderState();float time;
    }
    private final ItemModelResolver resolver;
    public OfferingRenderer(BlockEntityRendererProvider.Context context){resolver=context.itemModelResolver();}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(OfferingBlockEntity block,State state,float partial,Vec3 camera,ModelFeatureRenderer.CrumblingOverlay overlay){
        BlockEntityRenderer.super.extractRenderState(block,state,partial,camera,overlay);
        // The altar is opaque: its own voxel has no light. The displayed stack occupies the air above it.
        if(block.getLevel()!=null)state.lightCoords=net.minecraft.client.renderer.LevelRenderer.getLightColor(block.getLevel(),block.getBlockPos().above());
        state.time=block.getLevel()==null?0:(block.getLevel().getGameTime()%24000)+partial;
        resolver.updateForTopItem(state.item,block.display(),ItemDisplayContext.GROUND,block.getLevel(),null,(int)block.getBlockPos().asLong());
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        if(state.item.isEmpty())return;
        pose.pushPose();pose.translate(.5,1.35+Math.sin(state.time*.06)*.06,.5);pose.mulPose(Axis.YP.rotationDegrees(state.time*1.5f));pose.scale(.7f,.7f,.7f);
        state.item.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);pose.popPose();
    }
}
