package br.com.soulbound.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record TargetCommandPayload(int entityId) implements CustomPacketPayload {
    public static final Type<TargetCommandPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","target_command"));
    public static final StreamCodec<FriendlyByteBuf,TargetCommandPayload> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.entityId),b->new TargetCommandPayload(b.readVarInt()));
    @Override public Type<TargetCommandPayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,CODEC,(p,c)->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)br.com.soulbound.companion.target.TargetCommandService.request(player,p.entityId);});}
}
