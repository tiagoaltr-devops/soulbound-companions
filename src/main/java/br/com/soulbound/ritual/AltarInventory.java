package br.com.soulbound.ritual;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/** Registry-aware stack snapshots retain components, counts and modded item data. */
public final class AltarInventory {
    private AltarInventory() {}
    public static String key(ServerLevel level,BlockPos pos){return level.dimension().identifier()+":"+pos.asLong();}
    public static String encode(ServerLevel level,ItemStack stack){return stack.isEmpty()?"":ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),stack).getOrThrow().toString();}
    public static ItemStack decode(ServerLevel level,String json){return json==null||json.isEmpty()?ItemStack.EMPTY:ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE),JsonParser.parseString(json)).getOrThrow();}
    public static void exchange(ServerPlayer p,BlockPos pos){
        if(RitualService.locked(p.level(),pos))return;
        InventoryTransfers.exchange(p,key(p.level(),pos),false);
    }
}
