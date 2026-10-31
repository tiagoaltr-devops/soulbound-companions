package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.item.CompanionEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final java.util.Map<String, DeferredItem<net.minecraft.world.item.Item>> ALTAR_ITEMS = new java.util.LinkedHashMap<>();
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SoulboundCompanions.MODID);
    public static final DeferredItem<CompanionEggItem> COMPANION_EGG =
            ITEMS.registerItem("companion_egg", CompanionEggItem::new);
    static {
        for (String id : new String[]{"ancestral_spark","reactivation_essence","draconic_essence","awakening_core","ascension_core","divine_core","restoration_core","charged_core"})
            ALTAR_ITEMS.put(id, ITEMS.registerSimpleItem(id, p -> p.stacksTo(id.equals("charged_core") || id.equals("ancestral_spark") ? 1 : 64)));
    }
    public static final DeferredItem<br.com.soulbound.ritual.ForgeBlockItem> SOUL_FORGE = ITEMS.registerItem("soul_forge", p -> new br.com.soulbound.ritual.ForgeBlockItem(ModAltar.FORGE.get(),p.stacksTo(1)));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> OFFERING_ALTAR = ITEMS.registerSimpleBlockItem(ModAltar.OFFERING);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> XP_PLATFORM = ITEMS.registerSimpleBlockItem(ModAltar.PLATFORM);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> RITUAL_STONE = ITEMS.registerSimpleBlockItem(ModAltar.STONE);

    private ModItems() {}
}
