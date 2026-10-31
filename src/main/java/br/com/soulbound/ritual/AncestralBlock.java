package br.com.soulbound.ritual;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class AncestralBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public AncestralBlock(Properties properties) { super(properties); }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AncestralPedestalBlockEntity(pos,state);}
    @Override protected void affectNeighborsAfterRemoval(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,boolean moved){
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if(level.isClientSide() || !level.getServer().isSameThread())return;
        var data=AltarSavedData.get(level.getServer());
        for(var r:data.state.ruins.values())if(r.x==pos.getX() && r.y==pos.getY() && r.z==pos.getZ() && r.dimension.equals(level.dimension().identifier().toString())){r.integrity="PERMANENTLY_INVALID";data.setDirty();}
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) TrialService.interact(serverPlayer, pos);
        return InteractionResult.SUCCESS;
    }
}
