package br.com.soulbound;

import br.com.soulbound.client.LupusCompanionRenderer;
import br.com.soulbound.client.BabyCompanionModel;
import br.com.soulbound.registry.ModEntities;
import br.com.soulbound.registry.ModMenus;
import br.com.soulbound.client.SoulCommandScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = SoulboundCompanions.MODID, dist = Dist.CLIENT)
public class SoulboundCompanionsClient {
    public SoulboundCompanionsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerRenderers);
        modEventBus.addListener(this::registerLayers);
        modEventBus.addListener(this::registerScreens);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SOUL_COMMAND.get(), SoulCommandScreen::new);
        event.register(ModMenus.ANCESTRAL_TRIAL.get(), br.com.soulbound.client.TrialScreen::new);
        event.register(ModMenus.SOUL_FORGE.get(), br.com.soulbound.client.ForgeScreen::new);
    }

    private void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(BabyCompanionModel.LAYER, BabyCompanionModel::createBodyLayer);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LUPUS_COMPANION.get(), LupusCompanionRenderer::new);
        event.registerEntityRenderer(ModEntities.SPIRIT_GUARDIAN.get(), br.com.soulbound.client.SpiritGuardianRenderer::new);
    }
}
