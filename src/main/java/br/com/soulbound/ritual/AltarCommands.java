package br.com.soulbound.ritual;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mojang.brigadier.arguments.IntegerArgumentType;

/** Diagnostics do not generate or force-load any chunk. */
@EventBusSubscriber(modid="soulbound")
public final class AltarCommands {
    private AltarCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent e){
        if(net.neoforged.fml.loading.FMLEnvironment.isProduction())return;
        e.getDispatcher().register(Commands.literal("soulbound").then(Commands.literal("altar")
            .then(Commands.literal("inspect").executes(c->{
                var p=c.getSource().getPlayerOrException();var d=AltarSavedData.get(p.level().getServer());
                var forge=ForgeService.at(p.level(),p.blockPosition().below());
                if(forge==null)forge=d.state.forges.values().stream().filter(f->f.dimension.equals(p.level().dimension().identifier().toString()) && p.distanceToSqr(f.x,f.y,f.z)<64).findFirst().orElse(null);
                if(forge!=null){String text=forge.id+" | "+forge.state+" | owner="+forge.owner+" | attempts="+forge.attempts;c.getSource().sendSuccess(()->Component.literal(text),false);}
                for(var r:d.state.ruins.values())if(TrialEngine.inside(p,r))c.getSource().sendSuccess(()->Component.literal(r.id+" | completions="+r.completions+" | completed="+r.completed.contains(p.getStringUUID())+" | cooldown="+r.cooldowns.getOrDefault(p.getStringUUID(),0L)),false);
                return 1;
            }))
            .then(Commands.literal("candidate").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("cellX",IntegerArgumentType.integer(-1000,1000)).then(Commands.argument("cellZ",IntegerArgumentType.integer(-1000,1000)).executes(c->{
                    var level=c.getSource().getLevel();int cx=IntegerArgumentType.getInteger(c,"cellX"),cz=IntegerArgumentType.getInteger(c,"cellZ");
                    for(var h:level.getChunkSource().getGeneratorState().possibleStructureSets()){
                        if(h.unwrapKey().map(k->k.identifier().toString().equals("minecraft:villages")).orElse(false) && h.value().placement() instanceof RandomSpreadStructurePlacement v){
                            int cell=Math.max(512,v.spacing()*8);var candidate=v.getPotentialStructureChunk(level.getSeed(),cx*cell+cell/2,cz*cell+cell/2);
                            c.getSource().sendSuccess(()->Component.literal("Candidate only; no chunks loaded: x="+(candidate.getMiddleBlockX()+32)+", z="+candidate.getMiddleBlockZ()),false);return 1;
                        }
                    }return 0;
                }))))));
    }
}
