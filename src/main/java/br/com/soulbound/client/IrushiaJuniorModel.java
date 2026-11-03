package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Adapted Irushia rig; native procedural animation only, no client gameplay state changes. */
public final class IrushiaJuniorModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("soulbound","lupus_junior_irushia"),"main");
    private final ModelPart body,head,rightArm,leftArm,rightLeg,leftLeg,rightFoot,leftFoot,rightWing,leftWing,tail,tailEnd,leftEar,rightEar;
    public IrushiaJuniorModel(ModelPart root){
        super(root);var lookup=root.createPartLookup();
        body=lookup.apply("UpperBody");head=lookup.apply("Head");rightArm=lookup.apply("RightArm");leftArm=lookup.apply("LeftArm");
        rightLeg=lookup.apply("RightLeg");leftLeg=lookup.apply("LeftLeg");rightFoot=lookup.apply("RightFeet");leftFoot=lookup.apply("LeftFeet");
        rightWing=lookup.apply("RightWing");leftWing=lookup.apply("LeftWing");tail=lookup.apply("TailRoot");tailEnd=lookup.apply("Tail1");
        leftEar=lookup.apply("LeftEarRoot");rightEar=lookup.apply("RightEarRoot");
    }
    public static LayerDefinition createBodyLayer(){return IrushiaJuniorGeometry.layer();}
    @Override public void setupAnim(BabyCompanionRenderState s){
        super.setupAnim(s);
        float amount=Mth.clamp(s.walkAnimationSpeed,0,1),stride=Mth.sin(s.walkAnimationPos*.7f)*amount*.6f;
        body.y+=Mth.sin(s.ageInTicks*.09f)*.1f+Math.abs(stride)*.3f;
        head.yRot+=Mth.clamp(s.yRot,-45,45)*Mth.DEG_TO_RAD;head.xRot+=Mth.clamp(s.xRot,-25,30)*Mth.DEG_TO_RAD;
        rightLeg.xRot+=stride;leftLeg.xRot-=stride;
        rightFoot.xRot+=Math.max(0,-stride)*.5f;leftFoot.xRot+=Math.max(0,stride)*.5f;
        rightArm.xRot-=stride*.45f;leftArm.xRot+=stride*.45f;
        float flutter=Mth.sin(s.ageInTicks*.18f)*.09f+amount*.12f;
        rightWing.yRot-=flutter;leftWing.yRot+=flutter;
        tail.yRot+=Mth.sin(s.ageInTicks*.075f)*.12f-stride*.18f;
        tailEnd.yRot+=Mth.sin(s.ageInTicks*.075f-.8f)*.14f;
        leftEar.zRot+=Mth.sin(s.ageInTicks*.07f)*.025f;rightEar.zRot-=Mth.sin(s.ageInTicks*.07f)*.025f;
        if(s.actionAmount>0){
            float swing=Mth.sin(Mth.clamp(s.actionProgress,0,1)*Mth.PI);
            if(s.actionType==1){rightArm.xRot-=swing*1.1f;leftArm.xRot-=swing*.5f;body.xRot+=swing*.18f;head.xRot+=swing*.12f;}
            if(s.actionType==2){body.xRot-=swing*.15f;rightArm.xRot-=swing*.9f;leftArm.xRot-=swing*.9f;rightWing.yRot-=swing*.5f;leftWing.yRot+=swing*.5f;}
            if(s.actionType==3){rightWing.yRot-=swing*.7f;leftWing.yRot+=swing*.7f;head.xRot-=swing*.15f;}
        }
        if(s.ritualizing){head.xRot+=.2f;rightArm.xRot-=.45f;leftArm.xRot-=.45f;}
        if(s.hasRedOverlay){body.zRot+=.06f;head.xRot+=.1f;}
        if(s.deathTime>0){rightArm.xRot+=.2f;leftArm.xRot+=.2f;}
    }
}
