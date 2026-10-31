package br.com.soulbound.item;

import br.com.soulbound.companion.identity.CompanionStatus;
import net.minecraft.resources.Identifier;

/** Cosmetic projection only. Neither this enum nor ITEM_MODEL grants command authority. */
public enum SoulItemForm {
    EGG("companion_egg", "item.soulbound.companion_egg"),
    STAFF("soul_staff", "item.soulbound.soul_staff"),
    SHATTERED_EGG("shattered_egg", "item.soulbound.shattered_egg"),
    INACTIVE_STAFF("inactive_staff", "item.soulbound.inactive_staff");

    private final Identifier model;
    private final String nameKey;

    SoulItemForm(String model, String nameKey) {
        this.model = Identifier.fromNamespaceAndPath("soulbound", model);
        this.nameKey = nameKey;
    }

    public Identifier model() { return model; }
    public String nameKey() { return nameKey; }

    public static SoulItemForm forStatus(CompanionStatus status) {
        return switch (status) {
            case READY, SUMMONING -> EGG;
            case ACTIVE -> STAFF;
            case DEAD -> SHATTERED_EGG;
            case MISSING -> INACTIVE_STAFF;
        };
    }
}
