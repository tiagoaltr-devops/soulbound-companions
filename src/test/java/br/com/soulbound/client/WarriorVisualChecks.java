package br.com.soulbound.client;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Bakes actual meshes and samples actual poses, without a game window or GPU. */
public final class WarriorVisualChecks {
    private static int checks;
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static Quaternionf rotation(ModelPart p){return new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot);}
    private static void grip(ModelPart torso,String arm,int side){
        var upper=torso.getChild(arm);var fore=upper.getChild("forearm");var axe=torso.getChild("axe");
        var wrist=new Vector3f(0,7,0).rotate(rotation(fore)).add(fore.x,fore.y,fore.z).rotate(rotation(upper)).add(upper.x,upper.y,upper.z);
        var target=new Vector3f(0,side<0?-3:1,0).rotate(rotation(axe)).add(axe.x,axe.y,axe.z);
        require(wrist.distance(target)<.005f,"Wrist must remain attached to animated axe: "+arm+" error "+wrist.distance(target));
    }
    private static void texture(ModelPart root,String name)throws Exception{
        try(var stream=WarriorVisualChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/"+name+".png")){
            require(stream!=null,"Missing warrior texture");var image=javax.imageio.ImageIO.read(stream);
            require(image.getWidth()==128 && image.getHeight()==128,"Warrior atlas size");
            root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
                for(var face:cube.polygons){float u=0,v=0;for(var vertex:face.vertices()){
                    require(vertex.u()>=0 && vertex.u()<=1 && vertex.v()>=0 && vertex.v()<=1,"UV outside atlas: "+path);
                    u+=vertex.u()/4;v+=vertex.v()/4;
                }
                require((image.getRGB(Math.min(127,(int)(u*128)),Math.min(127,(int)(v*128)))>>>24)==255,"Unpainted UV face: "+path);
                }
            });
        }
    }
    public static void run()throws Exception{
        checks=0;var jr=JuniorCompanionModel.createBodyLayer().bakeRoot();var jrModel=new JuniorCompanionModel(jr);
        var pl=PlenoCompanionModel.createBodyLayer().bakeRoot();var plModel=new PlenoCompanionModel(pl);var torso=pl.getChild("torso");
        require(jr.getAllParts().size()>20,"Junior must include segmented armor/joints");require(pl.getAllParts().size()>30,"Pleno must include layered armor and axe parts");
        texture(jr,"lupus_junior");texture(pl,"lupus_pleno");
        require(jr.getChild("torso").hasChild("breastplate") && torso.hasChild("tasset_1"),"Tapered chest and articulated hip armor");
        for(int action:new int[]{1,2,3,10,11,12,13,14,15,16})for(int step=0;step<=20;step++){
            var state=new BabyCompanionRenderState();state.actionAmount=1;state.actionType=action;state.actionProgress=step/20f;
            state.walkAnimationSpeed=.8f;state.walkAnimationPos=step;state.ageInTicks=step;state.yRot=75;state.xRot=60;
            jrModel.setupAnim(state);plModel.setupAnim(state);grip(torso,"right",-1);grip(torso,"left",1);
            for(var part:pl.getAllParts())if(!Float.isFinite(part.xRot+part.yRot+part.zRot))throw new AssertionError("Non-finite warrior animation");
        }
        var idle=new BabyCompanionRenderState();plModel.setupAnim(idle);float axe=torso.getChild("axe").xRot;
        plModel.setupAnim(idle);require(axe==torso.getChild("axe").xRot,"Pose must reset between frames");
        idle.ritualizing=true;plModel.setupAnim(idle);grip(torso,"right",-1);grip(torso,"left",1);
        idle.deathTime=10;plModel.setupAnim(idle);grip(torso,"right",-1);grip(torso,"left",1);
        var recovered=new BabyCompanionRenderState();recovered.actionAmount=1;recovered.actionType=12;recovered.actionProgress=1;
        plModel.setupAnim(recovered);require(Math.abs(torso.getChild("axe").xRot-axe)<.001,"Heavy attack returns to idle within action duration");
        System.out.println("Warrior visual checks passed: "+checks);
    }
}
