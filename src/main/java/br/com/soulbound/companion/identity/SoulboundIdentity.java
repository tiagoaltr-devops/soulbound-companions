package br.com.soulbound.companion.identity;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.item.CompanionEggItem;
import br.com.soulbound.item.SoulEggIdentity;
import br.com.soulbound.registry.ModItems;
import br.com.soulbound.companion.recall.CompanionLocationTracker;
import com.mojang.brigadier.Command;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/** All authority checks live here and run on the server thread. No resurrection or evolution mechanics. */
@EventBusSubscriber(modid = SoulboundCompanions.MODID)
public final class SoulboundIdentity {
    private SoulboundIdentity() {}

    public static boolean isEgg(ItemStack stack) { return !stack.isEmpty() && stack.getItem() instanceof CompanionEggItem; }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var server = player.level().getServer();
        var data = SoulboundSavedData.get(server);
        var record = data.find(player.getUUID());
        if (record == null) record = SoulboundRecord.create(player.getUUID(), player.getName().getString());
        data.put(record.withName(player.getName().getString()));
        // Confirm the permanent identity on disk before giving its first item.
        data.flush(server);
        synchronizePlayer(player);
        deliverPending(player);
        if (data.find(player.getUUID()).eggDeliveryPending()) message(player, "inventory_full");
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        recoverExternalSlots(player, player.containerMenu);
        if (player.tickCount % 20 == 0) {
            synchronizePlayer(player);
            deliverPending(player);
        }
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) recoverExternalSlots(player, event.getContainer());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            requestEgg(player);
            synchronizePlayer(player);
            deliverPending(player);
        }
    }

    public static void synchronizePlayer(ServerPlayer player) {
        var data = SoulboundSavedData.get(player.level().getServer());
        var own = data.find(player.getUUID());
        if (own == null) return;
        boolean kept = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (!isEgg(stack)) continue;
            if (SoulEggIdentity.isBlank(stack) || SoulEggIdentity.matches(stack, own)) {
                if (kept) player.getInventory().setItem(slot, ItemStack.EMPTY);
                else { SoulEggIdentity.refresh(stack, own); kept = true; }
            } else refreshForeign(stack, data);
        }
        var carried = player.containerMenu.getCarried();
        if (isEgg(carried)) {
            if (SoulEggIdentity.isBlank(carried) || SoulEggIdentity.matches(carried, own)) {
                if (kept) player.containerMenu.setCarried(ItemStack.EMPTY);
                else SoulEggIdentity.refresh(carried, own);
            } else refreshForeign(carried, data);
        }
        player.containerMenu.broadcastChanges();
    }

    private static void refreshForeign(ItemStack stack, SoulboundSavedData data) {
        var owner = SoulEggIdentity.owner(stack);
        var record = owner == null ? null : data.find(owner);
        if (record != null && SoulEggIdentity.matches(stack, record)) SoulEggIdentity.refresh(stack, record);
    }

    private static boolean hasEgg(ServerPlayer player, SoulboundRecord record) {
        if(player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu) {
            for(var slot:player.containerMenu.slots)if(slot.container instanceof net.minecraft.world.inventory.CraftingContainer
                    && isEgg(slot.getItem()) && SoulEggIdentity.matches(slot.getItem(),record))return true;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            var stack = player.getInventory().getItem(slot);
            if (isEgg(stack) && SoulEggIdentity.matches(stack, record)) return true;
        }
        return isEgg(player.containerMenu.getCarried()) && SoulEggIdentity.matches(player.containerMenu.getCarried(), record);
    }

    private static void deliverPending(ServerPlayer player) {
        var server = player.level().getServer();
        var data = SoulboundSavedData.get(server);
        var record = data.find(player.getUUID());
        if (record == null || !record.eggDeliveryPending() || !player.isAlive()) return;
        if (!hasEgg(player, record)) {
            int slot = player.getInventory().getFreeSlot();
            if (slot < 0) return; // Never use Inventory.add's creative overflow behavior or drop into the world.
            var stack = new ItemStack(ModItems.COMPANION_EGG.get());
            SoulEggIdentity.refresh(stack, record);
            player.getInventory().setItem(slot, stack);
        }
        data.put(record.withDelivery(true, false));
        data.flush(server);
        server.getPlayerList().saveAll();
        player.containerMenu.broadcastChanges();
    }

    private static void requestEgg(ServerPlayer player) {
        var server = player.level().getServer();
        var data = SoulboundSavedData.get(server);
        var record = data.find(player.getUUID());
        if (record != null) {
            data.put(record.withDelivery(record.eggDelivered(), true));
            data.flush(server);
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("soulbound").then(Commands.literal("recover").executes(context -> {
            var player = context.getSource().getPlayerOrException();
            if (SoulboundSavedData.get(player.level().getServer()).find(player.getUUID()) == null) {
                message(player, "no_bond");
                return 0;
            }
            // Recovery reissues only an item for an existing identity, never another entity or new bond.
            requestEgg(player);
            synchronizePlayer(player);
            deliverPending(player);
            var record = SoulboundSavedData.get(player.level().getServer()).find(player.getUUID());
            message(player, record != null && !record.eggDeliveryPending() ? "egg_recovered" : "inventory_full");
            return Command.SINGLE_SUCCESS;
        })));
    }

    /** Normalization is permitted only against an already established canonical identity. */
    public static @Nullable SoulboundRecord validateEgg(ServerPlayer player, ItemStack stack) {
        var record = SoulboundSavedData.get(player.level().getServer()).find(player.getUUID());
        if (record == null) { message(player, "no_bond"); return null; }
        if (!SoulEggIdentity.isBlank(stack) && !SoulEggIdentity.matches(stack, record)) {
            message(player, "foreign_egg");
            return null;
        }
        SoulEggIdentity.refresh(stack, record);
        return record;
    }

    public static boolean isCanonical(LupusCompanionEntity companion) {
        if (!(companion.level() instanceof ServerLevel level) || companion.getOwnerUuid() == null) return false;
        var record = SoulboundSavedData.get(level.getServer()).find(companion.getOwnerUuid());
        return record != null && record.companionStatus() == CompanionStatus.ACTIVE
                && !record.resurrectionRequired()
                && record.matches(companion.getOwnerUuid(), companion.getBondUuid(), companion.getUUID());
    }

    public static void behaviorChanged(LupusCompanionEntity companion) {
        if (!isCanonical(companion)) return;
        var server = ((ServerLevel) companion.level()).getServer();
        var data = SoulboundSavedData.get(server);
        br.com.soulbound.companion.CompanionEquipment.capture(companion);var record = data.find(companion.getOwnerUuid());
        data.put(record.withState(record.companionStatus(), record.resurrectionRequired(), companion.getBehaviorMode().name()));
        var owner = server.getPlayerList().getPlayer(record.ownerUuid());
        if (owner != null) synchronizePlayer(owner);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LupusCompanionEntity companion) || !(event.getLevel() instanceof ServerLevel level)) return;
        var server = level.getServer();
        var owner = companion.getOwnerUuid();
        if (owner == null) { event.setCanceled(true); return; }
        var data = SoulboundSavedData.get(server);
        var record = data.find(owner);
        // Migrate only old entities read from disk. The first accepted legacy entity wins, across all dimensions.
        if (event.loadedFromDisk() && companion.getBondUuid() == null
                && (record == null || record.companionStatus() == CompanionStatus.READY)) {
            if (record == null) record = SoulboundRecord.create(owner, owner.toString());
            record = record.adoptLegacy(companion.getUUID(), companion.getEvolutionStage().name(), companion.getBehaviorMode().name());
            data.put(record);
            data.flush(server);
            companion.applyIdentity(record);
        }
        boolean accepted = record != null && record.matches(owner, companion.getBondUuid(), companion.getUUID())
                && (record.companionStatus() == CompanionStatus.ACTIVE || record.companionStatus() == CompanionStatus.SUMMONING)
                && record.species().equals("LUPUS") && java.util.Set.of("BABY","JUNIOR","PLENO").contains(record.evolutionStage()) && !record.resurrectionRequired();
        if (accepted) {
            for (var dimension : server.getAllLevels()) {
                var existing = dimension.getEntity(companion.getUUID());
                if (existing != null && existing != companion && !existing.isRemoved()) { accepted = false; break; }
            }
        }
        if (!accepted) {
            event.setCanceled(true);
            LogUtils.getLogger().warn("Rejected noncanonical Lupus {} for owner {}", companion.getUUID(), owner);
            return;
        }
        companion.applyIdentity(record);
        if (record.companionStatus() == CompanionStatus.SUMMONING) {
            data.put(record.withState(CompanionStatus.ACTIVE, false, record.behaviorMode()));
            data.flush(server);
        }
        CompanionLocationTracker.update(companion);
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LupusCompanionEntity companion) || !(event.getLevel() instanceof ServerLevel level)
                || companion.getOwnerUuid() == null) return;
        var data = SoulboundSavedData.get(level.getServer());
        br.com.soulbound.companion.CompanionEquipment.capture(companion);var record = data.find(companion.getOwnerUuid());
        if (record == null || !record.matches(companion.getOwnerUuid(), companion.getBondUuid(), companion.getUUID())) return;
        record = record.withRecall(record.recall().at(level.dimension(), companion.position(), level.getServer().overworld().getGameTime()));
        data.put(record);
        var reason = companion.getRemovalReason();companion.plenoCombat().cancelAll();
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            data.put(record.withState(reason == Entity.RemovalReason.KILLED ? CompanionStatus.DEAD : CompanionStatus.MISSING,
                    reason == Entity.RemovalReason.KILLED, record.behaviorMode()));
            data.flush(level.getServer());
            var owner = level.getServer().getPlayerList().getPlayer(record.ownerUuid());
            if (owner != null) synchronizePlayer(owner);
        }
        // Chunk unloading, logout, shutdown and dimension changes never release the reservation.
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        var stack = event.getItemEntity().getItem();
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isEgg(stack)) return;
        var record = SoulboundSavedData.get(player.level().getServer()).find(player.getUUID());
        if (record == null || (!SoulEggIdentity.isBlank(stack) && !SoulEggIdentity.matches(stack, record))) {
            event.setCanPickup(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !isEgg(event.getEntity().getItem())) return;
        parkEgg(player.level().getServer(), player, event.getEntity().getItem());
        event.setCanceled(true); // Toss already removed the item: persistent pending delivery replaces it safely.
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            var item = iterator.next();
            if (isEgg(item.getItem()) && parkEgg(player.level().getServer(), player, item.getItem())) iterator.remove();
        }
    }

    private static boolean parkEgg(MinecraftServer server, ServerPlayer holder, ItemStack stack) {
        var data = SoulboundSavedData.get(server);
        var owner = SoulEggIdentity.owner(stack);
        var record = data.find(SoulEggIdentity.isBlank(stack) ? holder.getUUID() : owner);
        if (record == null || (!SoulEggIdentity.isBlank(stack) && !SoulEggIdentity.matches(stack, record))) return false;
        data.put(record.withDelivery(record.eggDelivered(), true));
        data.flush(server); // Never erase the physical copy before its recovery intent is durable.
        return true;
    }

    private static void recoverExternalSlots(ServerPlayer player, AbstractContainerMenu menu) {
        if (menu == player.inventoryMenu) return;
        boolean changed = false;
        for (var slot : menu.slots) {
            var stack = slot.getItem();
            if (slot.container != player.getInventory() && !(menu instanceof net.minecraft.world.inventory.CraftingMenu
                    && (slot.container instanceof net.minecraft.world.inventory.CraftingContainer || slot instanceof net.minecraft.world.inventory.ResultSlot)) && isEgg(stack)
                    && parkEgg(player.level().getServer(), player, stack)) {
                slot.set(ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) menu.broadcastChanges();
    }

    public static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.soulbound." + key), true);
    }
}
