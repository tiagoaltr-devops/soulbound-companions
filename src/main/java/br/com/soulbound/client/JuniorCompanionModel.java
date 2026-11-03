package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original light armor, separate joints and a forearm-mounted spiritual blade. */
public final class JuniorCompanionModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("soulbound","lupus_junior"),"main");
    private final ModelPart torso,head,right,left,foreR,foreL,legR,legL,kneeR,kneeL;
    public JuniorCompanionModel(ModelPart root){super(root);torso=root.getChild("torso");head=torso.getChild("head");right=torso.getChild("right");left=torso.getChild("left");foreR=right.getChild("forearm");foreL=left.getChild("forearm");legR=root.getChild("leg_r");legL=root.getChild("leg_l");kneeR=legR.getChild("shin");kneeL=legL.getChild("shin");}
    private static CubeListBuilder box(int u,int v,float x,float y,float z,int w,int h,int d){return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);}
    public static LayerDefinition createBodyLayer(){
        var m=new MeshDefinition();var r=m.getRoot();var t=r.addOrReplaceChild("torso",box(0,14,-3,0,-1.5f,6,7,3),PartPose.ZERO);
        var h=t.addOrReplaceChild("head",box(0,0,-3,-6,-3,6,6,6),PartPose.ZERO);
        h.addOrReplaceChild("brow",box(0,52,-3.5f,-6.2f,-3.5f,7,1,7),PartPose.ZERO);
        t.addOrReplaceChild("breastplate",box(64,0,-3.5f,.2f,-2,7,3,4),PartPose.rotation(-.06f,0,0));
        t.addOrReplaceChild("abdomen",box(64,10,-2.5f,5,-2,5,2,1),PartPose.ZERO);
        t.addOrReplaceChild("waist",box(0,26,-3,7,-1.5f,6,3,3),PartPose.ZERO);
        t.addOrReplaceChild("belt",box(0,26,-3,10,-1.5f,6,2,3),PartPose.ZERO);
        t.addOrReplaceChild("soul_sigil",box(10,48,-1,2,-3,2,3,1),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            String name=side<0?"right":"left";
            var a=t.addOrReplaceChild(name,box(28,10,-1.5f,0,-1.5f,3,5,3),PartPose.offset(side*5,1,0));
            a.addOrReplaceChild("shoulder",box(28,0,-2,-1,-2.5f,4,3,5),PartPose.rotation(0,0,side*.16f));
            a.addOrReplaceChild("elbow",box(80,10,-1.5f,4,-2,3,2,4),PartPose.ZERO);
            var f=a.addOrReplaceChild("forearm",box(42,10,-1.5f,0,-2,3,5,4),PartPose.offset(0,5,0));
            f.addOrReplaceChild("hand",box(28,20,-1.5f,5,-1.5f,3,2,3),PartPose.ZERO);
            if(side<0){var blade=f.addOrReplaceChild("soul_blade",box(48,26,-1,-13,-.5f,2,12,1),PartPose.offsetAndRotation(0,6,-1,.45f,0,0));blade.addOrReplaceChild("guard",box(46,42,-2.5f,-1,-1,5,1,2),PartPose.ZERO);}
            var leg=r.addOrReplaceChild(side<0?"leg_r":"leg_l",box(0,34,-1.5f,0,-1.5f,3,5,3),PartPose.offset(side*2,12,0));
            leg.addOrReplaceChild("hip_plate",box(64,16,-1.5f,0,-2,3,3,1),PartPose.rotation(-.08f,0,side*.12f));
            var shin=leg.addOrReplaceChild("shin",box(14,34,-1.5f,0,-1.5f,3,5,3),PartPose.offset(0,5,0));
            shin.addOrReplaceChild("boot",box(28,28,-2,4,-3,4,3,5),PartPose.ZERO);
        }
        return LayerDefinition.create(m,128,128);
    }
    @Override public void setupAnim(BabyCompanionRenderState s){
        super.setupAnim(s);head.yRot=Mth.clamp(s.yRot,-55,55)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-35,35)*Mth.DEG_TO_RAD;
        float phase=s.walkAnimationPos*.65f,amount=Math.min(1,s.walkAnimationSpeed),stride=Mth.sin(phase)*amount*.65f;
        legR.xRot=stride;legL.xRot=-stride;kneeR.xRot=Math.max(0,-stride)*.65f;kneeL.xRot=Math.max(0,stride)*.65f;
        torso.y=Mth.sin(s.ageInTicks*.09f)*.07f+Math.abs(Mth.sin(phase))*amount*.22f;torso.yRot=-stride*.13f;torso.xRot=amount*.04f;
        right.xRot=-.12f-stride*.45f;left.xRot=-.08f+stride*.55f;right.zRot=.07f;left.zRot=-.07f;foreR.xRot=-.35f;foreL.xRot=-.12f;
        if(s.actionAmount>0){float attack=Mth.sin(Mth.clamp(s.actionProgress/.65f,0,1)*Mth.PI);
            if(s.actionType==1){right.xRot=-.12f-attack*1.2f;foreR.xRot=-.35f-attack*.4f;torso.yRot=-attack*.22f;left.xRot=-.12f+attack*.16f;}
            if(s.actionType==2){right.xRot=-1.15f;left.xRot=-1.15f;right.zRot=.35f;left.zRot=-.35f;foreR.xRot=foreL.xRot=-.35f;}
            if(s.actionType==3){right.xRot=left.xRot=-.8f;head.xRot=.2f;}
        }
        if(s.ritualizing){right.xRot=left.xRot=-.65f;foreR.xRot=foreL.xRot=-.5f;head.xRot=.25f;}
        if(s.hasRedOverlay){torso.zRot=.07f;head.xRot+=.1f;}
        if(s.deathTime>0){right.xRot=left.xRot=.2f;foreR.xRot=foreL.xRot=.1f;}
    }
}
