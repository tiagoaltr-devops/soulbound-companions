package br.com.soulbound;

import br.com.soulbound.registry.ModEntities;
import br.com.soulbound.registry.ModItems;
import br.com.soulbound.registry.ModMenus;
import br.com.soulbound.registry.ModTickets;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(SoulboundCompanions.MODID)
public class SoulboundCompanions {
    public static final String MODID = "soulbound";

    public SoulboundCompanions(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        ModEntities.ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModTickets.TICKETS.register(modEventBus);
        br.com.soulbound.registry.ModAltar.register(modEventBus);
        modEventBus.addListener(ModEntities::registerAttributes);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(br.com.soulbound.network.JuniorUltimatePayload::register);
        modEventBus.addListener(br.com.soulbound.network.TargetCommandPayload::register);
        modEventBus.addListener(br.com.soulbound.network.TargetStatusPayload::register);
        modEventBus.addListener(br.com.soulbound.network.GrimoirePayload::register);
        modEventBus.addListener(br.com.soulbound.network.GrimoireOpenPayload::register);
        modEventBus.addListener(br.com.soulbound.network.CompanionHudPayload::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(ModItems.COMPANION_EGG);
        if(event.getTabKey()==CreativeModeTabs.INGREDIENTS) {
            ModItems.ALTAR_ITEMS.values().forEach(event::accept);
            event.accept(ModItems.SOUL_FORGE);event.accept(ModItems.OFFERING_ALTAR);event.accept(ModItems.XP_PLATFORM);event.accept(ModItems.RITUAL_STONE);
        }
    }
}
