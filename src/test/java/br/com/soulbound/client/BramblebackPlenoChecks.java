package br.com.soulbound.client;

public final class BramblebackPlenoChecks {
    private static int count;
    private static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void run()throws Exception{
        count=0;var root=BramblebackPlenoModel.createBodyLayer().bakeRoot();var model=new BramblebackPlenoModel(root);
        check(root.getAllParts().size()==84,"58 cubes and 25 visual bones plus root");
        check(root.createPartLookup().apply("hitbox")==null,"Editor hitbox must not render");
        int[] cubes={0};root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            cubes[0]++;for(var face:cube.polygons)for(var vertex:face.vertices())check(Float.isFinite(vertex.u()+vertex.v()) && vertex.u()>=0 && vertex.u()<=1 && vertex.v()>=0 && vertex.v()<=1,"Invalid Brambleback UV: "+path);
        });check(cubes[0]==58,"All visual cubes imported");
        var state=new BabyCompanionRenderState();var arm=root.createPartLookup().apply("R_ARM");model.setupAnim(state);float idle=arm.xRot;
        state.walkAnimationSpeed=1;state.walkAnimationPos=2;model.setupAnim(state);check(arm.xRot!=idle,"Walking animates arms");
        for(int action:new int[]{3,10,11,12,13,14,15,16})for(int step=0;step<=20;step++){
            state.actionAmount=1;state.actionType=action;state.actionProgress=step/20f;model.setupAnim(state);
            for(var part:root.getAllParts())check(Float.isFinite(part.xRot+part.yRot+part.zRot+part.x+part.y+part.z),"Finite Brambleback pose");
        }
        state.actionAmount=0;model.setupAnim(state);float a=arm.xRot;model.setupAnim(state);check(arm.xRot==a,"Pose resets each frame");
        state.ritualizing=true;model.setupAnim(state);state.deathTime=10;model.setupAnim(state);
        try(var stream=BramblebackPlenoChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_pleno_brambleback.png")){
            check(stream!=null,"Texture packaged");var image=javax.imageio.ImageIO.read(stream);check(image.getWidth()==128 && image.getHeight()==128,"Original PNG dimensions");
        }
        try(var stream=BramblebackPlenoChecks.class.getResourceAsStream("/assets/soulbound/credits/brambleback.txt")){
            check(stream!=null,"Credits packaged");var text=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);check(text.contains("monkus") && text.contains("https://creativecommons.org/licenses/by-sa/4.0/"),"Author and ShareAlike terms");
        }
        System.out.println("Brambleback Pleno checks passed: "+count);
    }
}
