package br.com.soulbound.item;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import br.com.soulbound.companion.identity.SoulboundRecord;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import org.jspecify.annotations.Nullable;

/** A vanilla-synchronized tooltip projection. Only owner and bond IDs are accepted as item credentials. */
public final class SoulEggIdentity {
    private SoulEggIdentity() {}

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static @Nullable UUID owner(ItemStack stack) { return data(stack).read("SoulboundOwner", UUIDUtil.CODEC).orElse(null); }
    public static @Nullable UUID bond(ItemStack stack) { return data(stack).read("SoulboundBond", UUIDUtil.CODEC).orElse(null); }

    public static boolean isBlank(ItemStack stack) {
        var tag = data(stack);
        return !tag.contains("SoulboundOwner") && !tag.contains("SoulboundBond");
    }

    public static boolean matches(ItemStack stack, SoulboundRecord record) {
        return record.ownerUuid().equals(owner(stack)) && record.bondUuid().equals(bond(stack));
    }

    public static void refresh(ItemStack stack, SoulboundRecord record) {
        var tag = new CompoundTag();
        tag.store("SoulboundOwner", UUIDUtil.CODEC, record.ownerUuid());
        tag.store("SoulboundBond", UUIDUtil.CODEC, record.bondUuid());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var form = SoulItemForm.forStatus(record.companionStatus());
        stack.set(DataComponents.ITEM_MODEL, form.model());
        stack.set(DataComponents.ITEM_NAME, Component.translatable(form.nameKey()));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.soulbound.owner", record.ownerLastKnownName()),
                Component.translatable("tooltip.soulbound.species", translated("species", record.species())),
                Component.translatable("tooltip.soulbound.stage", translated("stage", record.evolutionStage())),
                Component.translatable("tooltip.soulbound.status", translated("status", record.companionStatus().name())),
                Component.translatable("tooltip.soulbound.mode", translated("behavior", record.behaviorMode())),
                Component.translatable("tooltip.soulbound.permanent"))));
        stack.setCount(1);
    }

    private static Component translated(String category, String value) {
        return Component.translatable(category + ".soulbound." + value.toLowerCase(Locale.ROOT));
    }
}
