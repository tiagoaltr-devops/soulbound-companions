package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original shared Baby geometry and procedural animation; species appearance comes from the renderer's texture. */
public final class BabyCompanionModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath("soulbound", "baby_companion"), "main");
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public BabyCompanionModel(ModelPart baked) {
        super(baked.getChild("baby"));
        head = root.getChild("head");
        body = root.getChild("body");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        leftWing = body.getChild("left_wing");
        rightWing = body.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition();
        var baby = mesh.getRoot().addOrReplaceChild("baby", CubeListBuilder.create(), PartPose.offset(0, 24, 0));
        var head = baby.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.5F, -6, -3, 7, 6, 6), PartPose.offset(0, -5, 0));
        // Small generic crown fins; the Lupus palette gives them its ear-like identity.
        head.addOrReplaceChild("left_fin", CubeListBuilder.create().texOffs(40, 0)
                .addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(2.5F, -6, 0));
        head.addOrReplaceChild("right_fin", CubeListBuilder.create().texOffs(48, 0)
                .addBox(-1, -3, -1, 2, 3, 2), PartPose.offset(-2.5F, -6, 0));
        var body = baby.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -1.5F, 4, 5, 3), PartPose.offset(0, -5, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(16, 16)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(2.7F, 0.4F, 0));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(24, 16)
                .addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(-2.7F, 0.4F, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, 0, 0, 1, 6, 7), PartPose.offset(1.0F, 0.3F, 1.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(20, 32)
                .addBox(-1, 0, 0, 1, 6, 7), PartPose.offset(-1.0F, 0.3F, 1.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BabyCompanionRenderState state) {
        super.setupAnim(state);
        float age = state.ageInTicks;
        float flight = state.flightAmount;
        float wingBeat = Mth.sin(age * (0.42F + 0.55F * flight)) * (0.45F + flight * 0.35F);
        root.y += Mth.sin(age * 0.12F) * (state.staying ? 0.45F : 0.7F);
        head.yRot = Mth.clamp(state.yRot, -70, 70) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(state.xRot, -35, 35) * Mth.DEG_TO_RAD;
        body.xRot = flight * 0.28F;
        leftWing.yRot = -0.7F - wingBeat;
        rightWing.yRot = 0.7F + wingBeat;
        leftWing.zRot = -0.08F;
        rightWing.zRot = 0.08F;
        leftArm.zRot = -0.18F - Mth.sin(age * 0.12F) * 0.08F;
        rightArm.zRot = 0.18F + Mth.sin(age * 0.12F) * 0.08F;
        leftArm.xRot = rightArm.xRot = -flight * 0.35F;
        if (state.alerting) {
            head.zRot = Mth.sin(age * 0.6F) * 0.08F;
            leftArm.xRot = rightArm.xRot = -0.5F;
        }
        if (state.retreating) body.xRot = -0.2F;
        float discovery = state.discoveryAmount;
        root.y -= discovery * 1.2F;
        root.zRot = Mth.sin(age * 0.4F) * discovery * 0.14F;
        leftArm.zRot -= discovery * 0.9F;
        rightArm.zRot += discovery * 0.9F;
        head.xRot -= discovery * 0.2F;
        if(state.ritualizing || state.actionAmount>0 && state.actionType==3){head.xRot=.3f;leftArm.xRot=rightArm.xRot=-1.1f;leftWing.yRot=-1;rightWing.yRot=1;}
    }
}
