package br.com.soulbound.companion.target;

import java.util.UUID;
import net.minecraft.world.entity.Mob;

/** Minimal adapter for future companions; targeting owns no species-specific controller. */
public interface TargetCommandActor {
    Mob commandActor();
    UUID getOwnerUuid();
    UUID getBondUuid();
    boolean acceptsTargetCommand();
    void cancelTargetActions();
}
