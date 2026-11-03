package br.com.soulbound.companion.progression;

import br.com.soulbound.companion.identity.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only, executor-bound diagnostics; never accepts an owner or companion selector. */
@EventBusSubscriber(modid="soulbound")
public final class ProgressionCommands {
    private ProgressionCommands() {}
    /** Set XP within the level to zero. Reject reductions invalidating any existing talent. */
    public static ProgressionState level(SoulboundRecord record,int level){
        if(level<1 || level>ProgressionState.MAX_LEVEL || record.progression().spent()>level-1)throw new IllegalArgumentException("Invalid level or point budget");
        for(var entry:record.progression().talents().entrySet()){
            var talent=TalentDefinitions.find(entry.getKey());
            if(talent==null || !record.archetypeId().equals(talent.archetype()) || entry.getValue()>talent.maximum() || level<talent.minimumLevel()
                    || talent.branch()!=TalentDefinitions.Branch.MAIN && !record.unlockedForms().contains(talent.branch().name())
                    || TalentDefinitions.branchPoints(record.progression(),talent.branch())-entry.getValue()<talent.branchPoints()
                    || talent.previous()!=null && record.progression().rank(talent.previous())<1)throw new IllegalArgumentException("Invalid talent investment");
        }
        var old=record.progression();return new ProgressionState(level,0,level-1,old.talents(),old.rewards(),old.furyEnd(),old.furyCooldownEnd(),old.bonusCarry());
    }
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        if(net.neoforged.fml.loading.FMLEnvironment.isProduction())return;
        event.getDispatcher().register(Commands.literal("soulbound").then(Commands.literal("debug")
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("setlevel").then(Commands.argument("level",IntegerArgumentType.integer(1,30)).executes(c->execute(c,true))))
            .then(Commands.literal("addxp").then(Commands.argument("amount",LongArgumentType.longArg(1)).executes(c->execute(c,false))))));
    }
    private static int execute(CommandContext<CommandSourceStack> context,boolean setLevel)throws CommandSyntaxException{
        var source=context.getSource();var player=source.getPlayerOrException();var server=player.level().getServer();
        var data=SoulboundSavedData.get(server);var record=data.find(player.getUUID());
        if(record==null){source.sendFailure(Component.translatable("message.soulbound.debug.no_bond"));return 0;}
        if(br.com.soulbound.companion.CompanionFormService.ritualBusy(server,player.getUUID())){source.sendFailure(Component.translatable("message.soulbound.debug.busy"));return 0;}
        ProgressionState next;
        try{next=setLevel?level(record,IntegerArgumentType.getInteger(context,"level")):record.progression().gain(LongArgumentType.getLong(context,"amount"));}
        catch(IllegalArgumentException ex){source.sendFailure(Component.translatable("message.soulbound.debug.invalid_talents"));return 0;}
        var updated=record.withProgression(next);data.put(updated);data.flush(server);ProgressionService.publish(server,updated);
        source.sendSuccess(()->Component.translatable("message.soulbound.debug.success",next.level(),next.xp(),next.available(),next.spent()),false);return 1;
    }
}
