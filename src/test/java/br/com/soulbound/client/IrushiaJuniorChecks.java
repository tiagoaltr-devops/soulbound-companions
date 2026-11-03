package br.com.soulbound.client;

/** Actual native mesh/pose checks. No window or game-world simulation. */
public final class IrushiaJuniorChecks {
    private static int count;
    private static void check(boolean value,String message){count++;if(!value)throw new AssertionError(message);}
    public static void run()throws Exception{
        count=0;var root=IrushiaJuniorModel.createBodyLayer().bakeRoot();var model=new IrushiaJuniorModel(root);
        check(root.getAllParts().size()==78,"Complete 38-bone/39-cube hierarchy plus native root");
        int[] cubes={0};root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            cubes[0]++;for(var face:cube.polygons)for(var vertex:face.vertices())
                check(Float.isFinite(vertex.u()+vertex.v()) && vertex.u()>=0 && vertex.u()<=1 && vertex.v()>=0 && vertex.v()<=1,"Irushia UV outside original atlas: "+path);
        });
        check(cubes[0]==39,"All original cubes imported exactly once");
        var lookup=root.createPartLookup();var head=lookup.apply("Head");var wing=lookup.apply("LeftWing");var leg=lookup.apply("RightLeg");
        var state=new BabyCompanionRenderState();state.ageInTicks=8;model.setupAnim(state);float idle=wing.yRot;
        state.walkAnimationSpeed=1;state.walkAnimationPos=2;model.setupAnim(state);
        check(Math.abs(leg.xRot)>.1f && wing.yRot!=idle,"Walking must animate original legs and wings");
        for(int action:new int[]{0,1,2,3})for(int step=0;step<=20;step++){
            state.actionType=action;state.actionAmount=1;state.actionProgress=step/20f;state.yRot=90;state.xRot=60;
            model.setupAnim(state);for(var part:root.getAllParts())check(Float.isFinite(part.xRot+part.yRot+part.zRot+part.x+part.y+part.z),"Finite native pose");
        }
        state.actionAmount=0;model.setupAnim(state);float angle=head.xRot;model.setupAnim(state);check(head.xRot==angle,"Poses reset instead of accumulating");
        state.ritualizing=true;model.setupAnim(state);state.deathTime=10;model.setupAnim(state);
        try(var stream=IrushiaJuniorChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_junior_irushia.png")){
            check(stream!=null,"Imported PNG packaged");var image=javax.imageio.ImageIO.read(stream);check(image.getWidth()==64 && image.getHeight()==64,"Original texture dimensions");
        }
        try(var stream=IrushiaJuniorChecks.class.getResourceAsStream("/assets/soulbound/credits/irushia.txt")){
            check(stream!=null,"Attribution packaged");var credit=new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            check(credit.contains("kyoukster") && credit.contains("https://creativecommons.org/licenses/by/4.0/"),"Creator and license link preserved");
        }
        System.out.println("Irushia Junior checks passed: "+count);
    }
}
