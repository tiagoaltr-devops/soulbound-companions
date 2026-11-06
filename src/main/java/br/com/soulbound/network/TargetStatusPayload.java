package br.com.soulbound.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record TargetStatusPayload(int entityId,Component name,int range) implements CustomPacketPayload {
    public static final Type<TargetStatusPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","target_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TargetStatusPayload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.entityId);ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b,p.name);b.writeVarInt(p.range);},b->new TargetStatusPayload(b.readVarInt(),ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b),b.readVarInt()));
    @Override public Type<TargetStatusPayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC);}
}
