package br.com.soulbound.network;

import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Read-only projection, scoped to one open menu. Contains no inventory snapshots or client mutations. */
public record GrimoirePayload(int menu,List<Component> overview,List<Component> abilities,List<Component> stats,List<Component> bond) implements CustomPacketPayload {
    public static final Type<GrimoirePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","grimoire"));
    public GrimoirePayload {overview=List.copyOf(overview);abilities=List.copyOf(abilities);stats=List.copyOf(stats);bond=List.copyOf(bond);}
    private static void write(RegistryFriendlyByteBuf b,List<Component> lines){if(lines.size()>64)throw new IllegalArgumentException("Too many grimoire lines");b.writeVarInt(lines.size());for(var c:lines)ComponentSerialization.TRUSTED_STREAM_CODEC.encode(b,c);}
    private static List<Component> read(RegistryFriendlyByteBuf b){int size=b.readVarInt();if(size<0 || size>64)throw new IllegalArgumentException("Invalid grimoire lines");var lines=new ArrayList<Component>();for(int i=0;i<size;i++)lines.add(ComponentSerialization.TRUSTED_STREAM_CODEC.decode(b));return lines;}
    public static final StreamCodec<RegistryFriendlyByteBuf,GrimoirePayload> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.menu);write(b,p.overview);write(b,p.abilities);write(b,p.stats);write(b,p.bond);},b->new GrimoirePayload(b.readVarInt(),read(b),read(b),read(b),read(b)));
    @Override public Type<GrimoirePayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC);}
}
