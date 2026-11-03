package br.com.soulbound.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Original Prime silhouette: tapered armor, crown, orbiting sigils and a refined crescent axe. */
public final class GodPrimeModel extends EntityModel<BabyCompanionRenderState> {
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(Identifier.fromNamespaceAndPath("soulbound","lupus_god_prime"),"main");
    private final ModelPart torso,head,right,left,foreR,foreL,legR,legL,kneeR,kneeL,axe,east,west;
    public GodPrimeModel(ModelPart root){super(root);torso=root.getChild("torso");head=torso.getChild("head");right=torso.getChild("right");left=torso.getChild("left");foreR=right.getChild("forearm");foreL=left.getChild("forearm");legR=root.getChild("leg_r");legL=root.getChild("leg_l");kneeR=legR.getChild("shin");kneeL=legL.getChild("shin");axe=torso.getChild("axe");east=root.getChild("east_sigil");west=root.getChild("west_sigil");}
    private static CubeListBuilder box(int u,int v,float x,float y,float z,int w,int h,int d){return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);}
    public static LayerDefinition createBodyLayer(){
        var m=new MeshDefinition();var r=m.getRoot();
        var t=r.addOrReplaceChild("torso",box(32,0,-4,0,-2.5f,8,8,5),PartPose.offset(0,-5,0));
        var h=t.addOrReplaceChild("head",box(0,0,-3.5f,-7,-3.5f,7,7,7),PartPose.ZERO);
        h.addOrReplaceChild("diadem",box(0,64,-4,-7.3f,-4,8,1,8),PartPose.ZERO);
        h.addOrReplaceChild("crown_heart",box(48,64,-1,-10,-4,2,4,1),PartPose.ZERO);
        for(int side:new int[]{-1,1})h.addOrReplaceChild("crown_"+side,box(64,64,-.5f,-4,-.5f,1,4,1),PartPose.offsetAndRotation(side*3,-6,-3,0,0,side*.23f));
        t.addOrReplaceChild("heart",box(48,64,-1,2,-3.5f,2,4,1),PartPose.ZERO);
        t.addOrReplaceChild("waist",box(64,0,-3,8,-1.5f,6,5,3),PartPose.ZERO);
        t.addOrReplaceChild("hips",box(96,0,-3.5f,12,-2,7,3,4),PartPose.ZERO);
        t.addOrReplaceChild("collar",box(32,64,-3,-.5f,-3,6,1,1),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            var a=t.addOrReplaceChild(side<0?"right":"left",box(32,20,-1.5f,0,-1.5f,3,7,3),PartPose.offset(side*5.5f,1,0));
            a.addOrReplaceChild("pauldron",box(0,20,-2.5f,-1.5f,-3,5,3,6),PartPose.ZERO);
            a.addOrReplaceChild("gold_flare",box(32,64,-3,-1,-3,6,1,1),PartPose.rotation(0,0,side*.18f));
            var f=a.addOrReplaceChild("forearm",box(64,20,-1.5f,0,-2,3,7,4),PartPose.offset(0,7,0));
            f.addOrReplaceChild("hand",box(96,20,-1.5f,6,-1.5f,3,3,3),PartPose.ZERO);
            var leg=r.addOrReplaceChild(side<0?"leg_r":"leg_l",box(0,40,-1.5f,0,-1.5f,3,7,3),PartPose.offset(side*2,9,0));
            var shin=leg.addOrReplaceChild("shin",box(32,40,-1.5f,0,-1.5f,3,6,3),PartPose.offset(0,7,0));
            shin.addOrReplaceChild("knee",box(96,40,-2,-.5f,-2.5f,4,3,1),PartPose.ZERO);
            shin.addOrReplaceChild("boot",box(64,40,-2,4,-3.5f,4,3,6),PartPose.ZERO);
            t.addOrReplaceChild("hip_flare_"+side,box(96,84,-1,0,-1,2,5,2),PartPose.offsetAndRotation(side*3.5f,12,0,0,0,-side*.2f));
        }
        var w=t.addOrReplaceChild("axe",box(64,84,-1,-20,-1,2,23,2),PartPose.offset(0,6,-7.5f));
        w.addOrReplaceChild("heart",box(0,84,-1.5f,-18.5f,-1.5f,3,5,3),PartPose.ZERO);
        w.addOrReplaceChild("pommel",box(16,84,-1.5f,2,-1.5f,3,3,3),PartPose.ZERO);
        for(int side:new int[]{-1,1}){
            w.addOrReplaceChild("crescent_"+side,box(80,84,side<0?-7:2,-19,-1,5,7,2),PartPose.ZERO);
            w.addOrReplaceChild("edge_"+side,box(112,84,side<0?-9:7,-21,-1,2,11,2),PartPose.ZERO);
            w.addOrReplaceChild("tip_"+side,box(48,84,-1,-5,-1,2,5,2),PartPose.offsetAndRotation(side*7,-18,0,0,0,side*.38f));
        }
        r.addOrReplaceChild("east_sigil",box(48,64,-1,-2,-.5f,2,4,1),PartPose.offset(9,-4,3));
        r.addOrReplaceChild("west_sigil",box(48,64,-1,-2,-.5f,2,4,1),PartPose.offset(-9,-4,3));
        return LayerDefinition.create(m,128,128);
    }
    @Override public void setupAnim(BabyCompanionRenderState s){
        super.setupAnim(s);head.yRot=Mth.clamp(s.yRot,-45,45)*Mth.DEG_TO_RAD;head.xRot=Mth.clamp(s.xRot,-30,30)*Mth.DEG_TO_RAD;
        float stride=Mth.sin(s.walkAnimationPos*.62f)*Math.min(1,s.walkAnimationSpeed)*.42f;
        legR.xRot=stride;legL.xRot=-stride;kneeR.xRot=Math.max(0,-stride)*.55f;kneeL.xRot=Math.max(0,stride)*.55f;
        torso.y+=Mth.sin(s.ageInTicks*.075f)*.08f;torso.yRot=-stride*.06f;axe.xRot=.1f;
        east.y+=Mth.sin(s.ageInTicks*.07f)*.5f;west.y-=Mth.sin(s.ageInTicks*.07f)*.5f;east.yRot=s.ageInTicks*.025f;west.yRot=-east.yRot;
        if(s.actionAmount>0){float p=s.actionProgress,swing=Mth.sin(p*Mth.PI);
            switch(s.actionType){
                case 10->{axe.zRot=swing*.3f;axe.xRot=.15f+.65f*swing;torso.yRot=-swing*.18f;}
                case 11->{axe.zRot=-swing*.4f;axe.xRot=.2f+.6f*swing;torso.yRot=swing*.22f;}
                case 12,14,16->{axe.y=6-3*p-3*swing;axe.xRot=.1f+1.05f*p;torso.xRot=swing*.06f;}
                case 13->{axe.xRot=.5f;torso.xRot=.08f;}
                case 15->{axe.y=-1-2*p;axe.xRot=.06f;head.xRot=-.1f;east.y-=p*1.5f;west.y-=p*1.5f;}
                case 3->{axe.y=3;head.xRot=-swing*.12f;east.zRot=swing*.4f;west.zRot=-east.zRot;}
                default->{}
            }
        }
        if(s.ritualizing)head.xRot=.2f;
        if(s.hasRedOverlay){torso.zRot=.04f;head.xRot+=.07f;}
        if(s.deathTime>0){axe.xRot=.65f;east.y+=s.deathTime*.1f;west.y+=s.deathTime*.1f;}
        axe.z=-7.5f-1.4f*Mth.sin(axe.xRot);
        WarriorArmPose.grip(right,foreR,axe,-3,7,-1);WarriorArmPose.grip(left,foreL,axe,1,7,1);
    }
}
