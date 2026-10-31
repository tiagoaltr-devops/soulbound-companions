package br.com.soulbound.ritual;

import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.ChunkPos;

/** Pure NBT verification. A predicted start alone is never sufficient evidence. */
public record NaturalRuinProof(String id,String dimension,BlockPos center,int villageX,int villageZ,int version) {
    public static Optional<NaturalRuinProof> fromSource(CompoundTag chunk,long seed,String dimension,ChunkPos expected) {
        return fromSource(chunk,seed,dimension,expected.x,expected.z);
    }
    public static Optional<NaturalRuinProof> fromSource(CompoundTag chunk,long seed,String dimension,int expectedX,int expectedZ) {
        if(chunk.getIntOr("xPos",Integer.MIN_VALUE)!=expectedX || chunk.getIntOr("zPos",Integer.MIN_VALUE)!=expectedZ)return Optional.empty();
        var starts=chunk.getCompound("structures").flatMap(t->t.getCompound("starts")).orElseGet(CompoundTag::new);
        var ruin=starts.getCompound("soulbound:ancestral_ruin").orElseGet(CompoundTag::new);
        if(!ruin.getStringOr("id","").equals("soulbound:ancestral_ruin"))return Optional.empty();
        boolean village=false;
        for(String key:starts.keySet())if(key.startsWith("minecraft:village_") && starts.getCompound(key).map(t->t.getStringOr("id","").equals(key)).orElse(false)){village=true;break;}
        if(!village)return Optional.empty();
        for(var tag:ruin.getList("Children").orElseGet(ListTag::new))if(tag instanceof CompoundTag child && child.getStringOr("id","").equals("soulbound:ancestral_ruin")) {
            int[] b=child.getIntArray("BB").orElse(new int[0]);if(b.length!=6)continue;
            int version=child.getIntOr("RuinVersion",1),radius=version==1?6:child.getIntOr("Radius",3);
            if((radius!=3 && radius!=6) || b[3]-b[0]!=2*radius || b[5]-b[2]!=2*radius)continue;
            var center=new BlockPos(b[0]+radius,b[1]+1,b[2]+radius);
            int sx=child.getIntOr("VillageX",Math.floorDiv(center.getX()-32,16)),sz=child.getIntOr("VillageZ",Math.floorDiv(center.getZ(),16));
            if(sx!=expectedX || sz!=expectedZ)continue;
            String derived=UUID.nameUUIDFromBytes((seed+":"+dimension+":"+center.asLong()).getBytes(StandardCharsets.UTF_8)).toString();
            String id=child.getStringOr("RuinId",derived);if(id.isEmpty())id=derived;if(!id.equals(derived))continue;
            return Optional.of(new NaturalRuinProof(id,dimension,center,sx,sz,version));
        }
        return Optional.empty();
    }
    public boolean physicalPedestal(CompoundTag chunk) {
        if(chunk.getIntOr("xPos",Integer.MIN_VALUE)!=(center.getX()>>4) || chunk.getIntOr("zPos",Integer.MIN_VALUE)!=(center.getZ()>>4))return false;
        String status=chunk.getStringOr("Status","");
        if(!Set.of("minecraft:features","minecraft:initialize_light","minecraft:light","minecraft:spawn","minecraft:full").contains(status))return false;
        boolean block=false;
        for(var tag:chunk.getList("sections").orElseGet(ListTag::new))if(tag instanceof CompoundTag section && section.getIntOr("Y",Integer.MIN_VALUE)==Math.floorDiv(center.getY(),16)){
            var states=section.getCompound("block_states").orElseGet(CompoundTag::new);
            var palette=states.getList("palette").orElseGet(ListTag::new);int target=-1;
            for(int i=0;i<palette.size();i++)if(palette.get(i) instanceof CompoundTag entry && entry.getStringOr("Name","").equals("soulbound:ancestral_pedestal")){target=i;break;}
            if(target<0)return false;
            int index=((center.getY()&15)<<8)|((center.getZ()&15)<<4)|(center.getX()&15);
            int bits=Math.max(4,32-Integer.numberOfLeadingZeros(palette.size()-1));
            block=palette.size()==1 || new SimpleBitStorage(bits,4096,states.getLongArray("data").orElseThrow()).get(index)==target;
            break;
        }
        if(!block)return false;
        if(version==1)return true; // Legacy natural starts had no block entity; require both structure and physical block.
        for(var tag:chunk.getList("block_entities").orElseGet(ListTag::new))if(tag instanceof CompoundTag be
                && be.getIntOr("x",Integer.MIN_VALUE)==center.getX() && be.getIntOr("y",Integer.MIN_VALUE)==center.getY() && be.getIntOr("z",Integer.MIN_VALUE)==center.getZ()
                && be.getStringOr("id","").equals("soulbound:ancestral_pedestal") && be.getStringOr("NaturalRuin","").equals(id)
                && be.getLongOr("NaturalPosition",Long.MIN_VALUE)==center.asLong())return true;
        return false;
    }
}
