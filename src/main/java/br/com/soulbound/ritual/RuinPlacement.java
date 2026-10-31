package br.com.soulbound.ritual;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.*;
import br.com.soulbound.registry.ModAltar;

/** Local priority selection of native village candidates. Never accesses chunks. */
public final class RuinPlacement extends StructurePlacement {
    public static final MapCodec<RuinPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1000,4000).optionalFieldOf("minimum_separation",1000).forGetter(p -> p.minimumSeparation)
    ).apply(i,RuinPlacement::new));
    public final int minimumSeparation;
    public RuinPlacement() { this(1000); }
    public RuinPlacement(int separation) { super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1, 765491, Optional.empty());minimumSeparation=separation; }
    @Override protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        for (var holder : state.possibleStructureSets()) {
            if (holder.unwrapKey().map(k -> k.identifier().toString().equals("minecraft:villages")).orElse(false)
                    && holder.value().placement() instanceof RandomSpreadStructurePlacement villages) {
                var candidate = villages.getPotentialStructureChunk(state.getLevelSeed(),x,z);
                return candidate.x==x && candidate.z==z && villages.isStructureChunk(state,x,z);
            }
        }
        return false;
    }
    public static boolean selected(RandomSpreadStructurePlacement villages,long seed,int x,int z,int separation) {
        return selected(villages,seed,x,z,separation,(a,b)->true);
    }
    public static boolean selected(RandomSpreadStructurePlacement villages,long seed,int x,int z,int separation,java.util.function.BiPredicate<Integer,Integer> eligible) {
        if(!eligible.test(x,z))return false;
        int spacing=villages.spacing();
        int radius=(int)Math.ceil((separation+2.0*RuinSelection.MAX_CENTER_OFFSET)/(spacing*16.0))+1;
        int rx=Math.floorDiv(x,spacing),rz=Math.floorDiv(z,spacing);
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++) {
            var other=villages.getPotentialStructureChunk(seed,(rx+dx)*spacing,(rz+dz)*spacing);
            if((other.x!=x || other.z!=z) && eligible.test(other.x,other.z) && !RuinSelection.wins(seed,x,z,other.x,other.z,separation))return false;
        }
        int cell=Math.max(512,spacing*8),cx=Math.floorDiv(x,cell),cz=Math.floorDiv(z,cell);
        long buffer=separation+2L*RuinSelection.MAX_CENTER_OFFSET;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            var old=villages.getPotentialStructureChunk(seed,(cx+dx)*cell+cell/2,(cz+dz)*cell+cell/2);
            long bx=((long)x-old.x)*16,bz=((long)z-old.z)*16;
            if(bx*bx+bz*bz<buffer*buffer)return false;
        }
        return true;
    }
    @Override public StructurePlacementType<?> type() { return ModAltar.PLACEMENT.get(); }
}
