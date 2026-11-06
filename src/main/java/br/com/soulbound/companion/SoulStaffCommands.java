package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.identity.SoulboundRecord;
import br.com.soulbound.companion.identity.SoulboundSavedData;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.item.SoulEggIdentity;
import br.com.soulbound.menu.SoulCommandMenu;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** The air-use and menu-button paths share the same server authority checks. */
public final class SoulStaffCommands {
    private SoulStaffCommands() {}

    public static @Nullable SoulboundRecord authorize(ServerPlayer player, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator() || !SoulboundIdentity.isEgg(stack)) return null;
        var record = SoulboundSavedData.get(player.level().getServer()).find(player.getUUID());
        return SoulStaffAccess.allows(record, player.getUUID(), SoulEggIdentity.owner(stack), SoulEggIdentity.bond(stack)) ? record : null;
    }

    public static @Nullable LupusCompanionEntity loadedCompanion(ServerPlayer player, SoulboundRecord record) {
        var entity = player.level().getEntity(record.companionUuid());
        return entity instanceof LupusCompanionEntity companion && companion.isAlive()
                && record.matches(companion.getOwnerUuid(), companion.getBondUuid(), companion.getUUID())
                && SoulboundIdentity.isCanonical(companion) ? companion : null;
    }

    public static boolean setMode(ServerPlayer player, ItemStack stack, SoulboundRecord session, BehaviorMode mode) {
        var record = authorize(player, stack);
        if (record == null || !record.matches(session.ownerUuid(), session.bondUuid(), session.companionUuid())) {
            SoulboundIdentity.message(player, "staff_denied");
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) return false;
        var companion = loadedCompanion(player, record);
        if (companion == null) {
            SoulboundIdentity.message(player, "staff_unloaded");
            return false;
        }
        br.com.soulbound.companion.target.TargetCommandService.cancel(player);
        if (companion.getBehaviorMode() == mode) return true; // Idempotent: do not reset Stay's anchor.
        if (!companion.commandBehaviorMode(player, mode)) return false;
        player.getCooldowns().addCooldown(stack, 6);
        player.displayClientMessage(Component.translatable("message.soulbound.mode",
                Component.translatable("behavior.soulbound." + mode.name().toLowerCase(java.util.Locale.ROOT))), true);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.5F, mode == BehaviorMode.STAY ? 0.8F : 1.3F);
        var level = (net.minecraft.server.level.ServerLevel) companion.level();
        level.sendParticles(ParticleTypes.END_ROD, companion.getX(), companion.getY() + 0.5, companion.getZ(),
                5, 0.2, 0.2, 0.2, 0.02);
        return true;
    }

    public static SoulboundRecord information(ServerPlayer player,ItemStack stack){
        if(!player.isAlive() || player.isSpectator() || !SoulboundIdentity.isEgg(stack))return null;
        var r=SoulboundSavedData.get(player.level().getServer()).find(player.getUUID());return r!=null && SoulEggIdentity.matches(stack,r)?r:null;
    }
    public static void openMenu(ServerPlayer player, InteractionHand hand) {
        var record = information(player, player.getItemInHand(hand));
        if (record == null) { SoulboundIdentity.message(player, "staff_denied"); return; }
        var companion = loadedCompanion(player, record);
        Component name = companion != null ? companion.getDisplayName() : Component.translatable("entity.soulbound.lupus_companion");
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) -> new SoulCommandMenu(id, inventory, hand, record), name));
    }
}
