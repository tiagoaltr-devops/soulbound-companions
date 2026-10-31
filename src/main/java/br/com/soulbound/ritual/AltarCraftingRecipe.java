package br.com.soulbound.ritual;

import br.com.soulbound.registry.*;
import br.com.soulbound.item.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

public final class AltarCraftingRecipe extends CustomRecipe {
    private final boolean forge;
    public AltarCraftingRecipe(CraftingBookCategory category,boolean forge){super(category);this.forge=forge;}
    @Override public boolean matches(CraftingInput input,Level level){
        int spark=0,crying=0,netherite=0,diamond=0,staff=0,core=0;
        for(var stack:input.items()){
            if(stack.isEmpty())continue;
            if(forge){if(stack.is(ModItems.ALTAR_ITEMS.get("ancestral_spark").get()) && !AltarTokens.id(stack).isEmpty())spark++;
                else if(stack.is(Items.CRYING_OBSIDIAN))crying++;else if(stack.is(Items.NETHERITE_INGOT))netherite++;else if(stack.is(Items.DIAMOND))diamond++;else return false;
            }else{if(stack.getItem() instanceof CompanionEggItem)staff++;else if(stack.is(ModItems.ALTAR_ITEMS.get("charged_core").get()) && !AltarTokens.id(stack).isEmpty())core++;else return false;}
        }
        return forge?spark==1 && crying==6 && netherite==1 && diamond==1:staff==1 && core==1;
    }
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider lookup){
        ItemStack out=forge?new ItemStack(ModItems.SOUL_FORGE.get()):input.items().stream().filter(s->s.getItem() instanceof CompanionEggItem).findFirst().orElse(ItemStack.EMPTY).copyWithCount(1);
        for(var stack:input.items())if(!AltarTokens.id(stack).isEmpty()){
            if(forge)AltarTokens.tag(out,AltarTokens.id(stack));
            else{var data=out.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.putString("SoulboundPendingCore",AltarTokens.id(stack));out.set(DataComponents.CUSTOM_DATA,CustomData.of(data));}
        }return out;
    }
    @Override public NonNullList<ItemStack> getRemainingItems(CraftingInput input){
        var list=NonNullList.withSize(input.size(),ItemStack.EMPTY);
        if(!forge){var server=net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            for(int i=0;i<input.size();i++){var stack=input.getItem(i);if(stack.is(ModItems.ALTAR_ITEMS.get("charged_core").get())){
                var token=server!=null && server.isSameThread()?AltarSavedData.get(server).state.tokens.get(AltarTokens.id(stack)):null;
                if(token==null || !token.spent)list.set(i,stack.copyWithCount(1));
            }}
        }return list;
    }
    @Override public RecipeSerializer<? extends CustomRecipe> getSerializer(){return forge?ModAltar.FORGE_RECIPE.get():ModAltar.CORE_RECIPE.get();}
}
