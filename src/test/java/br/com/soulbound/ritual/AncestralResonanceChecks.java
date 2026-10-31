package br.com.soulbound.ritual;

import java.util.*;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.SimpleBitStorage;
import com.mojang.serialization.JsonOps;
import br.com.soulbound.entity.ai.ResonanceFlightState;

/** Exercises physical provenance, migration, nearest filtering, cooldown persistence and bounded flight. */
public final class AncestralResonanceChecks {
    private static int checks;
    private static void require(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        var center=new BlockPos(3208,71,3208);long seed=20261101;
        String dimension="minecraft:overworld",id=UUID.nameUUIDFromBytes((seed+":"+dimension+":"+center.asLong()).getBytes(StandardCharsets.UTF_8)).toString();
        var proof=new NaturalRuinProof(id,dimension,center,200,200,2);
        var target=physical(center,id,true);
        require(proof.physicalPedestal(target),"Exact physical pedestal and natural stamp");
        var artificial=physical(center,id,false);require(!proof.physicalPedestal(artificial),"Creative pedestal has no natural stamp");
        var wrong=physical(center,id,true);wrong.putInt("xPos",1);require(!proof.physicalPedestal(wrong),"Copied chunk/incorrect position");
        var unfinished=physical(center,id,true);unfinished.putString("Status","minecraft:structure_starts");require(!proof.physicalPedestal(unfinished),"Unbuilt start is not a ruin");
        var src=new CompoundTag();src.putInt("xPos",200);src.putInt("zPos",200);
        var starts=new CompoundTag();var village=new CompoundTag();village.putString("id","minecraft:village_plains");starts.put("minecraft:village_plains",village);
        var start=new CompoundTag();start.putString("id","soulbound:ancestral_ruin");
        var child=new CompoundTag();child.putString("id","soulbound:ancestral_ruin");child.putString("RuinId",id);child.putInt("RuinVersion",2);child.putInt("Radius",3);child.putInt("VillageX",200);child.putInt("VillageZ",200);
        child.putIntArray("BB",new int[]{3205,70,3205,3211,75,3211});var children=new ListTag();children.add(child);start.put("Children",children);starts.put("soulbound:ancestral_ruin",start);
        var structures=new CompoundTag();structures.put("starts",starts);src.put("structures",structures);
        require(NaturalRuinProof.fromSource(src,seed,dimension,200,200).orElseThrow().equals(proof),"Native village and stable piece identity");
        var noVillage=src.copy();noVillage.getCompound("structures").orElseThrow().getCompound("starts").orElseThrow().remove("minecraft:village_plains");
        require(NaturalRuinProof.fromSource(noVillage,seed,dimension,200,200).isEmpty(),"False-positive ruin start with no actual village rejected");
        require(NaturalRuinProof.fromSource(src,seed+1,dimension,200,200).isEmpty(),"Wrong seed identity rejected");
        var extended=src.copy();var extendedPiece=extended.getCompound("structures").orElseThrow().getCompound("starts").orElseThrow().getCompound("soulbound:ancestral_ruin").orElseThrow().getList("Children").orElseThrow().getCompound(0).orElseThrow();
        extendedPiece.putLong("PedestalCenter",center.asLong());extendedPiece.putIntArray("BB",new int[]{3190,70,3200,3220,78,3220});
        require(NaturalRuinProof.fromSource(extended,seed,dimension,200,200).orElseThrow().equals(proof),"Bell cleanup bounds preserve physical identity and center");
        var bell=center.offset(12,2,0);long[] bells={bell.asLong()};
        var bounds=RuinSelection.replacementBounds(center,3,bells);
        require(bounds.isInside(bell) && bounds.isInside(center.below()),"Worldgen bounds include plaza bell outside ruin footprint");
        require(RuinSelection.replacesBell(bell,bounds,bells),"Exact plaza template bell selected");
        require(!RuinSelection.replacesBell(bell.east(),bounds,bells),"Nearby unrelated bell never selected");
        require(!RuinSelection.replacesBell(bell,new net.minecraft.world.level.levelgen.structure.BoundingBox(0,0,0,15,255,15),bells),"Bell removal limited to current chunk pass");
        require(!RuinSelection.replacesBell(bell,bounds,new long[0]),"Legacy pieces do not gain bell removal");
        var legacySource=src.copy();var legacyChild=legacySource.getCompound("structures").orElseThrow().getCompound("starts").orElseThrow().getCompound("soulbound:ancestral_ruin").orElseThrow().getList("Children").orElseThrow().getCompound(0).orElseThrow();
        legacyChild.remove("RuinVersion");legacyChild.remove("RuinId");legacyChild.remove("VillageX");legacyChild.remove("VillageZ");legacyChild.putIntArray("BB",new int[]{3234,70,3202,3246,75,3214});
        var oldProof=NaturalRuinProof.fromSource(legacySource,seed,dimension,200,200).orElseThrow();
        require(oldProof.version()==1 && oldProof.center().equals(new BlockPos(3240,71,3208)),"Read Phase 6 piece without new provenance fields");
        require(oldProof.physicalPedestal(physical(oldProof.center(),oldProof.id(),false)),"Legacy physical pedestal plus native village/start is migratable");
        var ledger=new AltarSavedData();UUID player=UUID.randomUUID();
        var near=ruin("near",10,dimension);var far=ruin("far",100,dimension);var fake=ruin("fake",1,dimension);fake.origin="ARTIFICIAL";
        ledger.state.ruins.put(near.id,near);ledger.state.ruins.put(far.id,far);ledger.state.ruins.put(fake.id,fake);
        require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,1000).orElseThrow()==near,"Nearest natural eligible target");
        near.cooldowns.put(player.toString(),Long.MAX_VALUE);near.trial=new AltarSavedData.Trial();
        require(RuinIndex.eligible(near,dimension,player),"Temporary occupation and cooldown remain locatable");
        near.completed.add(player.toString());require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,1000).orElseThrow()==far,"Skip completed target and find next");
        far.integrity="PERMANENTLY_INVALID";require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,1000).isEmpty(),"Skip permanently invalid and artificial targets");
        var edge=ruin("edge",5000,dimension);ledger.state.ruins.put(edge.id,edge);
        require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,5000).orElseThrow()==edge,"Natural unvisited ruin at 5000 horizontal blocks is eligible");
        edge.x=5001;require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,5000).isEmpty(),"Beyond 5000 is excluded");
        edge.x=4999;edge.completed.add(player.toString());require(RuinIndex.nearest(ledger,dimension,BlockPos.ZERO,player,5000).isEmpty(),"Completion remains owner-specific at long range");
        var legacy=ruin("legacy",5,dimension);legacy.origin="LEGACY_UNVERIFIED";require(!RuinIndex.eligible(legacy,dimension,player),"Legacy must be physically verified before discovery");
        ledger.progress(player).resonanceCooldownEnd=600;
        var decoded=AltarSavedData.CODEC.parse(JsonOps.INSTANCE,AltarSavedData.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow()).getOrThrow();
        require(decoded.progress(player).resonanceCooldownEnd==600,"Persist resonance cooldown");
        require(decoded.state.ruins.get("near").completed.contains(player.toString()),"Preserve old completion/cooldowns");
        var oldJson=com.google.gson.JsonParser.parseString(AltarSavedData.CODEC.encodeStart(JsonOps.INSTANCE,ledger).getOrThrow().getAsString()).getAsJsonObject();
        var oldRuin=oldJson.getAsJsonObject("ruins").getAsJsonObject("near");oldRuin.remove("origin");oldRuin.remove("integrity");oldRuin.remove("generationVersion");
        var migrated=AltarSavedData.CODEC.parse(JsonOps.INSTANCE,new com.google.gson.JsonPrimitive(oldJson.toString())).getOrThrow();
        require(migrated.state.ruins.get("near").origin.equals("LEGACY_UNVERIFIED"),"Missing legacy origin never silently becomes natural");
        require(migrated.state.ruins.get("near").completed.contains(player.toString()),"Migration preserves completed UUIDs");
        for(String mode:new String[]{"FOLLOW_PASSIVE","STAY"}){
            var identity=br.com.soulbound.companion.identity.SoulboundRecord.create(player,"Owner").withState(br.com.soulbound.companion.identity.CompanionStatus.ACTIVE,false,mode);
            var flight=new ResonanceFlightState();flight.begin(0);require(flight.phase()==ResonanceFlightState.Phase.OUTBOUND,"Start outbound");
            flight.tick(20,true,false);require(flight.phase()==ResonanceFlightState.Phase.HOLD,"Hold at tip");
            flight.tick(50,false,false);require(flight.phase()==ResonanceFlightState.Phase.RETURN,"Return after hold");
            flight.tick(60,false,true);require(!flight.active(),"Finish near owner");
            require(identity.behaviorMode().equals(mode) && identity.companionUuid()!=null,"State machine does not mutate canonical mode or UUID");
            flight.begin(70);flight.cancel();require(!flight.active(),"Safe explicit cancellation/recall");
            flight.begin(0);flight.tick(200,false,false);require(!flight.active(),"Hard timeout never leaves a stuck tracking state");
        }
        int selected=0;
        var chosen=new ArrayList<int[]>();
        for(int x=-30;x<=30;x++)for(int z=-30;z<=30;z++){
            boolean wins=true;
            for(int dx=-4;dx<=4 && wins;dx++)for(int dz=-4;dz<=4;dz++)if((dx!=0 || dz!=0) && !RuinSelection.wins(seed,x*34,z*34,(x+dx)*34,(z+dz)*34,1000)){wins=false;break;}
            if(wins){selected++;chosen.add(new int[]{x*34,z*34});}
        }
        require(selected>3721/100,"Significantly denser than one candidate per 512-chunk cell");
        boolean separated=true;for(int i=0;i<chosen.size();i++)for(int j=i+1;j<chosen.size();j++){
            long dx=(chosen.get(i)[0]-chosen.get(j)[0])*16L,dz=(chosen.get(i)[1]-chosen.get(j)[1])*16L;
            if(dx*dx+dz*dz<(1000+256L)*(1000+256L))separated=false;
        }
        require(separated,"Pairwise separation including two maximum center displacements");
        var distribution=new RuinSelection.Distribution(seed,1000,site->{
            var neighbors=new ArrayList<RuinSelection.Site>();
            for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)neighbors.add(new RuinSelection.Site(site.x()+dx*34,site.z()+dz*34));
            return neighbors;
        },site->true);
        var denser=new ArrayList<RuinSelection.Site>();
        for(int x=-30;x<=30;x++)for(int z=-30;z<=30;z++){
            var site=new RuinSelection.Site(x*34,z*34);if(distribution.selected(site))denser.add(site);
        }
        require(denser.size()>selected*1.4,"Second round significantly increases selection, not just changing constants");
        require(chosen.stream().allMatch(p->denser.contains(new RuinSelection.Site(p[0],p[1]))),"All previous first-round winners retained");
        boolean newSeparated=true;
        for(int i=0;i<denser.size();i++)for(int j=i+1;j<denser.size();j++){
            long dx=(denser.get(i).x()-denser.get(j).x())*16L,dz=(denser.get(i).z()-denser.get(j).z())*16L;
            if(dx*dx+dz*dz<(1000+256L)*(1000+256L))newSeparated=false;
        }
        require(newSeparated,"Both rounds preserve 1000 blocks after maximum center displacements");
        require(new HashSet<>(denser).size()==denser.size(),"No duplicate village candidates");
        require(RuinSelection.distanceSquared(new BlockPos(500,0,0),BlockPos.ZERO)==250000,"New spawn boundary is 500 blocks");
        require(!AltarPolicy.trialCoolingDown(Long.MAX_VALUE,100,0),"Zero bypasses persisted old trial cooldowns");
        require(AltarPolicy.trialCoolingDown(200,100,36000),"Positive trial cooldown can be reenabled");
        require(!AltarPolicy.trialCoolingDown(100,100,36000),"Expired positive cooldown permits retry");
        require(RuinSelection.distanceSquared(new BlockPos(2000,0,0),BlockPos.ZERO)==4000000,"Spawn barrier boundary");
        System.out.println("Ancestral resonance checks passed: "+checks+"; synthetic village candidates selected="+selected+"/3721");
        System.out.println("Phase 6.1.2 frequency comparison: "+selected+" -> "+denser.size()+" / 3721 candidates (one seed, no chunks generated)");
    }
    private static AltarSavedData.Ruin ruin(String id,int x,String dimension){var r=new AltarSavedData.Ruin();r.id=id;r.dimension=dimension;r.x=x;r.origin="NATURAL_WORLDGEN";r.integrity="INTACT";return r;}
    private static CompoundTag physical(BlockPos pos,String id,boolean stamped){
        var root=new CompoundTag();root.putInt("xPos",pos.getX()>>4);root.putInt("zPos",pos.getZ()>>4);root.putString("Status","minecraft:full");
        var section=new CompoundTag();section.putByte("Y",(byte)(pos.getY()>>4));var states=new CompoundTag();var palette=new ListTag();
        var air=new CompoundTag();air.putString("Name","minecraft:air");palette.add(air);var pedestal=new CompoundTag();pedestal.putString("Name","soulbound:ancestral_pedestal");palette.add(pedestal);
        var bits=new SimpleBitStorage(4,4096);bits.set(((pos.getY()&15)<<8)|((pos.getZ()&15)<<4)|(pos.getX()&15),1);
        states.put("palette",palette);states.putLongArray("data",bits.getRaw());section.put("block_states",states);var sections=new ListTag();sections.add(section);root.put("sections",sections);
        if(stamped){var be=new CompoundTag();be.putString("id","soulbound:ancestral_pedestal");be.putInt("x",pos.getX());be.putInt("y",pos.getY());be.putInt("z",pos.getZ());be.putString("NaturalRuin",id);be.putLong("NaturalPosition",pos.asLong());var list=new ListTag();list.add(be);root.put("block_entities",list);}
        return root;
    }
}
