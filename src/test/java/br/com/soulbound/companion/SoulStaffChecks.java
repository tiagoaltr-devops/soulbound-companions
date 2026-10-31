package br.com.soulbound.companion;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.imageio.ImageIO;
import br.com.soulbound.companion.identity.CompanionStatus;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.item.SoulItemForm;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Checks policy and the actual item asset graph without claiming to simulate login or menu packets. */
public final class SoulStaffChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        var initial = SoulboundRecord.create(UUID.randomUUID(), "Owner");
        var active = new SoulboundRecord(initial.ownerUuid(), initial.ownerLastKnownName(), initial.bondUuid(),
                initial.companionUuid(), "LUPUS", "BABY", "FOLLOW_PASSIVE", CompanionStatus.ACTIVE,
                false, 73.5, 987654321L, true, false);
        require(SoulStaffAccess.allows(active, active.ownerUuid(), active.ownerUuid(), active.bondUuid()), "The matching ACTIVE owner must be authorized");
        require(!SoulStaffAccess.allows(active, UUID.randomUUID(), active.ownerUuid(), active.bondUuid()), "Another player must be denied");
        require(!SoulStaffAccess.allows(active, active.ownerUuid(), UUID.randomUUID(), active.bondUuid()), "An egg owned by another player must be denied");
        require(!SoulStaffAccess.allows(active, active.ownerUuid(), active.ownerUuid(), UUID.randomUUID()), "Another bond must be denied");
        require(!SoulStaffAccess.allows(active, active.ownerUuid(), null, null), "A visual staff with no credentials must not authorize commands");
        require(!SoulStaffAccess.allows(null, active.ownerUuid(), active.ownerUuid(), active.bondUuid()), "A missing canonical record must be denied");
        for (var status : CompanionStatus.values()) {
            var record = active.withState(status, false, active.behaviorMode());
            require(SoulStaffAccess.allows(record, record.ownerUuid(), record.ownerUuid(), record.bondUuid()) == (status == CompanionStatus.ACTIVE), "Access must follow canonical state: " + status);
        }
        require(!SoulStaffAccess.allows(active.withState(CompanionStatus.ACTIVE, true, "STAY"), active.ownerUuid(), active.ownerUuid(), active.bondUuid()), "Resurrection-required records must not receive commands");
        var stay = active.withState(active.companionStatus(), active.resurrectionRequired(), "STAY");
        require(stay.ownerUuid().equals(active.ownerUuid()) && stay.bondUuid().equals(active.bondUuid())
                && stay.companionUuid().equals(active.companionUuid()), "A mode command must retain all identity UUIDs");
        require(stay.evolutionProgress() == active.evolutionProgress() && stay.godCooldownEnd() == active.godCooldownEnd()
                && stay.species().equals(active.species()) && stay.evolutionStage().equals(active.evolutionStage()), "A mode command must retain progression and species");
        require(SoulItemForm.forStatus(CompanionStatus.READY) == SoulItemForm.EGG, "READY must remain an egg");
        require(SoulItemForm.forStatus(CompanionStatus.SUMMONING) == SoulItemForm.EGG, "SUMMONING must remain an egg");
        require(SoulItemForm.forStatus(CompanionStatus.ACTIVE) == SoulItemForm.STAFF, "ACTIVE must project to staff");
        require(SoulItemForm.forStatus(CompanionStatus.DEAD) == SoulItemForm.SHATTERED_EGG, "DEAD must project to shattered egg");
        require(SoulItemForm.forStatus(CompanionStatus.MISSING) == SoulItemForm.INACTIVE_STAFF, "MISSING must project to inactive staff");
        for (var form : SoulItemForm.values()) {
            var definition = json("/assets/soulbound/items/" + form.model().getPath() + ".json");
            var model = definition.getAsJsonObject("model");
            require(model.get("type").getAsString().equals("minecraft:model"), "Item definition must use the modern model type");
            String modelName = model.get("model").getAsString().split(":", 2)[1];
            var geometry = json("/assets/soulbound/models/" + modelName + ".json");
            String texture = geometry.getAsJsonObject("textures").get("layer0").getAsString().split(":", 2)[1];
            try (var stream = SoulStaffChecks.class.getResourceAsStream("/assets/soulbound/textures/" + texture + ".png")) {
                require(stream != null, "Every projected model must reference a packaged original texture");
                var image = ImageIO.read(stream);
                require(image != null && image.getWidth() == 16 && image.getHeight() == 16 && image.getColorModel().hasAlpha(), "Item textures must be valid 16x16 RGBA PNGs");
            }
            for (String locale : new String[] { "pt_br", "en_us" }) {
                require(json("/assets/soulbound/lang/" + locale + ".json").has(form.nameKey()), "Every projected form must have a translated name");
            }
        }
        for (String locale : new String[] { "pt_br", "en_us" }) {
            var language = json("/assets/soulbound/lang/" + locale + ".json");
            require(language.has("menu.soulbound.follow") && language.has("menu.soulbound.stay")
                    && language.has("menu.soulbound.threat") && language.has("menu.soulbound.mineral"), "Command controls and skill indicators must be localized");
        }
        System.out.println("Soul staff checks passed: " + checks);
    }

    private static JsonObject json(String path) throws Exception {
        try (var stream = SoulStaffChecks.class.getResourceAsStream(path)) {
            if (stream == null) throw new AssertionError("Missing resource: " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
