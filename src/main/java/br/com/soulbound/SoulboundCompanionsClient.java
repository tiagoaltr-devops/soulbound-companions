package br.com.soulbound;

import br.com.soulbound.client.LupusCompanionRenderer;
import br.com.soulbound.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = SoulboundCompanions.MODID, dist = Dist.CLIENT)
public class SoulboundCompanionsClient {
    public SoulboundCompanionsClient(IEventBus modEventBus) {
        modEventBus.addListener(this::registerRenderers);
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.LUPUS_COMPANION.get(), LupusCompanionRenderer::new);
    }
}
