package br.com.soulbound.item;

import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.gameevent.GameEvent;

/** Player-only summoning item: never configures spawners or accepts entity NBT from the stack. */
public class CompanionEggItem extends Item {
    public CompanionEggItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        var level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                ? clicked : clicked.relative(context.getClickedFace());
        if (!level.mayInteract(player, clicked) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;
        LupusCompanionEntity companion = ModEntities.LUPUS_COMPANION.get().create(serverLevel, EntitySpawnReason.SPAWN_ITEM_USE);
        if (companion == null) return InteractionResult.FAIL;
        companion.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot(), 0.0F);
        if (!serverLevel.getWorldBorder().isWithinBounds(companion.getBoundingBox())
                || !serverLevel.noCollision(companion)
                || !serverLevel.getBlockState(pos.below()).isFaceSturdy(serverLevel, pos.below(), net.minecraft.core.Direction.UP)) {
            return InteractionResult.FAIL;
        }
        companion.assignOwner(player);
        if (!serverLevel.addFreshEntity(companion)) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return InteractionResult.SUCCESS;
    }
}
