package br.com.soulbound.ritual;

import br.com.soulbound.registry.ModAltar;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public final class RuinStructure extends Structure {
    public static final MapCodec<RuinStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),Codec.intRange(2000,32000).optionalFieldOf("minimum_spawn_distance",2000).forGetter(s -> s.minimumSpawnDistance)
    ).apply(i,RuinStructure::new));
    private final int minimumSpawnDistance;
    private static final List<net.minecraft.world.level.block.Block> FUNCTIONAL=List.of(Blocks.BELL,Blocks.BLAST_FURNACE,Blocks.BREWING_STAND,
            Blocks.CARTOGRAPHY_TABLE,Blocks.CAULDRON,Blocks.COMPOSTER,Blocks.FLETCHING_TABLE,Blocks.GRINDSTONE,Blocks.LECTERN,Blocks.LOOM,
            Blocks.SMITHING_TABLE,Blocks.SMOKER,Blocks.STONECUTTER,Blocks.BARREL,
            Blocks.WHITE_BED,Blocks.ORANGE_BED,Blocks.MAGENTA_BED,Blocks.LIGHT_BLUE_BED,Blocks.YELLOW_BED,Blocks.LIME_BED,Blocks.PINK_BED,Blocks.GRAY_BED,
            Blocks.LIGHT_GRAY_BED,Blocks.CYAN_BED,Blocks.PURPLE_BED,Blocks.BLUE_BED,Blocks.BROWN_BED,Blocks.GREEN_BED,Blocks.RED_BED,Blocks.BLACK_BED);
    public RuinStructure(StructureSettings settings) { this(settings,2000); }
    public RuinStructure(StructureSettings settings,int minimum) { super(settings);minimumSpawnDistance=minimum; }
    @Override protected Optional<GenerationStub> findGenerationPoint(GenerationContext c) {
        if(!selected(c.chunkGenerator(),c.randomState(),c.registryAccess(),c.heightAccessor(),c.seed(),c.chunkPos()))return Optional.empty();
        // Reconstruct only the vanilla jigsaw plan on the generation worker, without loading chunks or changing pools.
        for(var holder:c.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList()) {
            String key=holder.key().identifier().toString();
            if(!key.startsWith("minecraft:village_"))continue;
            var village=holder.value().generate(holder,Level.OVERWORLD,c.registryAccess(),c.chunkGenerator(),c.biomeSource(),
                    c.randomState(),c.structureTemplateManager(),c.seed(),c.chunkPos(),0,c.heightAccessor(),holder.value().biomes()::contains);
            if(!village.isValid())continue;
            var site=site(c,village);if(site==null){com.mojang.logging.LogUtils.getLogger().debug("No safe decorative ruin site in village {} at {}",key,c.chunkPos());continue;}
            var center=site.center;
            String id=java.util.UUID.nameUUIDFromBytes((c.seed()+":minecraft:overworld:"+center.asLong()).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
            return Optional.of(new GenerationStub(center,builder -> builder.addPiece(new Piece(center,id,c.chunkPos(),key,site.radius,minimumSpawnDistance,site.protectedBlocks))));
        }
        return Optional.empty();
    }
    public static boolean selected(ChunkGenerator generator,net.minecraft.world.level.levelgen.RandomState random,RegistryAccess registry,LevelHeightAccessor heights,long seed,ChunkPos pos){
        var villageSet=registry.lookupOrThrow(Registries.STRUCTURE_SET).getValue(net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE_SET,net.minecraft.resources.Identifier.withDefaultNamespace("villages")));
        if(villageSet==null || !(villageSet.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement villages))return false;
        int separation=1000;
        for(var set:registry.lookupOrThrow(Registries.STRUCTURE_SET).listElements().toList())if(set.value().placement() instanceof RuinPlacement p)separation=p.minimumSeparation;
        return RuinPlacement.selected(villages,seed,pos.x,pos.z,separation,(x,z)->{
            int bx=x*16+8,bz=z*16+8;
            var biome=generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(bx),QuartPos.fromBlock(generator.getSeaLevel()),QuartPos.fromBlock(bz),random.sampler());
            return villageSet.structures().stream().anyMatch(entry->entry.structure().value().biomes().contains(biome));
        });
    }
    private record Site(BlockPos center,int radius,long[] protectedBlocks) {}
    public static boolean decorativeTemplate(String name) {
        return name.startsWith("minecraft:village/") && (name.contains("fountain") || name.contains("/town_centers/") && name.contains("meeting_point"));
    }
    private static Site site(GenerationContext c,StructureStart village) {
        var pieces=new ArrayList<PoolElementStructurePiece>();
        for(var piece:village.getPieces())if(piece instanceof PoolElementStructurePiece pool && pool.getElement() instanceof SinglePoolElement single
                && decorativeTemplate(single.getTemplateLocation().toString()))pieces.add(pool);
        pieces.sort(Comparator.comparingInt(p -> ((SinglePoolElement)p.getElement()).getTemplateLocation().getPath().contains("fountain")?0:1));
        for(var chosen:pieces) {
            var single=(SinglePoolElement)chosen.getElement();
            var bounds=chosen.getBoundingBox();
            var protectedPositions=new ArrayList<BlockPos>();
            for(var block:FUNCTIONAL)for(var info:c.structureTemplateManager().getOrCreate(single.getTemplateLocation()).filterBlocks(chosen.getPosition(),new StructurePlaceSettings().setRotation(chosen.getRotation()),block)){
                protectedPositions.add(info.pos());for(var direction:Direction.values())protectedPositions.add(info.pos().relative(direction));
            }
            for(int radius:new int[]{6,3})for(int[] offset:new int[][]{{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1},{3,0},{-3,0},{0,3},{0,-3}}) {
                int x=bounds.getCenter().getX()+offset[0],z=bounds.getCenter().getZ()+offset[1];
                int floor=chosen.getPosition().getY()+chosen.getGroundLevelDelta()-1;
                var center=new BlockPos(x,floor+1,z);
                if(x-radius<bounds.minX() || x+radius>bounds.maxX() || z-radius<bounds.minZ() || z+radius>bounds.maxZ()
                        || horizontalSquared(center,c.chunkPos().getMiddleBlockPosition(0))>128L*128
                        || protectedPositions.contains(center))continue;
                boolean clear=true;
                for(var other:village.getPieces()) {
                    if(other==chosen)continue;
                    if(other instanceof PoolElementStructurePiece p && p.getElement() instanceof SinglePoolElement e) {
                        String name=e.getTemplateLocation().getPath();
                        if(name.contains("/streets/") || name.contains("/decor/") || name.contains("/terminators/") || name.contains("/villagers/") || name.contains("/animals/") || name.endsWith("well_bottom"))continue;
                    }
                    if(other instanceof PoolElementStructurePiece p && p.getElement() instanceof net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement)continue;
                    var b=other.getBoundingBox();
                    if(b.minX()<=x+radius && b.maxX()>=x-radius && b.minZ()<=z+radius && b.maxZ()>=z-radius){
                        if(radius==3 && offset[0]==0 && offset[1]==0)com.mojang.logging.LogUtils.getLogger().debug("Ruin plaza overlaps protected piece {}",other);
                        clear=false;break;
                    }
                }
                if(clear)return new Site(center,radius,protectedPositions.stream().mapToLong(BlockPos::asLong).toArray());
            }
        }
        return null;
    }
    @Override public StructureType<?> type() { return ModAltar.RUIN.get(); }
    public static final class Piece extends StructurePiece {
        public final String ruinId;
        public final int version,sourceX,sourceZ,radius,spawnDistance;
        public final String villageType;
        private final long[] protectedBlocks;
        public Piece(BlockPos center,String id,ChunkPos source,String village,int size,int minimum,long[] protectedPositions) {
            super(ModAltar.PIECE.get(),0,new BoundingBox(center.getX()-size,center.getY()-1,center.getZ()-size,center.getX()+size,center.getY()+4,center.getZ()+size));
            ruinId=id;version=2;sourceX=source.x;sourceZ=source.z;radius=size;spawnDistance=minimum;villageType=village;protectedBlocks=protectedPositions;
        }
        public Piece(CompoundTag tag) {
            super(ModAltar.PIECE.get(),tag);ruinId=tag.getStringOr("RuinId","");version=tag.getIntOr("RuinVersion",1);
            radius=version==1?6:tag.getIntOr("Radius",3);spawnDistance=tag.getIntOr("MinimumSpawn",2000);
            sourceX=tag.getIntOr("VillageX",Math.floorDiv(boundingBox.minX()+radius-32,16));sourceZ=tag.getIntOr("VillageZ",Math.floorDiv(boundingBox.minZ()+radius,16));
            villageType=tag.getStringOr("VillageType","");protectedBlocks=tag.getLongArray("ProtectedBlocks").orElse(new long[0]);
        }
        @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("RuinId",ruinId);tag.putInt("RuinVersion",version);tag.putInt("VillageX",sourceX);tag.putInt("VillageZ",sourceZ);tag.putInt("Radius",radius);
            tag.putInt("MinimumSpawn",spawnDistance);tag.putString("VillageType",villageType);tag.putLongArray("ProtectedBlocks",protectedBlocks);
        }
        public BlockPos center(){return new BlockPos(boundingBox.minX()+radius,boundingBox.minY()+1,boundingBox.minZ()+radius);}
        @Override public void postProcess(WorldGenLevel world, StructureManager manager, ChunkGenerator generator,
                RandomSource random, BoundingBox box, ChunkPos chunk, BlockPos origin) {
            if(!(world instanceof net.minecraft.server.level.WorldGenRegion))return;
            int x=boundingBox.minX()+radius,y=boundingBox.minY(),z=boundingBox.minZ()+radius;
            var center = new BlockPos(x,y+1,z);
            var spawn = world.getLevel().getRespawnData().pos();
            if (!world.getLevel().dimension().equals(Level.OVERWORLD) || horizontalSquared(center,spawn)<(long)spawnDistance*spawnDistance) return;
            var source = new ChunkPos(sourceX,sourceZ);
            var sourceChunk = world.getChunk(source.x,source.z,ChunkStatus.STRUCTURE_STARTS);
            boolean village = false;
            for (var holder : world.registryAccess().lookupOrThrow(Registries.STRUCTURE).listElements().toList()) {
                if (holder.key().identifier().getPath().startsWith("village_") && (villageType.isEmpty() || holder.key().identifier().toString().equals(villageType))) {
                    var start = sourceChunk.getStartForStructure(holder.value());
                    if (start != null && start.isValid()) { village = true; break; }
                }
            }
            if (!village) return;
            var ancient=villageType.contains("desert")?Blocks.CUT_SANDSTONE:villageType.contains("savanna")?Blocks.TERRACOTTA:Blocks.MOSSY_STONE_BRICKS;
            var cracked=villageType.contains("desert")?Blocks.SMOOTH_SANDSTONE:villageType.contains("taiga") || villageType.contains("snowy")?Blocks.COBBLESTONE:Blocks.CRACKED_STONE_BRICKS;
            for(int dx=-radius;dx<=radius;dx++) for(int dz=-radius;dz<=radius;dz++) {
                var p=new BlockPos(x+dx,y,z+dz);
                if (!box.isInside(p)) continue;
                if(!protectedAt(p))world.setBlock(p,(random.nextInt(3)==0?cracked:ancient).defaultBlockState(),2);
                for(int dy=1;dy<=4;dy++)if(!protectedAt(p.above(dy)))world.setBlock(p.above(dy),Blocks.AIR.defaultBlockState(),2);
                if (Math.abs(dx)==radius && Math.abs(dz)==radius) {
                    for(int dy=1;dy<=3;dy++)if(!protectedAt(p.above(dy)))world.setBlock(p.above(dy),ancient.defaultBlockState(),2);
                    if(!protectedAt(p.above(4)))world.setBlock(p.above(4),Blocks.SOUL_LANTERN.defaultBlockState(),2);
                }
            }
            if (box.isInside(center)) {
                world.setBlock(center,ModAltar.PEDESTAL.get().defaultBlockState(),2);
                if(world.getBlockEntity(center) instanceof AncestralPedestalBlockEntity pedestal)pedestal.stamp(ruinId,sourceX,sourceZ);
                RuinIndex.generated(world.getLevel().getServer(),world.getLevel().dimension(),center,ruinId,sourceX,sourceZ);
            }
        }
        private boolean protectedAt(BlockPos pos){for(long value:protectedBlocks)if(value==pos.asLong())return true;return false;}
    }
    public static long horizontalSquared(BlockPos a, BlockPos b) { long x=(long)a.getX()-b.getX(),z=(long)a.getZ()-b.getZ();return x*x+z*z; }
}
