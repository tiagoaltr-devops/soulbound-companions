package br.com.soulbound.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class OfferingBlock extends Block {
    public OfferingBlock(Properties p){super(p);}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(player instanceof ServerPlayer p)AltarInventory.exchange(p,pos);return InteractionResult.SUCCESS;}
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(hand==InteractionHand.MAIN_HAND && player instanceof ServerPlayer p)AltarInventory.exchange(p,pos);return InteractionResult.SUCCESS;
    }
}
