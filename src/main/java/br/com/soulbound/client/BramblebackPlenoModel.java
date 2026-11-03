package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Adaptation of monkus' Brambleback, CC BY-SA 4.0; see packaged credits/brambleback.txt. */
public final class BramblebackPlenoModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("soulbound","lupus_pleno_brambleback"),"main");
    private final ModelPart torso,upper,head,right,left,foreR,foreL,legR,legL,kneeR,kneeL;
    public BramblebackPlenoModel(ModelPart root){
        super(root);var parts=root.createPartLookup();torso=parts.apply("torso");upper=parts.apply("UPPER");head=parts.apply("h_HEAD");
        right=parts.apply("R_ARM");left=parts.apply("L_ARM");foreR=parts.apply("R_LOWER_ARM");foreL=parts.apply("L_LOWER_ARM");
        legR=parts.apply("R_LEG");legL=parts.apply("L_LEG");kneeR=parts.apply("R_LOWER_LEG");kneeL=parts.apply("L_LOWER_LEG");
    }
    public static LayerDefinition createBodyLayer(){return BramblebackPlenoGeometry.layer();}
    @Override public void setupAnim(BabyCompanionRenderState s){
        super.setupAnim(s);
        float amount=Mth.clamp(s.walkAnimationSpeed,0,1),stride=Mth.sin(s.walkAnimationPos*.65f)*amount*.48f;
        head.yRot+=Mth.clamp(s.yRot,-35,35)*Mth.DEG_TO_RAD;head.xRot+=Mth.clamp(s.xRot,-20,25)*Mth.DEG_TO_RAD;
        torso.y+=Mth.sin(s.ageInTicks*.06f)*.08f+Math.abs(stride)*.45f;
        upper.yRot-=stride*.12f;legR.xRot+=stride;legL.xRot-=stride;
        kneeR.xRot+=Math.max(0,-stride)*.5f;kneeL.xRot+=Math.max(0,stride)*.5f;
        right.xRot-=stride*.5f;left.xRot+=stride*.5f;
        if(s.actionAmount>0){float p=Mth.clamp(s.actionProgress,0,1),swing=Mth.sin(p*Mth.PI);
            switch(s.actionType){
                case 10,11->{boolean r=s.actionType==10;upper.yRot+=(r?1:-1)*swing*.3f;(r?right:left).xRot-=swing*1.2f;(r?foreR:foreL).xRot-=swing*.35f;}
                case 12,14,16->{float lift=Mth.sin(Mth.clamp(p/.75f,0,1)*Mth.PI);right.xRot-=lift*1.6f;left.xRot-=lift*1.6f;upper.xRot+=swing*.22f;foreR.xRot-=lift*.25f;foreL.xRot-=lift*.25f;}
                case 13->{upper.xRot+=.15f;right.xRot-=.45f;left.xRot-=.45f;}
                case 15->{right.xRot-=.8f;left.xRot-=.8f;head.xRot-=.12f;}
                case 3->{right.zRot+=swing*.2f;left.zRot-=swing*.2f;}
                default->{}
            }
        }
        if(s.ritualizing){head.xRot+=.2f;right.xRot-=.55f;left.xRot-=.55f;}
        if(s.hasRedOverlay){torso.zRot+=.05f;head.xRot+=.1f;}
        if(s.deathTime>0){right.xRot+=.2f;left.xRot+=.2f;}
    }
}
