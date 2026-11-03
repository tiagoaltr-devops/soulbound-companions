package br.com.soulbound.client;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Real client mesh and poses without a game window. Does not judge visual quality. */
public final class GodVisualChecks {
    private static int count;
    private static void check(boolean value){count++;if(!value)throw new AssertionError("God visual check "+count);}
    private static Quaternionf rotation(ModelPart p){return new Quaternionf().rotationZYX(p.zRot,p.yRot,p.xRot);}
    private static void hands(ModelPart torso){
        var axe=torso.getChild("axe");
        for(int side:new int[]{-1,1}){var arm=torso.getChild(side<0?"right":"left");var lower=arm.getChild("forearm");
            var wrist=new Vector3f(0,7,0).rotate(rotation(lower)).add(lower.x,lower.y,lower.z).rotate(rotation(arm)).add(arm.x,arm.y,arm.z);
            var goal=new Vector3f(0,side<0?-3:1,0).rotate(rotation(axe)).add(axe.x,axe.y,axe.z);check(wrist.distance(goal)<.005f);
        }
    }
    public static void run()throws Exception{
        count=0;var root=GodPrimeModel.createBodyLayer().bakeRoot();var model=new GodPrimeModel(root);var torso=root.getChild("torso");
        check(root.getAllParts().size()>40);torso.getChild("head").getChild("diadem");root.getChild("east_sigil");root.getChild("west_sigil");
        for(int action:new int[]{3,10,11,12,13,14,15,16})for(int step=0;step<=20;step++){
            var state=new BabyCompanionRenderState();state.god=true;state.ageInTicks=step;state.walkAnimationPos=step;state.walkAnimationSpeed=1;state.actionType=action;state.actionAmount=1;state.actionProgress=step/20f;
            model.setupAnim(state);hands(torso);for(var part:root.getAllParts())if(!Float.isFinite(part.xRot+part.yRot+part.zRot))throw new AssertionError("Invalid God pose");
        }
        var idle=new BabyCompanionRenderState();model.setupAnim(idle);float y=torso.y;model.setupAnim(idle);check(torso.y==y);
        idle.deathTime=15;model.setupAnim(idle);hands(torso);
        try(var in=GodVisualChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_god_prime.png")){check(in!=null);var image=javax.imageio.ImageIO.read(in);check(image.getWidth()==128 && image.getHeight()==128);}
        System.out.println("God visual checks passed: "+count);
    }
}
