package br.com.soulbound.companion;

import br.com.soulbound.Config;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.recall.SoulRecallService;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.ritual.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/** Bounded asynchronous disk discovery. No chunk generation/loading, future.join or guessed destination. */
@EventBusSubscriber(modid="soulbound")
public final class AncestralResonance {
    public static final int MAX_SOURCES=512,MAX_REQUESTS=4;
    private static final Map<MinecraftServer,Map<UUID,Search>> SEARCHES=new IdentityHashMap<>();
    private static final class Search {
        final SoulboundRecord session; final InteractionHand hand; final ServerLevel level;
        final ArrayDeque<ChunkPos> sources; final Set<String> proven=new HashSet<>();
        final long deadline;int inFlight,examined;boolean technicalFailure;
        int coverage;
        BlockPos origin;
        Search(SoulboundRecord record,InteractionHand hand,ServerLevel level,List<ChunkPos> sources,long now){
            session=record;this.hand=hand;this.level=level;this.sources=new ArrayDeque<>(sources);deadline=now+400;
        }
    }
    private AncestralResonance() {}
    public static boolean pending(MinecraftServer server,UUID bond){return SEARCHES.containsKey(server) && SEARCHES.get(server).containsKey(bond);}
    public static int remainingSeconds(MinecraftServer server,UUID owner){return (int)Math.min(32767,Math.max(0,AltarSavedData.get(server).progress(owner).resonanceCooldownEnd-server.overworld().getGameTime()+19)/20);}
    private static void tell(ServerPlayer p,String key,Object... args){p.displayClientMessage(Component.translatable("message.soulbound.resonance_"+key,args),true);}
    public static boolean request(ServerPlayer p,InteractionHand hand,SoulboundRecord session){
        var server=p.level().getServer();var record=SoulStaffCommands.authorize(p,p.getItemInHand(hand));
        if(record==null || !record.matches(session.ownerUuid(),session.bondUuid(),session.companionUuid())){tell(p,"unavailable");return false;}
        var companion=SoulStaffCommands.loadedCompanion(p,record);
        if(companion==null || companion.distanceToSqr(p)>256 || companion.resonanceGoal().active() || SoulRecallService.inProgress(server,record.bondUuid())
                || !p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD)){tell(p,"unavailable");return false;}
        int seconds=remainingSeconds(server,p.getUUID());if(seconds>0){tell(p,"cooldown",seconds);return false;}
        var searches=SEARCHES.computeIfAbsent(server,k->new LinkedHashMap<>());
        if(searches.containsKey(record.bondUuid()) || searches.size()>=MAX_REQUESTS){tell(p,"searching");return false;}
        var search=new Search(record,hand,p.level(),sources(p.level(),p.blockPosition(),Config.RESONANCE_RADIUS.get()),server.overworld().getGameTime());
        search.coverage=Config.RESONANCE_RADIUS.get();
        search.origin=p.blockPosition().immutable();
        if(search.sources.size()==MAX_SOURCES)search.coverage=Math.max(0,(int)Math.sqrt(RuinSelection.distanceSquared(p.blockPosition(),search.sources.getLast().getMiddleBlockPosition(0)))-RuinSelection.MAX_CENTER_OFFSET);
        var data=AltarSavedData.get(server);
        for(var r:data.state.ruins.values())if(RuinIndex.eligible(r,p.level().dimension().identifier().toString(),p.getUUID())){
            var pos=new BlockPos(r.x,r.y,r.z);
            if(p.level().getChunkSource().getChunkNow(r.x>>4,r.z>>4)!=null && RuinIndex.validateLoaded(p.level(),r))search.proven.add(r.id);
        }
        searches.put(record.bondUuid(),search);tell(p,"searching");return true;
    }
    public static List<ChunkPos> sources(ServerLevel level,BlockPos center,int radius){
        var candidates=new HashSet<ChunkPos>();
        for(var holder:level.getChunkSource().getGeneratorState().possibleStructureSets())if(holder.unwrapKey().map(k->k.identifier().toString().equals("minecraft:villages")).orElse(false)
                && holder.value().placement() instanceof RandomSpreadStructurePlacement v){
            int spacing=v.spacing(),rx=Math.floorDiv(center.getX()>>4,spacing),rz=Math.floorDiv(center.getZ()>>4,spacing);
            int reach=(int)Math.ceil((radius+128.0)/(spacing*16))+1;
            for(int dx=-reach;dx<=reach;dx++)for(int dz=-reach;dz<=reach;dz++){
                var pos=v.getPotentialStructureChunk(level.getSeed(),(rx+dx)*spacing,(rz+dz)*spacing);
                if(RuinStructure.horizontalSquared(center,pos.getMiddleBlockPosition(0))<=(radius+128L)*(radius+128L))candidates.add(pos);
            }
        }
        // Include proven legacy starts even after a modpack changes its village placement.
        for(var r:AltarSavedData.get(level.getServer()).state.ruins.values())if(r.dimension.equals(level.dimension().identifier().toString()) && RuinStructure.horizontalSquared(center,new BlockPos(r.x,r.y,r.z))<=(long)radius*radius){
            try{var parts=r.village.replace("[","").replace("]","").split(",");candidates.add(new ChunkPos(Integer.parseInt(parts[0].trim()),Integer.parseInt(parts[1].trim())));}catch(RuntimeException ignored){}
        }
        return candidates.stream().sorted(Comparator.<ChunkPos>comparingLong(pos->RuinStructure.horizontalSquared(center,pos.getMiddleBlockPosition(0))).thenComparingInt(pos->pos.x).thenComparingInt(pos->pos.z)).limit(MAX_SOURCES).toList();
    }
    public static void cancel(ServerPlayer p,SoulboundRecord record){
        var server=p.level().getServer();var searches=SEARCHES.get(server);if(searches!=null)searches.remove(record.bondUuid());
        for(var level:server.getAllLevels())if(level.getEntity(record.companionUuid()) instanceof LupusCompanionEntity c && record.matches(c.getOwnerUuid(),c.getBondUuid(),c.getUUID()) && c.resonanceGoal().active())c.resonanceGoal().cancel();
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var server=e.getServer();var searches=SEARCHES.get(server);if(searches==null)return;
        int launches=2;
        for(var search:List.copyOf(searches.values())){
            var p=server.getPlayerList().getPlayer(search.session.ownerUuid());
            var record=p==null?null:SoulStaffCommands.authorize(p,p.getItemInHand(search.hand));
            if(p==null || record==null || p.level()!=search.level || !record.matches(search.session.ownerUuid(),search.session.bondUuid(),search.session.companionUuid())
                    || !record.behaviorMode().equals(search.session.behaviorMode()) || SoulRecallService.inProgress(server,search.session.bondUuid())){searches.remove(search.session.bondUuid());continue;}
            if(server.overworld().getGameTime()>search.deadline){searches.remove(record.bondUuid());tell(p,"technical");continue;}
            while(launches>0 && search.inFlight<2 && !search.sources.isEmpty()){
                var source=search.sources.removeFirst();search.inFlight++;search.examined++;launches--;
                RuinIndex.verifySource(search.level,source).whenComplete((proof,error)->server.execute(()->{
                    if(SEARCHES.get(server)!=searches || searches.get(search.session.bondUuid())!=search)return;
                    search.inFlight--;if(error!=null)search.technicalFailure=true;
                    else proof.ifPresent(value->{RuinIndex.accept(search.level,value);search.proven.add(value.id());});
                }));
            }
            if(!search.sources.isEmpty() || search.inFlight!=0)continue;
            searches.remove(record.bondUuid());
            if(search.technicalFailure){tell(p,"technical");continue;}
            var data=AltarSavedData.get(server);
            int coverage=Math.max(0,search.coverage-(int)Math.ceil(Math.sqrt(RuinSelection.distanceSquared(search.origin,p.blockPosition()))));
            var target=data.state.ruins.values().stream().filter(r->search.proven.contains(r.id) && RuinIndex.eligible(r,search.level.dimension().identifier().toString(),p.getUUID()) && RuinIndex.validateLoaded(search.level,r)
                    && RuinStructure.horizontalSquared(p.blockPosition(),new BlockPos(r.x,r.y,r.z))<=(long)coverage*coverage)
                    .min(Comparator.<AltarSavedData.Ruin>comparingLong(r->RuinStructure.horizontalSquared(p.blockPosition(),new BlockPos(r.x,r.y,r.z))).thenComparing(r->r.id));
            if(target.isEmpty()){tell(p,search.technicalFailure?"technical":"none");continue;}
            var companion=SoulStaffCommands.loadedCompanion(p,record);
            if(companion==null || companion.distanceToSqr(p)>256 || companion.getThreatSensor().nearestDanger(6)!=null){tell(p,"unavailable");continue;}
            Vec3 direction=new Vec3(target.get().x+.5-p.getX(),0,target.get().z+.5-p.getZ());
            if(direction.lengthSqr()<1 || !companion.resonanceGoal().begin(direction.normalize())){tell(p,"blocked");continue;}
            data.progress(p.getUUID()).resonanceCooldownEnd=server.overworld().getGameTime()+Config.RESONANCE_COOLDOWN.get();data.setDirty();server.overworld().getDataStorage().scheduleSave();tell(p,"found");
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){SEARCHES.remove(e.getServer());}
}
