package br.com.soulbound.ritual;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.visitors.CollectToTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import br.com.soulbound.registry.ModAltar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Worldgen publishes immutable evidence; only the server thread mutates the canonical index. */
@EventBusSubscriber(modid="soulbound")
public final class RuinIndex {
    private record Generated(MinecraftServer server,NaturalRuinProof proof) {}
    private static final ConcurrentLinkedQueue<Generated> GENERATED=new ConcurrentLinkedQueue<>();
    private static final Map<MinecraftServer,Map<BlockPos,NaturalRuinProof>> WAITING=new IdentityHashMap<>();
    private static final Map<MinecraftServer,ArrayDeque<ChunkPos>> BACKFILL=new IdentityHashMap<>();
    private static final Map<MinecraftServer,Integer> ACTIVE_READS=new IdentityHashMap<>();
    private RuinIndex() {}
    public static String status(MinecraftServer s){return "registered="+AltarSavedData.get(s).state.ruins.size()+", generationPending="+WAITING.getOrDefault(s,Map.of()).size()+", diskQueued="+BACKFILL.getOrDefault(s,new ArrayDeque<>()).size()+", diskInFlight="+ACTIVE_READS.getOrDefault(s,0);}
    public static void queueBackfill(ServerLevel level,List<ChunkPos> sources){var queue=BACKFILL.computeIfAbsent(level.getServer(),s->new ArrayDeque<>());for(var pos:sources)if(!queue.contains(pos) && queue.size()<512)queue.addLast(pos);}
    public static void generated(MinecraftServer server,ResourceKey<Level> dimension,BlockPos center,String id,int x,int z){
        GENERATED.add(new Generated(server,new NaturalRuinProof(id,dimension.identifier().toString(),center.immutable(),x,z,2)));
    }
    public static void loaded(ServerLevel level,BlockPos center,String id,int x,int z){
        var r=AltarSavedData.get(level.getServer()).state.ruins.get(id);
        if(r!=null && r.x==center.getX() && r.y==center.getY() && r.z==center.getZ())validateLoaded(level,r);
        else verifySource(level,new ChunkPos(x,z));
    }
    public static void accept(ServerLevel level,NaturalRuinProof proof){
        var data=AltarSavedData.get(level.getServer());var old=data.state.ruins.get(proof.id());
        if(old!=null && (old.x!=proof.center().getX() || old.y!=proof.center().getY() || old.z!=proof.center().getZ() || !old.dimension.equals(proof.dimension()) || old.integrity.equals("PERMANENTLY_INVALID")))return;
        var r=old==null?new AltarSavedData.Ruin():old;
        r.id=proof.id();r.dimension=proof.dimension();r.x=proof.center().getX();r.y=proof.center().getY();r.z=proof.center().getZ();
        r.village="["+proof.villageX()+", "+proof.villageZ()+"]";r.origin="NATURAL_WORLDGEN";r.integrity="INTACT";r.generationVersion=proof.version();r.lastVerified=level.getServer().overworld().getGameTime();
        data.state.ruins.put(r.id,r);data.setDirty();
    }
    public static boolean validateLoaded(ServerLevel level,AltarSavedData.Ruin r){
        var pos=new BlockPos(r.x,r.y,r.z);var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
        if(chunk==null)return true;
        boolean valid=chunk.getBlockState(pos).is(ModAltar.PEDESTAL.get());
        if(valid && r.generationVersion>=2)valid=chunk.getBlockEntity(pos) instanceof AncestralPedestalBlockEntity be && be.matches(r.id);
        if(!valid){r.integrity="PERMANENTLY_INVALID";AltarSavedData.get(level.getServer()).setDirty();}
        return valid;
    }
    public static Optional<AltarSavedData.Ruin> nearest(AltarSavedData data,String dimension,BlockPos pos,UUID player,int radius){
        return data.state.ruins.values().stream().filter(r->eligible(r,dimension,player) && RuinSelection.distanceSquared(pos,new BlockPos(r.x,r.y,r.z))<=(long)radius*radius)
                .min(Comparator.<AltarSavedData.Ruin>comparingLong(r->RuinSelection.distanceSquared(pos,new BlockPos(r.x,r.y,r.z))).thenComparing(r->r.id));
    }
    public static boolean eligible(AltarSavedData.Ruin r,String dimension,UUID player){return r.dimension.equals(dimension) && "NATURAL_WORLDGEN".equals(r.origin) && "INTACT".equals(r.integrity) && !r.completed.contains(player.toString());}
    public static CompletableFuture<CompoundTag> read(ServerLevel level,ChunkPos pos){
        var collector=new CollectToTag();return level.getChunkSource().chunkScanner().scanChunk(pos,collector).thenApply(ignored -> collector.getResult() instanceof CompoundTag tag?tag:new CompoundTag());
    }
    public static CompletableFuture<Optional<NaturalRuinProof>> verifySource(ServerLevel level,ChunkPos source){
        long seed=level.getSeed();String dimension=level.dimension().identifier().toString();
        var future=read(level,source).thenCompose(tag->{
            var proof=NaturalRuinProof.fromSource(tag,seed,dimension,source);
            if(proof.isEmpty())return CompletableFuture.completedFuture(Optional.<NaturalRuinProof>empty());
            return read(level,new ChunkPos(proof.get().center())).thenApply(target->proof.get().physicalPedestal(target)?proof:Optional.<NaturalRuinProof>empty());
        });
        future.thenAccept(proof->level.getServer().execute(()->{if(level.getServer().isRunning())proof.ifPresent(p->accept(level,p));}));
        return future;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var s=e.getServer();var waiting=WAITING.computeIfAbsent(s,k->new LinkedHashMap<>());
        // Drain only a small batch. Worldgen never blocks on disk or the main thread.
        for(int i=0;i<16;i++){var item=GENERATED.poll();if(item==null)break;if(item.server==s)waiting.put(item.proof.center(),item.proof);else GENERATED.add(item);}
        int budget=16;
        var deferred=new ArrayList<NaturalRuinProof>();
        for(var iterator=waiting.values().iterator();iterator.hasNext() && budget-->0;){var proof=iterator.next();var level=s.getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(proof.dimension())));
            if(level==null){iterator.remove();continue;}
            var chunk=level.getChunkSource().getChunkNow(proof.center().getX()>>4,proof.center().getZ()>>4);if(chunk==null){iterator.remove();deferred.add(proof);continue;}
            if(chunk.getBlockState(proof.center()).is(ModAltar.PEDESTAL.get()) && chunk.getBlockEntity(proof.center()) instanceof AncestralPedestalBlockEntity be && be.matches(proof.id()))accept(level,proof);
            iterator.remove();
        }
        for(var proof:deferred)waiting.put(proof.center(),proof);
        var queue=BACKFILL.get(s);
        if(queue!=null)for(int i=0;i<2 && !queue.isEmpty() && ACTIVE_READS.getOrDefault(s,0)<4;i++){
            ACTIVE_READS.merge(s,1,Integer::sum);verifySource(s.overworld(),queue.removeFirst()).whenComplete((proof,error)->s.execute(()->ACTIVE_READS.computeIfPresent(s,(key,count)->Math.max(0,count-1))));
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){WAITING.remove(e.getServer());BACKFILL.remove(e.getServer());ACTIVE_READS.remove(e.getServer());GENERATED.removeIf(g->g.server==e.getServer());}
}
