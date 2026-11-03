package br.com.soulbound.ritual;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.recall.SoulRecallDestinations;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.item.SoulEggIdentity;
import br.com.soulbound.registry.ModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import static net.minecraft.commands.Commands.literal;

/** Core spending is journaled before identity changes; recovery replays the exact authorized operation. */
@EventBusSubscriber(modid="soulbound")
public final class CompanionEvolution {
    private CompanionEvolution() {}
    public static boolean accepts(SoulboundRecord record,String kind,AltarSavedData d){
        return record!=null && record.species().equals("LUPUS")
                && (record.companionStatus()==CompanionStatus.DEAD?record.resurrectionRequired():!record.resurrectionRequired())
                && (!kind.equals("divine") || br.com.soulbound.companion.GodRules.canUnlock(record)) && (!kind.equals("ascension") || br.com.soulbound.companion.PlenoRules.canUnlock(record)) && AltarPolicy.accepts(kind,record.evolutionStage(),record.companionStatus().name(),d.progress(record.ownerUuid()).godUnlocked);
    }
    public static void applyCore(ServerPlayer p,ItemStack staff){
        var tag=staff.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        String id=tag.getString("SoulboundPendingCore").orElse("");if(id.isEmpty())return;
        tag.remove("SoulboundPendingCore");staff.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        var s=p.level().getServer();var d=AltarSavedData.get(s);var token=d.state.tokens.get(id);
        var identities=SoulboundSavedData.get(s);var record=identities.find(p.getUUID());
        if(!(p.containerMenu instanceof net.minecraft.world.inventory.CraftingMenu) || !p.isAlive() || p.isSpectator() || token==null || token.spent || token.kind.equals("spark") || record==null
                || !p.getUUID().equals(SoulEggIdentity.owner(staff)) || !record.bondUuid().equals(SoulEggIdentity.bond(staff))
                || !accepts(record,token.kind,d))return;
        if(token.kind.equals("awakening") && (!br.com.soulbound.companion.JuniorCorePolicy.accepts(record,token.owner,token.bond,token.spent)
                || br.com.soulbound.companion.CompanionFormService.ritualBusy(s,p.getUUID()) || !record.activeForm().equals("BABY")
                || br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,record)==null))return;
        if(token.kind.equals("ascension") && (!p.getStringUUID().equals(token.owner) || !record.bondUuid().toString().equals(token.bond) || br.com.soulbound.companion.CompanionFormService.ritualBusy(s,p.getUUID()) || br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,record)==null))return;
        if(token.kind.equals("divine") && (!br.com.soulbound.companion.GodRules.core(record,token.owner,token.bond,token.spent) || br.com.soulbound.companion.CompanionFormService.ritualBusy(s,p.getUUID()) || br.com.soulbound.companion.SoulStaffCommands.loadedCompanion(p,record)==null))return;
        if(token.kind.equals("restoration")){
            if(!completeResurrection(s,d,record,token))return;
        }else{
            token.spent=true;token.applicationOwner=p.getStringUUID();token.applicationBond=record.bondUuid().toString();token.applicationPending=true;d.flush(s);
            replay(s,d,token);
        }
        SoulboundIdentity.synchronizePlayer(p);
        p.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,p.getX(),p.getY()+1,p.getZ(),40,.6,.8,.6,.1);
    }
    public static boolean completeResurrection(MinecraftServer server,AltarSavedData d,SoulboundRecord record,AltarSavedData.Token token){
        if(!record.equals(SoulboundSavedData.get(server).find(record.ownerUuid())) || !record.canResurrect() || token.spent || !token.kind.equals("restoration"))return false;
        for(var level:server.getAllLevels())if(level.getEntity(record.companionUuid())!=null)return false;
        token.recoveryOnly=true;token.spent=true;token.delivered=true;token.applicationOwner=record.ownerUuid().toString();token.applicationBond=record.bondUuid().toString();
        token.previousResurrections=d.progress(record.ownerUuid()).resurrections;token.applicationPending=true;d.flush(server);
        replay(server,d,token);var player=server.getPlayerList().getPlayer(record.ownerUuid());if(player!=null)SoulboundIdentity.synchronizePlayer(player);return true;
    }
    private static void replay(MinecraftServer s,AltarSavedData d,AltarSavedData.Token t){
        if(t.kind.equals("talent_reset")){TalentResetService.replay(s,d,t);return;}
        var identities=SoulboundSavedData.get(s);UUID owner=UUID.fromString(t.applicationOwner);var record=identities.find(owner);
        if(record==null || !record.bondUuid().toString().equals(t.applicationBond))throw new IllegalStateException("Core journal bond mismatch");
        if(t.kind.equals("restoration")){
            if(t.recoveryOnly){
                if(record.canResurrect()){identities.put(record.recoverAfterDeath());identities.flush(s);}
                else if(!java.util.Set.of(CompanionStatus.READY,CompanionStatus.SUMMONING,CompanionStatus.ACTIVE).contains(record.companionStatus()))throw new IllegalStateException("Unexpected resurrection journal state");
                var progress=d.progress(owner);progress.resurrections=Math.max(progress.resurrections,t.previousResurrections+1);progress.godEnd=0;
            }else{
            // If reservation never reached disk, permit retry with the same core. Never recreate an ACTIVE/SUMMONING entity after restart.
            if(record.companionStatus()==CompanionStatus.DEAD){t.spent=false;var progress=d.progress(owner);progress.godUnlocked=t.previousGodUnlocked;progress.godEnd=t.previousGodEnd;progress.resurrections=t.previousResurrections;}
            }
        }else if(t.kind.equals("divine")){identities.put(record.withForms(record.forms().unlockGod()));identities.flush(s);}
        else{
            String target=t.kind.equals("awakening")?"JUNIOR":"PLENO";
            if(!record.evolutionStage().equals(target)){
                String expected=t.kind.equals("awakening")?"BABY":"JUNIOR";
                if(!record.evolutionStage().equals(expected))throw new IllegalStateException("Core journal stage mismatch");
                identities.put(t.kind.equals("awakening")?record.unlockJunior():record.unlockPleno());identities.flush(s);
            }
        }
        t.applicationPending=false;d.flush(s);
        var current=identities.find(owner);
        for(var level:s.getAllLevels()){var entity=level.getEntity(current.companionUuid());if(entity instanceof LupusCompanionEntity c)c.applyIdentity(current);}
    }
    @SubscribeEvent public static void started(ServerStartedEvent e){var d=AltarSavedData.get(e.getServer());for(var t:d.state.tokens.values())if(t.applicationPending)replay(e.getServer(),d,t);}
    public static void refresh(LupusCompanionEntity c){
        var level=(net.minecraft.server.level.ServerLevel)c.level();var s=level.getServer();var record=SoulboundSavedData.get(s).find(c.getOwnerUuid());
        if(record!=null)c.applyIdentity(record);
    }
    public static void tickGod(LupusCompanionEntity c){if(c.isGod() && c.tickCount%10==0)((net.minecraft.server.level.ServerLevel)c.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,c.getX(),c.getY()+1,c.getZ(),3,.45,.65,.45,.005);}
    public static void attributes(LupusCompanionEntity c){
        if(!(c.level() instanceof net.minecraft.server.level.ServerLevel level) || c.getOwnerUuid()==null)return;

        boolean god=c.isGod();
        double health=br.com.soulbound.companion.SurvivalRules.health(c.getEvolutionStage().name())*c.talents().health();
        double speed=god?br.com.soulbound.companion.GodRules.SPEED:switch(c.getEvolutionStage()){case BABY->.12;case JUNIOR->.28;case PLENO->br.com.soulbound.companion.PlenoRules.SPEED;case GOD->br.com.soulbound.companion.GodRules.SPEED;};
        c.healthBase(health,god);
        c.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(br.com.soulbound.Config.JUNIOR_ATTACK_DAMAGE.get()*(god?2.6:c.isPleno()?1.3:c.isBaby()?0:1));
        c.getAttribute(Attributes.FLYING_SPEED).setBaseValue(speed);c.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        c.getAttribute(Attributes.SCALE).setBaseValue(1);
        c.getAttribute(Attributes.ARMOR).setBaseValue(god?12:0); // Junior base armor is zero; multiplying by 1.15 remains zero.
        c.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(god?.6:c.isPleno()?Math.min(1,.2+.03*c.talents().guardian()):0);
    }
    public static float proportionalHealth(float health,double previousMax,double nextMax){
        if(!Float.isFinite(health) || !Double.isFinite(previousMax) || !Double.isFinite(nextMax) || previousMax<=0 || nextMax<=0)throw new IllegalArgumentException("Invalid companion health");
        return (float)Math.max(0,Math.min(nextMax,health*nextMax/previousMax));
    }
    public static boolean god(ServerPlayer p){return br.com.soulbound.companion.GodFormService.activate(p);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(literal("soulbound").then(literal("god").executes(c->{var p=c.getSource().getPlayerOrException();boolean ok=god(p);p.displayClientMessage(Component.translatable(ok?"message.soulbound.god_active":"message.soulbound.god_denied"),false);return ok?1:0;})));
    }
}
