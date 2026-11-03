package br.com.soulbound.ritual;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import br.com.soulbound.registry.ModItems;

public final class AltarTokens {
    private AltarTokens() {}
    public static String id(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getString("AltarToken").orElse(""); }
    public static void tag(ItemStack stack, String id) {
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();tag.putString("AltarToken",id);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
    public static AltarSavedData.Token issue(AltarSavedData data, String owner, String kind) {
        var token=new AltarSavedData.Token();token.id=UUID.randomUUID().toString();token.owner=owner;token.kind=kind;
        data.state.tokens.put(token.id,token);data.setDirty();return token;
    }
    public static ItemStack stack(AltarSavedData.Token token) {
        var stack=new ItemStack(ModItems.ALTAR_ITEMS.get(token.kind.equals("spark")?"ancestral_spark":"charged_core").get());
        tag(stack,token.id);
        if (!token.kind.equals("spark")) stack.set(DataComponents.ITEM_NAME,Component.translatable("item.soulbound.charged_core."+token.kind));
        stack.set(DataComponents.LORE,new net.minecraft.world.item.component.ItemLore(java.util.List.of(
                Component.translatable("tooltip.soulbound.altar_token",token.id),
                Component.translatable(token.kind.equals("spark")?"tooltip.soulbound.spark":token.kind.equals("divine")?"tooltip.soulbound.god_bound":token.kind.equals("ascension")?"tooltip.soulbound.pleno_bound":token.kind.equals("awakening")?"tooltip.soulbound.junior_bound":"tooltip.soulbound.core_tradeable"))));
        return stack;
    }
    public static void deliver(ServerPlayer player, AltarSavedData data) {
        for (var token:data.state.tokens.values()) {
            if(token.owner.equals(player.getStringUUID()) && token.xpRewardKey!=null){br.com.soulbound.companion.progression.ProgressionService.reward(player.level().getServer(),player.getUUID(),token.xpRewardKey,100,false);token.xpRewardKey=null;data.setDirty();}
            if (!token.owner.equals(player.getStringUUID()) || token.delivered || token.spent) continue;
            if(InventoryTransfers.received(player,token.id)){
                token.delivered=true;data.flush(player.level().getServer());continue;
            }
            int slot=player.getInventory().getFreeSlot();if(slot<0)return;
            player.getInventory().setItem(slot,stack(token));InventoryTransfers.receipt(player,token.id);
            InventoryTransfers.saveReceipt(player,token.id);
            token.delivered=true;data.flush(player.level().getServer());player.inventoryMenu.broadcastChanges();
        }
    }
}
