package br.com.soulbound.client;

import java.util.HashSet;
import javax.imageio.ImageIO;

/** Bakes the real client model without OpenGL and checks the packaged texture and animation reset contract. */
public final class BabyVisualChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var baked = BabyCompanionModel.createBodyLayer().bakeRoot();
        var root = baked.getChild("baby");
        var head = root.getChild("head");
        var body = root.getChild("body");
        var leftWing = body.getChild("left_wing");
        var rightWing = body.getChild("right_wing");
        var leftArm = body.getChild("left_arm");
        body.getChild("right_arm");
        head.getChild("left_fin");
        head.getChild("right_fin");
        require(root.getAllParts().size() == 9, "Baby mesh must bake with all body parts");
        var model = new BabyCompanionModel(baked);
        var state = new BabyCompanionRenderState();
        state.ageInTicks = 7.5F;
        model.setupAnim(state);
        float idleWing = leftWing.yRot;
        float idleHeight = root.y;
        require(Math.abs(leftWing.yRot + rightWing.yRot) < 0.0001F, "Wings must animate symmetrically");
        state.flightAmount = 1;
        model.setupAnim(state);
        require(Math.abs(leftWing.yRot - idleWing) > 0.01F, "Flight must change the wing animation");
        state.discoveryAmount = 1;
        model.setupAnim(state);
        require(leftArm.zRot < -0.8F, "Mineral discovery must visibly raise the arms");
        state.discoveryAmount = 0;
        state.flightAmount = 0;
        model.setupAnim(state);
        require(Math.abs(root.y - idleHeight) < 0.0001F, "Animation must reset instead of accumulating transforms");
        state.xRot = 10000;
        state.yRot = -10000;
        state.alerting = true;
        state.retreating = true;
        model.setupAnim(state);
        require(Math.abs(head.xRot) <= 0.62 && Math.abs(head.yRot) <= 1.23, "Head look must stay within a reasonable range");
        for (var part : root.getAllParts()) {
            require(Float.isFinite(part.xRot) && Float.isFinite(part.yRot) && Float.isFinite(part.zRot), "Animation must remain finite");
        }
        try (var stream = BabyVisualChecks.class.getResourceAsStream("/assets/soulbound/textures/entity/lupus_baby.png")) {
            require(stream != null, "Original texture must be packaged as a resource");
            var image = ImageIO.read(stream);
            require(image != null && image.getWidth() == 64 && image.getHeight() == 64, "Texture must be a valid 64x64 PNG");
            require(image.getColorModel().hasAlpha(), "Texture must retain alpha for wing cutouts");
            var colors = new HashSet<Integer>();
            for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) colors.add(image.getRGB(x, y));
            require(colors.size() >= 7, "Texture must contain the original multicolor palette");
            require((image.getRGB(8, 8) >>> 24) == 255, "Mapped magical eyes must be opaque");
        }
        System.out.println("Baby visual checks passed: " + checks);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
