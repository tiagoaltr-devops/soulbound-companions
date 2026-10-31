package br.com.soulbound.ritual;

import br.com.soulbound.registry.ModAltar;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;

public final class ForgeBlockItem extends BlockItem {
    public ForgeBlockItem(Block b,Properties p){super(b,p);}
    @Override public InteractionResult place(BlockPlaceContext c){
        if(!(c.getPlayer() instanceof ServerPlayer p))return super.place(c);
        String id=AltarTokens.id(c.getItemInHand());var d=AltarSavedData.get(p.level().getServer());
        var f=d.state.forges.get(id);var token=d.state.tokens.get(id);
        if(f==null && (token==null || token.spent || !token.kind.equals("spark")))return InteractionResult.FAIL;
        if(f!=null && (f.placed || f.state.equals("DESTROYED") || f.owner!=null && !f.owner.equals(p.getStringUUID())))return InteractionResult.FAIL;
        var result=super.place(c);
        if(result.consumesAction() && p.level().getBlockState(c.getClickedPos()).is(ModAltar.FORGE.get())){
            if(f==null){f=new AltarSavedData.Forge();f.id=id;d.state.forges.put(id,f);token.spent=true;}
            f.dimension=p.level().dimension().identifier().toString();f.x=c.getClickedPos().getX();f.y=c.getClickedPos().getY();f.z=c.getClickedPos().getZ();f.placed=true;
            d.flush(p.level().getServer());
        }
        return result;
    }
}
