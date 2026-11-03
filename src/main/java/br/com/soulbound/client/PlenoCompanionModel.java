package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original layered ancestral armor and stepped double axe; all pivots are joint-local. */
public final class PlenoCompanionModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("soulbound","lupus_pleno"),"main");
    private final ModelPart torso,head,right,left,foreR,foreL,legR,legL,kneeR,kneeL,axe;
    public PlenoCompanionModel(ModelPart root){super(root);torso=root.getChild("torso");head=torso.getChild("head");right=torso.getChild("right");left=torso.getChild("left");foreR=right.getChild("forearm");foreL=left.getChild("forearm");legR=root.getChild("leg_r");legL=root.getChild("leg_l");kneeR=legR.getChild("shin");kneeL=legL.getChild("shin");axe=torso.getChild("axe");}
    private static CubeListBuilder box(int u,int v,float x,float y,float z,int w,int h,int d){return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);}
    public static LayerDefinition createBodyLayer(){
        var m=new MeshDefinition();var r=m.getRoot();
        var t=r.addOrReplaceChild("torso",box(32,0,-4,0,-2.5f,8,9,5),PartPose.offset(0,-5,0));
        t.addOrReplaceChild("breastplate",box(0,104,-5,.2f,-3,10,4,6),PartPose.rotation(-.06f,0,0));
        t.addOrReplaceChild("abdomen",box(36,104,-3.5f,6,-3,7,2,1),PartPose.ZERO);
        var h=t.addOrReplaceChild("head",box(0,0,-3.5f,-7,-3.5f,7,7,7),PartPose.ZERO);
        h.addOrReplaceChild("crown",box(0,60,-4,-7.5f,-4,8,2,8),PartPose.ZERO);
        t.addOrReplaceChild("sigil",box(48,60,-1.5f,2,-4,3,4,1),PartPose.ZERO);
        t.addOrReplaceChild("waist",box(64,0,-3.5f,9,-2,7,4,4),PartPose.ZERO);
        t.addOrReplaceChild("hips",box(96,0,-4,11,-2.5f,8,3,5),PartPose.ZERO);
        t.addOrReplaceChild("belt",box(60,96,-4,10,-2.5f,8,1,5),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            var a=t.addOrReplaceChild(side<0?"right":"left",box(32,20,-2,0,-2,4,7,4),PartPose.offset(side*6,1,0));
            a.addOrReplaceChild("pauldron",box(0,20,-3,-2,-3.5f,6,4,7),PartPose.rotation(0,0,side*.15f));
            a.addOrReplaceChild("elbow_guard",box(56,104,-2.5f,5,-2.5f,5,3,5),PartPose.ZERO);
            a.addOrReplaceChild("shoulder_trim",box(36,84,-2.5f,2,-2.5f,5,2,5),PartPose.ZERO);
            var f=a.addOrReplaceChild("forearm",box(64,20,-2,0,-2.5f,4,7,5),PartPose.offset(0,7,0));
            f.addOrReplaceChild("hand",box(96,20,-2,6,-2,4,3,4),PartPose.ZERO);
            t.addOrReplaceChild("tasset_"+side,box(80,104,-2,0,-1,4,5,2),PartPose.offsetAndRotation(side*3,12,-2.5f,-.12f,0,side*.14f));
            var leg=r.addOrReplaceChild(side<0?"leg_r":"leg_l",box(0,40,-2,0,-2,4,7,4),PartPose.offset(side*2.5f,9,0));
            var shin=leg.addOrReplaceChild("shin",box(32,40,-2,0,-2,4,7,4),PartPose.offset(0,7,0));
            shin.addOrReplaceChild("knee",box(96,40,-2.5f,-1,-3,5,3,2),PartPose.ZERO);
            shin.addOrReplaceChild("boot",box(64,40,-2.5f,5,-4,5,3,7),PartPose.ZERO);
            shin.addOrReplaceChild("toe",box(88,96,-2.5f,6,-5,5,2,3),PartPose.ZERO);
        }
        var weapon=t.addOrReplaceChild("axe",box(64,60,-1,-20,-1,2,23,2),PartPose.offset(0,7,-7.7f));
        weapon.addOrReplaceChild("hub",box(16,84,-2,-19,-2,4,5,4),PartPose.ZERO);
        weapon.addOrReplaceChild("pommel",box(0,84,-1.5f,2,-1.5f,3,3,3),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            weapon.addOrReplaceChild("blade_"+side,box(80,60,side<0?-9:2,-4,-1.5f,7,8,3),PartPose.offsetAndRotation(0,-16,0,0,0,side*.12f));
            weapon.addOrReplaceChild("edge_"+side,box(104,60,side<0?-12:9,-6,-1,3,12,2),PartPose.offsetAndRotation(0,-16,0,0,0,side*.12f));
            weapon.addOrReplaceChild("tip_"+side,box(116,60,side<0?-13:11,-4,-1,2,6,2),PartPose.offsetAndRotation(0,-16,0,0,0,side*.12f));
        }
        return LayerDefinition.create(m,128,128);
    }
    @Override public void setupAnim(BabyCompanionRenderState s){
        super.setupAnim(s);head.yRot=Mth.clamp(s.yRot,-45,45)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-30,30)*Mth.DEG_TO_RAD;
        float amount=Math.min(1,s.walkAnimationSpeed),stride=Mth.sin(s.walkAnimationPos*.55f)*amount*.5f;
        legR.xRot=stride;legL.xRot=-stride;kneeR.xRot=Math.max(0,-stride)*.5f;kneeL.xRot=Math.max(0,stride)*.5f;
        torso.y+=Mth.sin(s.ageInTicks*.065f)*.06f+Math.abs(stride)*.3f;torso.yRot=-stride*.08f;
        axe.xRot=.12f+Math.abs(stride)*.08f;
        if(s.actionAmount>0){float p=s.actionProgress,swing=Mth.sin(p*Mth.PI);
            switch(s.actionType){
                case 10,11->{float sign=s.actionType==10?1:-1;torso.yRot=sign*swing*.28f;axe.zRot=sign*swing*.4f;axe.xRot=.15f+swing*.65f;}
                case 12,14,16->{float lift=Mth.sin(Mth.clamp(p/.72f,0,1)*Mth.PI);axe.y=7-6*lift;axe.xRot=.12f+1.08f*lift;torso.xRot=swing*.1f;}
                case 13->{torso.xRot=.12f;axe.xRot=.55f;}
                case 15->{axe.y=-1-2*p;axe.xRot=.08f;head.xRot=-.15f;}
                case 3->{axe.y=4;head.xRot=.15f;}
                default->{}
            }
        }
        if(s.ritualizing){head.xRot=.25f;axe.xRot=.18f;}
        if(s.hasRedOverlay){torso.zRot=.06f;head.xRot+=.12f;}
        if(s.deathTime>0){axe.xRot=.8f;axe.y=8;}
        // Wrist joints land exactly on two different points of the same animated shaft.
        axe.z=-7.7f-1.8f*Mth.sin(axe.xRot);
        WarriorArmPose.grip(right,foreR,axe,-3,7,-1);WarriorArmPose.grip(left,foreL,axe,1,7,1);
    }
}
