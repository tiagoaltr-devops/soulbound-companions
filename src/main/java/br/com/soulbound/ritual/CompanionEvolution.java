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
                && AltarPolicy.accepts(kind,record.evolutionStage(),record.companionStatus().name(),d.progress(record.ownerUuid()).godUnlocked);
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
        if(token.kind.equals("restoration")){
            for(var world:s.getAllLevels())if(world.getEntity(record.companionUuid())!=null)return;
            var companion=ModEntities.LUPUS_COMPANION.get().create(p.level(),EntitySpawnReason.MOB_SUMMONED);if(companion==null)return;
            var position=SoulRecallDestinations.find(p,companion);if(position==null)return;
            var progress=d.progress(p.getUUID());token.previousGodUnlocked=progress.godUnlocked;token.previousGodEnd=progress.godEnd;token.previousResurrections=progress.resurrections;
            token.spent=true;token.applicationOwner=p.getStringUUID();token.applicationBond=record.bondUuid().toString();token.applicationPending=true;
            progress.godUnlocked=false;progress.godEnd=0;progress.resurrections++;d.flush(s);
            var next=record.withEvolution("BABY",0,0).withState(CompanionStatus.SUMMONING,false,"FOLLOW_PASSIVE");
            identities.put(next);identities.flush(s);
            companion.setUUID(record.companionUuid());companion.applyIdentity(next);companion.setPos(position);
            if(!p.level().addFreshEntity(companion)){
                identities.put(record);identities.flush(s);token.spent=false;progress.godUnlocked=token.previousGodUnlocked;progress.godEnd=token.previousGodEnd;progress.resurrections=token.previousResurrections;token.applicationPending=false;d.flush(s);return;
            }
            token.applicationPending=false;d.flush(s);
        }else{
            token.spent=true;token.applicationOwner=p.getStringUUID();token.applicationBond=record.bondUuid().toString();token.applicationPending=true;d.flush(s);
            replay(s,d,token);
        }
        SoulboundIdentity.synchronizePlayer(p);
        p.level().sendParticles(ParticleTypes.TOTEM_OF_UNDYING,p.getX(),p.getY()+1,p.getZ(),40,.6,.8,.6,.1);
    }
    private static void replay(MinecraftServer s,AltarSavedData d,AltarSavedData.Token t){
        var identities=SoulboundSavedData.get(s);UUID owner=UUID.fromString(t.applicationOwner);var record=identities.find(owner);
        if(record==null || !record.bondUuid().toString().equals(t.applicationBond))throw new IllegalStateException("Core journal bond mismatch");
        if(t.kind.equals("restoration")){
            // If reservation never reached disk, permit retry with the same core. Never recreate an ACTIVE/SUMMONING entity after restart.
            if(record.companionStatus()==CompanionStatus.DEAD){t.spent=false;var progress=d.progress(owner);progress.godUnlocked=t.previousGodUnlocked;progress.godEnd=t.previousGodEnd;progress.resurrections=t.previousResurrections;}
        }else if(t.kind.equals("divine")){d.progress(owner).godUnlocked=true;}
        else{
            String target=t.kind.equals("awakening")?"JUNIOR":"PLENO";
            if(!record.evolutionStage().equals(target)){
                String expected=t.kind.equals("awakening")?"BABY":"JUNIOR";
                if(!record.evolutionStage().equals(expected))throw new IllegalStateException("Core journal stage mismatch");
                identities.put(record.withEvolution(target,record.evolutionProgress(),record.godCooldownEnd()));identities.flush(s);
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
    public static void tickGod(LupusCompanionEntity c){
        var level=(net.minecraft.server.level.ServerLevel)c.level();var s=level.getServer();
        var d=AltarSavedData.get(s);var p=d.progress(c.getOwnerUuid());
        if(p.godEnd!=0 && p.godEnd<=s.overworld().getGameTime()){p.godEnd=0;d.setDirty();attributes(c);}
    }
    public static void attributes(LupusCompanionEntity c){
        if(!(c.level() instanceof net.minecraft.server.level.ServerLevel level) || c.getOwnerUuid()==null)return;
        var d=AltarSavedData.get(level.getServer());var progress=d.progress(c.getOwnerUuid());
        boolean god=progress.godUnlocked && progress.godEnd>level.getServer().overworld().getGameTime();
        double health=god?80:switch(c.getEvolutionStage()){case BABY->12;case JUNIOR->24;case PLENO->40;};
        double speed=god?.20:switch(c.getEvolutionStage()){case BABY->.12;case JUNIOR->.14;case PLENO->.16;};
        double old=c.getMaxHealth();
        if(old!=health){c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);c.setHealth((float)Math.min(health,c.getHealth()*health/old));}
        c.getAttribute(Attributes.FLYING_SPEED).setBaseValue(speed);c.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        c.getAttribute(Attributes.SCALE).setBaseValue(god?1.7:switch(c.getEvolutionStage()){case BABY->1;case JUNIOR->1.2;case PLENO->1.45;});
    }
    public static boolean god(ServerPlayer p){
        var s=p.level().getServer();var d=AltarSavedData.get(s);var progress=d.progress(p.getUUID());var identities=SoulboundSavedData.get(s);var r=identities.find(p.getUUID());long now=s.overworld().getGameTime();
        if(r==null || !r.evolutionStage().equals("PLENO") || r.companionStatus()!=CompanionStatus.ACTIVE || !progress.godUnlocked || r.godCooldownEnd()>now)return false;
        progress.godEnd=now+1200;identities.put(r.withEvolution(r.evolutionStage(),r.evolutionProgress(),now+18000));
        identities.flush(s);d.flush(s);
        for(var level:s.getAllLevels()){var entity=level.getEntity(r.companionUuid());if(entity instanceof LupusCompanionEntity c)attributes(c);}
        return true;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(literal("soulbound").then(literal("god").executes(c->{var p=c.getSource().getPlayerOrException();boolean ok=god(p);p.displayClientMessage(Component.translatable(ok?"message.soulbound.god_active":"message.soulbound.god_denied"),false);return ok?1:0;})));
    }
}
