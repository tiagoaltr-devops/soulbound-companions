package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.ritual.AncestralBlock;
import br.com.soulbound.ritual.RuinStructure;
import br.com.soulbound.ritual.RuinPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.neoforge.registries.*;

public final class ModAltar {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("soulbound");
    public static final DeferredBlock<Block> PEDESTAL = BLOCKS.registerBlock("ancestral_pedestal", p -> new AncestralBlock(p), p -> p.strength(-1,3600000).pushReaction(PushReaction.BLOCK).lightLevel(s -> 7));
    public static final DeferredBlock<Block> FORGE = BLOCKS.registerBlock("soul_forge", p -> new br.com.soulbound.ritual.ForgeBlock(p), p -> p.strength(5,3600000).pushReaction(PushReaction.BLOCK).lightLevel(s -> 10));
    public static final DeferredBlock<Block> OFFERING = BLOCKS.registerBlock("offering_altar", p -> new br.com.soulbound.ritual.OfferingBlock(p), p -> p.strength(4,3600000).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<Block> PLATFORM = BLOCKS.registerSimpleBlock("xp_platform", p -> p.strength(4,3600000).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<Block> STONE = BLOCKS.registerSimpleBlock("ritual_stone", p -> p.strength(4,3600000).pushReaction(PushReaction.BLOCK));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"soulbound");
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<br.com.soulbound.ritual.AncestralPedestalBlockEntity>> NATURAL_PEDESTAL=BLOCK_ENTITIES.register("ancestral_pedestal",()->new net.minecraft.world.level.block.entity.BlockEntityType<>(br.com.soulbound.ritual.AncestralPedestalBlockEntity::new,true,PEDESTAL.get()));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_SERIALIZER,"soulbound");
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>,net.minecraft.world.level.block.entity.BlockEntityType<br.com.soulbound.ritual.OfferingBlockEntity>> OFFERING_DISPLAY=BLOCK_ENTITIES.register("offering_altar",()->new net.minecraft.world.level.block.entity.BlockEntityType<>(br.com.soulbound.ritual.OfferingBlockEntity::new,true,OFFERING.get()));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<br.com.soulbound.ritual.AltarCraftingRecipe>> FORGE_RECIPE = RECIPES.register("forge_crafting", () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(category -> new br.com.soulbound.ritual.AltarCraftingRecipe(category,true)));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.CustomRecipe.Serializer<br.com.soulbound.ritual.AltarCraftingRecipe>> CORE_RECIPE = RECIPES.register("staff_core", () -> new net.minecraft.world.item.crafting.CustomRecipe.Serializer<>(category -> new br.com.soulbound.ritual.AltarCraftingRecipe(category,false)));
    public static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, "soulbound");
    public static final DeferredHolder<StructureType<?>, StructureType<RuinStructure>> RUIN = STRUCTURES.register("ancestral_ruin", () -> () -> RuinStructure.CODEC);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, "soulbound");
    public static final DeferredHolder<StructurePieceType, StructurePieceType> PIECE = PIECES.register("ancestral_ruin", () -> (context, tag) -> new RuinStructure.Piece(tag));
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT,"soulbound");
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<RuinPlacement>> PLACEMENT = PLACEMENTS.register("village_ruin", () -> () -> RuinPlacement.CODEC);
    public static void register(net.neoforged.bus.api.IEventBus bus) {
        BLOCKS.register(bus); BLOCK_ENTITIES.register(bus); STRUCTURES.register(bus); PIECES.register(bus); PLACEMENTS.register(bus); RECIPES.register(bus);
    }
    private ModAltar() {}
}
