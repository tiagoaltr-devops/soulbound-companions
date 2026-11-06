package br.com.soulbound.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record GrimoireOpenPayload(int menu) implements CustomPacketPayload {
    public static final Type<GrimoireOpenPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("soulbound","open_grimoire"));
    public static final StreamCodec<FriendlyByteBuf,GrimoireOpenPayload> CODEC=StreamCodec.of((b,p)->b.writeVarInt(p.menu),b->new GrimoireOpenPayload(b.readVarInt()));
    @Override public Type<GrimoireOpenPayload> type(){return TYPE;}
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,CODEC,(p,c)->{
        if(c.player() instanceof net.minecraft.server.level.ServerPlayer player && player.containerMenu.containerId==p.menu
            && !(player.containerMenu instanceof br.com.soulbound.menu.SoulCommandMenu)
            && (player.containerMenu instanceof br.com.soulbound.menu.TalentMenu || player.containerMenu instanceof br.com.soulbound.menu.CompanionInventoryMenu))
            for(var hand:net.minecraft.world.InteractionHand.values())if(br.com.soulbound.companion.SoulStaffCommands.information(player,player.getItemInHand(hand))!=null){br.com.soulbound.companion.SoulStaffCommands.openMenu(player,hand);break;}
    });}
}
