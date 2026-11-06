package br.com.soulbound.item;

import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.companion.identity.CompanionStatus;
import br.com.soulbound.companion.identity.SoulboundIdentity;
import br.com.soulbound.companion.identity.SoulboundSavedData;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.SoulStaffCommands;
import br.com.soulbound.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/** Player-only summoning item: never configures spawners or accepts entity NBT from the stack. */
public class CompanionEggItem extends Item {
    @Override public void onCraftedBy(ItemStack stack,Player player) {
        super.onCraftedBy(stack,player);
        if(player instanceof ServerPlayer p)br.com.soulbound.ritual.CompanionEvolution.applyCore(p,stack);
    }
    public CompanionEggItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) { return false; }

    @Override
    public boolean canFitInsideContainerItems() { return false; }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) { return false; }

    @Override
    public boolean overrideStackedOnOther(ItemStack stack, Slot slot, ClickAction action, Player player) {
        return slot.container != player.getInventory() && !personalCraftingSlot(player,slot);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        return slot.container != player.getInventory() && !personalCraftingSlot(player,slot);
    }
    public static boolean personalCraftingSlot(Player player,Slot slot) {
        return player.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu
                && slot.container instanceof net.minecraft.world.inventory.CraftingContainer;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (SoulEggIdentity.isBlank(stack)) tooltip.accept(Component.translatable("tooltip.soulbound.awaiting_identity"));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        var record = SoulboundIdentity.validateEgg(serverPlayer, player.getItemInHand(hand));
        if (record == null) return InteractionResult.FAIL;
        return record.canSummon()?InteractionResult.PASS:command(serverPlayer,hand,record);
    }

    private InteractionResult command(ServerPlayer player,InteractionHand hand,br.com.soulbound.companion.identity.SoulboundRecord record) {
        SoulStaffCommands.openMenu(player,hand);return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        var level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(context.getClickedFace());
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        var record = SoulboundIdentity.validateEgg(serverPlayer, context.getItemInHand());
        if (record == null) return InteractionResult.FAIL;
        if (!record.canSummon()) return command(serverPlayer, context.getHand(), record);
        if (!level.mayInteract(player, clicked) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        for (var dimension : serverLevel.getServer().getAllLevels()) {
            if (dimension.getEntity(record.companionUuid()) != null) {
                SoulboundIdentity.message(serverPlayer, "companion_unavailable");
                return InteractionResult.FAIL;
            }
        }
        LupusCompanionEntity companion = ModEntities.LUPUS_COMPANION.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (companion == null) return InteractionResult.FAIL;
        companion.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0.0F);
        if (!serverLevel.getWorldBorder().isWithinBounds(companion.getBoundingBox())
                || !serverLevel.noCollision(companion)
                || !serverLevel.getBlockState(pos.below()).isFaceSturdy(serverLevel, pos.below(), net.minecraft.core.Direction.UP)) {
            return InteractionResult.FAIL;
        }
        var data = SoulboundSavedData.get(serverLevel.getServer());
        companion.setUUID(record.companionUuid());
        companion.applyIdentity(record);
        // Persist the reservation before the entity can enter any dimension. All use runs serially on the server.
        data.put(record.withState(CompanionStatus.SUMMONING, false, record.behaviorMode()));
        data.flush(serverLevel.getServer());
        if (!serverLevel.addFreshEntity(companion)) {
            data.put(record);
            data.flush(serverLevel.getServer());
            return InteractionResult.FAIL;
        }
        SoulEggIdentity.refresh(context.getItemInHand(), data.find(player.getUUID()));
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }
}
