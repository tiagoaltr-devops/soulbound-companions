package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.item.CompanionEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SoulboundCompanions.MODID);
    public static final DeferredItem<CompanionEggItem> COMPANION_EGG =
            ITEMS.registerItem("companion_egg", CompanionEggItem::new);

    private ModItems() {}
}
