package br.com.soulbound.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** No client-supplied owner, damage, position or target list. */
public record JuniorUltimatePayload() implements CustomPacketPayload {
    public static final JuniorUltimatePayload INSTANCE=new JuniorUltimatePayload();
    public static final Type<JuniorUltimatePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","junior_ultimate"));
    public static final StreamCodec<RegistryFriendlyByteBuf,JuniorUltimatePayload> CODEC=StreamCodec.unit(INSTANCE);
    @Override public Type<JuniorUltimatePayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
        event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->{
            if(context.player() instanceof net.minecraft.server.level.ServerPlayer p)br.com.soulbound.companion.CompanionFormService.ultimate(p);
        });
    }
}
