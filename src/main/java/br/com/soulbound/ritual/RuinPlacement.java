package br.com.soulbound.ritual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.*;
import br.com.soulbound.registry.ModAltar;

/** Independent deterministic 50% selection per native village candidate. Never accesses chunks. */
public final class RuinPlacement extends StructurePlacement {
    public static final MapCodec<RuinPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0,4000).optionalFieldOf("minimum_separation",0).forGetter(p -> p.minimumSeparation)
    ).apply(i,RuinPlacement::new));
    public final int minimumSeparation;
    public RuinPlacement() { this(0); }
    public RuinPlacement(int separation) { super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 765491, Optional.empty());minimumSeparation=separation; }
    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        for (var holder : state.possibleStructureSets()) {
            if (holder.unwrapKey().map(k -> k.identifier().toString().equals("minecraft:villages")).orElse(false)
                    && holder.value().placement() instanceof RandomSpreadStructurePlacement villages) {
                var candidate = villages.getPotentialStructureChunk(state.getLevelSeed(),x,z);
                return candidate.x==x && candidate.z==z && villages.isStructureChunk(state,x,z) && RuinSelection.villageSelected(state.getLevelSeed(),x,z);
            }
        }
        return false;
    }
    public static boolean selected(RandomSpreadStructurePlacement villages,long seed,int x,int z,int separation) {
        return selected(villages,seed,x,z,separation,(a,b)->true);
    }
    public static boolean selected(RandomSpreadStructurePlacement villages,long seed,int x,int z,int separation,java.util.function.BiPredicate<Integer,Integer> eligible) {
        // Keep the legacy codec/signature readable; separation no longer suppresses neighboring villages.
        return eligible.test(x,z) && RuinSelection.villageSelected(seed,x,z);
    }
    @Override public StructurePlacementType<?> type() { return ModAltar.PLACEMENT.get(); }
}
