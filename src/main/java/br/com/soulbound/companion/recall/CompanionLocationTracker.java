package br.com.soulbound.companion.recall;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.identity.SoulboundSavedData;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Mutates SavedData in memory. Normal autosaves persist it; no per-tick synchronous disk IO. */
@EventBusSubscriber(modid = SoulboundCompanions.MODID)
public final class CompanionLocationTracker {
    private CompanionLocationTracker() {}

    public static void update(LupusCompanionEntity companion) {
        if (!(companion.level() instanceof ServerLevel level) || !level.getServer().isSameThread()
                || !SoulboundIdentity.isCanonical(companion)) return;
        var data = SoulboundSavedData.get(level.getServer());
        var record = data.find(companion.getOwnerUuid());
        data.put(record.withRecall(record.recall().at(level.dimension(), companion.position(), level.getServer().overworld().getGameTime())));
    }

    @SubscribeEvent
    public static void onSave(LevelEvent.Save event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (var entity : level.getAllEntities()) if (entity instanceof LupusCompanionEntity companion) update(companion);
        // Level save may occur after the Overworld's SavedData pass; queue the final positions without joining IO.
        level.getServer().overworld().getDataStorage().scheduleSave();
    }
}
