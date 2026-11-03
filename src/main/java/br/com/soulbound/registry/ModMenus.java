package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.menu.SoulCommandMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SoulboundCompanions.MODID);
    public static final DeferredHolder<MenuType<?>, MenuType<SoulCommandMenu>> SOUL_COMMAND =
            MENUS.register("soul_command", () -> new MenuType<>(SoulCommandMenu::new, FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>,MenuType<br.com.soulbound.menu.CompanionInventoryMenu>> COMPANION_INVENTORY=MENUS.register("companion_inventory",()->new MenuType<>(br.com.soulbound.menu.CompanionInventoryMenu::new,FeatureFlags.VANILLA_SET));
    private ModMenus() {}
    public static final DeferredHolder<MenuType<?>,MenuType<br.com.soulbound.menu.TalentMenu>> TALENTS=MENUS.register("talents",()->new MenuType<>(br.com.soulbound.menu.TalentMenu::new,FeatureFlags.VANILLA_SET));
    public static final DeferredHolder<MenuType<?>, MenuType<br.com.soulbound.ritual.ForgeMenu>> SOUL_FORGE =
            MENUS.register("soul_forge", () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                    (id,inv,data)->new br.com.soulbound.ritual.ForgeMenu(id,inv,data)));
    public static final DeferredHolder<MenuType<?>, MenuType<br.com.soulbound.ritual.TrialMenu>> ANCESTRAL_TRIAL =
            MENUS.register("ancestral_trial", () -> new MenuType<>(br.com.soulbound.ritual.TrialMenu::new, FeatureFlags.VANILLA_SET));
}
