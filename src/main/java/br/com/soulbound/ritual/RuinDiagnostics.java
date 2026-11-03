package br.com.soulbound.ritual;

import br.com.soulbound.companion.AncestralResonance;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** All locations, exclusions and index information are operator-only. */
@EventBusSubscriber(modid="soulbound")
public final class RuinDiagnostics {
    private RuinDiagnostics() {}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        if(net.neoforged.fml.loading.FMLEnvironment.isProduction())return;
        var root=Commands.literal("soulbound").then(Commands.literal("altar")
            .then(Commands.literal("ruin_village_probe").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("x",IntegerArgumentType.integer(-29000000,29000000)).then(Commands.argument("z",IntegerArgumentType.integer(-29000000,29000000)).executes(c->{
                    int x=IntegerArgumentType.getInteger(c,"x"),z=IntegerArgumentType.getInteger(c,"z");var level=c.getSource().getServer().overworld();
                    var chunk=level.getChunkSource().getChunkNow(x>>4,z>>4);if(chunk==null){c.getSource().sendSuccess(()->Component.literal("Source chunk not loaded; no loading requested."),false);return 0;}
                    int villages=0;
                    for(var entry:chunk.getAllStarts().entrySet())if(entry.getValue().isValid()){
                        String key=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).getKey(entry.getKey()).toString();
                        if(key.startsWith("minecraft:village_")){villages++;
                            c.getSource().sendSuccess(()->Component.literal("Generated village: "+key),false);
                            for(var piece:entry.getValue().getPieces())if(piece instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pool
                                    && pool.getElement() instanceof net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement single && RuinStructure.decorativeTemplate(single.getTemplateLocation().toString()))
                                c.getSource().sendSuccess(()->Component.literal("Decoration: "+single.getTemplateLocation()+" | "+pool.getBoundingBox()),false);
                        }
                        if(key.equals("soulbound:ancestral_ruin"))c.getSource().sendSuccess(()->Component.literal("Native ruin start: valid (verify pedestal/index separately)"),false);
                    }return villages;
                }))))
            .then(Commands.literal("ruins").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{
                var s=c.getSource().getServer();var data=AltarSavedData.get(s);
                c.getSource().sendSuccess(()->Component.literal("Ruin index: "+RuinIndex.status(s)),false);
                var p=c.getSource().getPlayer();var center=BlockPos.containing(c.getSource().getPosition());
                if(p!=null)RuinIndex.nearest(data,p.level().dimension().identifier().toString(),center,p.getUUID(),br.com.soulbound.Config.RESONANCE_RADIUS.get()).ifPresent(r->{
                    double distance=Math.sqrt(RuinStructure.horizontalSquared(center,new BlockPos(r.x,r.y,r.z)));
                    c.getSource().sendSuccess(()->Component.literal("Nearest eligible: "+r.id+" | distance="+Math.round(distance)),false);
                });
                for(var r:data.state.ruins.values().stream().sorted(java.util.Comparator.comparingLong(r->RuinStructure.horizontalSquared(center,new BlockPos(r.x,r.y,r.z)))).limit(12).toList()){
                    String reason=!"NATURAL_WORLDGEN".equals(r.origin)?"unverified origin":!"INTACT".equals(r.integrity)?"integrity="+r.integrity:p!=null && r.completed.contains(p.getStringUUID())?"completed":p!=null && !r.dimension.equals(p.level().dimension().identifier().toString())?"other dimension":"eligible (occupation/cooldown do not exclude)";
                    c.getSource().sendSuccess(()->Component.literal("Ruin "+r.id+" | "+r.x+" "+r.y+" "+r.z+" | "+r.village+" | "+reason),false);
                }return 1;
            }))
            .then(Commands.literal("ruin_scan").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("x",IntegerArgumentType.integer(-29000000,29000000)).then(Commands.argument("z",IntegerArgumentType.integer(-29000000,29000000)).executes(c->{
                    var sources=AncestralResonance.sources(c.getSource().getServer().overworld(),new BlockPos(IntegerArgumentType.getInteger(c,"x"),0,IntegerArgumentType.getInteger(c,"z")),br.com.soulbound.Config.RESONANCE_RADIUS.get());
                    RuinIndex.queueBackfill(c.getSource().getServer().overworld(),sources);c.getSource().sendSuccess(()->Component.literal("Queued disk-only scan: "+sources.size()+" starts; no chunks generated or loaded."),false);return sources.size();
                }))))
            .then(Commands.literal("ruin_candidates").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("regionX",IntegerArgumentType.integer(-10000,10000)).then(Commands.argument("regionZ",IntegerArgumentType.integer(-10000,10000)).then(Commands.argument("radius",IntegerArgumentType.integer(0,6)).executes(c->{
                    var level=c.getSource().getServer().overworld();int count=0,separation=1000;
                    for(var h:level.getChunkSource().getGeneratorState().possibleStructureSets())if(h.value().placement() instanceof RuinPlacement r)separation=r.minimumSeparation;
                    for(var h:level.getChunkSource().getGeneratorState().possibleStructureSets())if(h.unwrapKey().map(k->k.identifier().toString().equals("minecraft:villages")).orElse(false) && h.value().placement() instanceof RandomSpreadStructurePlacement v){
                        int x=IntegerArgumentType.getInteger(c,"regionX"),z=IntegerArgumentType.getInteger(c,"regionZ"),radius=IntegerArgumentType.getInteger(c,"radius");
                        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                            var candidate=v.getPotentialStructureChunk(level.getSeed(),(x+dx)*v.spacing(),(z+dz)*v.spacing());
                            if(RuinStructure.selected(level.getChunkSource().getGenerator(),level.getChunkSource().getGeneratorState().randomState(),level.registryAccess(),level,level.getSeed(),candidate)){
                                count++;c.getSource().sendSuccess(()->Component.literal("Selected village source: "+candidate.getMiddleBlockX()+" "+candidate.getMiddleBlockZ()+" (candidate only)"),false);
                            }
                        }
                    }return count;
                }))))));e.getDispatcher().register(root);
    }
}
