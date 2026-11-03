package br.com.soulbound.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import br.com.soulbound.companion.identity.CompanionStatus;

/** Small clientbound payload: no per-frame requests and no client-supplied status changes. */
public record CompanionHudPayload(CompanionHudData data) implements CustomPacketPayload {
    public static final Type<CompanionHudPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","companion_hud"));
    public static final StreamCodec<FriendlyByteBuf,CompanionHudPayload> CODEC=StreamCodec.of((buf,payload)->{
        var d=payload.data;buf.writeBoolean(d.present());buf.writeUUID(d.owner());buf.writeUUID(d.bond());buf.writeUtf(d.name(),128);buf.writeUtf(d.form(),32);
        buf.writeEnum(d.status());buf.writeVarInt(d.level());buf.writeVarLong(d.xp());buf.writeVarLong(d.required());buf.writeBoolean(d.healthKnown());buf.writeFloat(d.health());buf.writeFloat(d.maxHealth());buf.writeFloat(d.shield());buf.writeFloat(d.maxShield());buf.writeVarInt(d.godSeconds());buf.writeVarInt(d.godCooldownSeconds());
    },buf->new CompanionHudPayload(new CompanionHudData(buf.readBoolean(),buf.readUUID(),buf.readUUID(),buf.readUtf(128),buf.readUtf(32),buf.readEnum(CompanionStatus.class),
        buf.readVarInt(),buf.readVarLong(),buf.readVarLong(),buf.readBoolean(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readVarInt(),buf.readVarInt())));
    @Override public Type<CompanionHudPayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){event.registrar("2").playToClient(TYPE,CODEC);}
}
