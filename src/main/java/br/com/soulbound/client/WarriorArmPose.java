package br.com.soulbound.client;

import net.minecraft.client.model.geom.ModelPart;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Client-only two-bone pose: wrists follow the integrated weapon's actual transform. */
final class WarriorArmPose {
    private WarriorArmPose() {}
    static void grip(ModelPart upper,ModelPart forearm,ModelPart weapon,float gripY,float length,int side){
        var rotation=new Quaternionf().rotationZYX(weapon.zRot,weapon.yRot,weapon.xRot);
        var goal=new Vector3f(0,gripY,0).rotate(rotation).add(weapon.x,weapon.y,weapon.z);
        var origin=new Vector3f(upper.x,upper.y,upper.z);var delta=new Vector3f(goal).sub(origin);
        float distance=Math.min(length*2-.001f,Math.max(.001f,delta.length()));
        var direction=new Vector3f(delta).normalize();
        // Bend toward the outside, away from the chest. The elbow stays in the shoulder/grip plane.
        var bend=new Vector3f(side,0,0);bend.sub(new Vector3f(direction).mul(bend.dot(direction))).normalize();
        var elbow=new Vector3f(origin).fma(distance*.5f,direction).fma((float)Math.sqrt(length*length-distance*distance*.25f),bend);
        aim(upper,new Vector3f(elbow).sub(origin));
        var local=new Vector3f(goal).sub(elbow).rotate(new Quaternionf().rotationZYX(upper.zRot,upper.yRot,upper.xRot).conjugate());
        aim(forearm,local);
    }
    private static void aim(ModelPart part,Vector3f delta){
        part.yRot=0;part.xRot=(float)Math.atan2(delta.z,Math.hypot(delta.x,delta.y));part.zRot=(float)Math.atan2(-delta.x,delta.y);
    }
}
