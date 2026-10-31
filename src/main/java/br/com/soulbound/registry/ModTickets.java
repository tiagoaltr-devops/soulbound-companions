package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.companion.recall.RecallQueue;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.TicketType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModTickets {
    public static final DeferredRegister<TicketType> TICKETS = DeferredRegister.create(Registries.TICKET_TYPE, SoulboundCompanions.MODID);
    // Nonpersistent, expires as a fallback; FULL/trackable entities are sufficient, no entity simulation required.
    public static final DeferredHolder<TicketType, TicketType> SOUL_RECALL = TICKETS.register("soul_recall",
            () -> new TicketType(RecallQueue.TIMEOUT_TICKS + 40L, TicketType.FLAG_LOADING | TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    private ModTickets() {}
}
