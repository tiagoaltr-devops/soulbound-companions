package br.com.soulbound.ritual;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Pedestals can only register when they belong to a native ruin structure. */
public final class TrialService {
    private TrialService() {}
    public static void interact(ServerPlayer player, BlockPos pos) {
        var level=player.level();
        var data=AltarSavedData.get(level.getServer());
        for(var r:data.state.ruins.values())if(r.x==pos.getX() && r.y==pos.getY() && r.z==pos.getZ() && r.dimension.equals(level.dimension().identifier().toString())
                && "NATURAL_WORLDGEN".equals(r.origin) && !"PERMANENTLY_INVALID".equals(r.integrity) && RuinIndex.validateLoaded(level,r)){
            TrialEngine.open(player,r);return;
        }
        var source=level.getBlockEntity(pos) instanceof AncestralPedestalBlockEntity be && be.matches(be.naturalId())?be.source():new net.minecraft.world.level.ChunkPos(pos.offset(-32,0,0));
        RuinIndex.verifySource(level,source).whenComplete((proof,error)->level.getServer().execute(()->{
            if(!player.isAlive() || player.hasDisconnected() || player.level()!=level || player.distanceToSqr(pos.getCenter())>64)return;
            if(error!=null || proof.isEmpty() || !proof.get().center().equals(pos)){
                player.displayClientMessage(Component.translatable("message.soulbound.ruin_not_natural"),false);return;
            }
            RuinIndex.accept(level,proof.get());var r=data.state.ruins.get(proof.get().id());
            if(r!=null && !"PERMANENTLY_INVALID".equals(r.integrity))TrialEngine.open(player,r);
        }));
    }
}
