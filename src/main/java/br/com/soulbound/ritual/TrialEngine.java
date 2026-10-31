package br.com.soulbound.ritual;

import java.util.*;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid="soulbound")
public final class TrialEngine {
    private static final Set<String> LIVE_RUNS=new HashSet<>();
    public static final String GUARD="SoulboundTrialGuard";
    private TrialEngine() {}
    public static void open(ServerPlayer p,AltarSavedData.Ruin r) {
        p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new TrialMenu(id,inv,r.id),Component.translatable("menu.soulbound.trial")));
    }
    static boolean inside(ServerPlayer p,AltarSavedData.Ruin r) {
        return p.level().dimension().identifier().toString().equals(r.dimension)
                && RuinStructure.horizontalSquared(p.blockPosition(),new BlockPos(r.x,r.y,r.z))<=48L*48;
    }
    static boolean exclusive(MinecraftServer s,AltarSavedData.Ruin r,String owner) {
        return s.getPlayerList().getPlayers().stream().noneMatch(p->!p.isSpectator() && !p.getStringUUID().equals(owner) && inside(p,r));
    }
    public static boolean start(ServerPlayer p,AltarSavedData.Ruin r) {
        var s=p.level().getServer();var d=AltarSavedData.get(s);long now=s.overworld().getGameTime();
        var record=SoulboundSavedData.get(s).find(p.getUUID());
        if (!"NATURAL_WORLDGEN".equals(r.origin) || !"INTACT".equals(r.integrity) || !p.isAlive() || p.isSpectator() || !inside(p,r) || r.trial!=null || r.completed.contains(p.getStringUUID())
                || AltarPolicy.trialCoolingDown(r.cooldowns.getOrDefault(p.getStringUUID(),0L),now,br.com.soulbound.Config.TRIAL_COOLDOWN.get()) || record==null
                || record.companionStatus()!=CompanionStatus.ACTIVE || !exclusive(s,r,p.getStringUUID())) return false;
        var t=new AltarSavedData.Trial();t.player=p.getStringUUID();t.run=UUID.randomUUID().toString();t.started=now;
        t.sequence=new int[AltarPolicy.symbols(r.completions)];for(int i=0;i<t.sequence.length;i++)t.sequence[i]=p.getRandom().nextInt(7);
        r.trial=t;LIVE_RUNS.add(t.run);d.flush(s);return true;
    }
    public static boolean symbol(ServerPlayer p,AltarSavedData.Ruin r,int symbol) {
        var t=r.trial;if(t==null || !t.player.equals(p.getStringUUID()) || t.phase!=0 || symbol<0 || symbol>6)return false;
        if(t.sequence[t.cursor]!=symbol){cancel(p.level().getServer(),r);return false;}
        p.level().playSound(null,p.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.7f,1+symbol*.1f);
        if(++t.cursor==t.sequence.length){t.phase=1;t.cursor=0;}
        AltarSavedData.get(p.level().getServer()).setDirty();return true;
    }
    public static void cancel(MinecraftServer s,AltarSavedData.Ruin r) {
        var t=r.trial;if(t==null)return;
        r.cooldowns.put(t.player,s.overworld().getGameTime()+br.com.soulbound.Config.TRIAL_COOLDOWN.get());
        LIVE_RUNS.remove(t.run);
        for(var level:s.getAllLevels())for(String uuid:t.guards){var entity=level.getEntity(UUID.fromString(uuid));if(entity!=null)entity.discard();}
        r.trial=null;AltarSavedData.get(s).flush(s);
        var player=s.getPlayerList().getPlayer(UUID.fromString(t.player));
        if(player!=null)player.displayClientMessage(Component.translatable("message.soulbound.trial_cancelled",br.com.soulbound.Config.TRIAL_COOLDOWN.get()/20),false);
    }
    @SubscribeEvent public static void started(ServerStartedEvent e) {
        LIVE_RUNS.clear();var d=AltarSavedData.get(e.getServer());
        for(var r:d.state.ruins.values())if(r.trial!=null)cancel(e.getServer(),r);
    }
    @SubscribeEvent public static void join(EntityJoinLevelEvent e) {
        if(e.getLevel().isClientSide())return;
        String run=e.getEntity().getPersistentData().getString(GUARD).orElse("");
        if(e.getEntity() instanceof br.com.soulbound.entity.SpiritGuardianEntity && run.isEmpty()){e.setCanceled(true);return;}
        if(!run.isEmpty() && !LIVE_RUNS.contains(run))e.setCanceled(true);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent e) {
        if(e.getEntity().getPersistentData().contains(GUARD))e.setCanceled(true);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        var s=e.getServer();var d=AltarSavedData.get(s);
        for(var r:d.state.ruins.values()) {
            var t=r.trial;if(t==null)continue;
            var p=s.getPlayerList().getPlayer(UUID.fromString(t.player));
            if(p==null || !p.isAlive() || !inside(p,r) || !exclusive(s,r,t.player) || s.overworld().getGameTime()-t.started>12000){cancel(s,r);continue;}
            var level=p.level();
            if(t.phase==1) {
                boolean missing=false;
                for(var iterator=t.guards.iterator();iterator.hasNext();) {
                    var entity=level.getEntity(UUID.fromString(iterator.next()));
                    if(entity==null){missing=true;break;}
                    if(!entity.isAlive())iterator.remove();
                }
                if(missing){cancel(s,r);continue;}
                int total=AltarPolicy.guardians(r.completions);
                while(t.guards.size()<Math.min(br.com.soulbound.Config.MAX_SIMULTANEOUS_GUARDIANS.get(),total) && t.spawned<total) {
                    Zombie guard=br.com.soulbound.registry.ModEntities.SPIRIT_GUARDIAN.get().create(level,EntitySpawnReason.EVENT);if(guard==null){cancel(s,r);break;}
                    guard.getPersistentData().putString(GUARD,t.run);guard.setCustomName(Component.translatable("entity.soulbound.spirit_guardian"));
                    guard.setGlowingTag(true);guard.setPersistenceRequired();guard.setTarget(p);
                    guard.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));guard.setDropChance(EquipmentSlot.HEAD,0);
                    guard.getAttribute(Attributes.MAX_HEALTH).setBaseValue(12+Math.min(10,r.completions)*2);guard.setHealth(guard.getMaxHealth());
                    guard.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2+Math.min(6,r.completions)*.5);
                    boolean room=false;
                    for(double distance:new double[]{5,3,2}){
                        for(int attempt=0;attempt<8;attempt++){
                            double angle=t.spawned*2.4+attempt*Math.PI/4;
                            guard.setPos(r.x+.5+Math.cos(angle)*distance,r.y+1,r.z+.5+Math.sin(angle)*distance);
                            if(level.hasChunkAt(guard.blockPosition()) && level.noCollision(guard)){room=true;break;}
                        }if(room)break;
                    }
                    if(!room || !level.addFreshEntity(guard)){cancel(s,r);break;}
                    t.guards.add(guard.getStringUUID());t.spawned++;d.setDirty();
                }
                if(r.trial==t && t.spawned==total && t.guards.isEmpty()){t.phase=2;t.ticks=0;d.setDirty();}
            } else if(t.phase==2) {
                var record=SoulboundSavedData.get(s).find(p.getUUID());
                var entity=record==null?null:level.getEntity(record.companionUuid());
                if(!(entity instanceof LupusCompanionEntity companion) || !SoulboundIdentity.isCanonical(companion)) {cancel(s,r);continue;}
                if(companion.distanceToSqr(r.x+.5,r.y+1,r.z+.5)<=9) {
                    t.ticks++;
                    if(t.ticks%10==0)level.sendParticles(ParticleTypes.SOUL,r.x+.5,r.y+1.5,r.z+.5,6,.5,.5,.5,.01);
                    if(t.ticks>=AltarPolicy.bondTicks(r.completions)) {
                        r.completed.add(t.player);r.completions++;LIVE_RUNS.remove(t.run);r.trial=null;
                        AltarTokens.issue(d,t.player,"spark");d.flush(s);AltarTokens.deliver(p,d);
                        p.displayClientMessage(Component.translatable("message.soulbound.trial_complete"),false);
                    }
                }else t.ticks=0;
            }
        }
        if(s.overworld().getGameTime()%20==0)for(var p:s.getPlayerList().getPlayers()){InventoryTransfers.recover(p);AltarTokens.deliver(p,d);}
    }
    @SubscribeEvent public static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){
        var attacker=e.getSource().getEntity();
        if(attacker!=null && attacker.getPersistentData().contains(GUARD)){
            String run=attacker.getPersistentData().getString(GUARD).orElse("");
            boolean valid=false;
            if(attacker.level() instanceof ServerLevel level)for(var r:AltarSavedData.get(level.getServer()).state.ruins.values())
                if(r.trial!=null && r.trial.run.equals(run) && e.getEntity().getStringUUID().equals(r.trial.player)){valid=true;break;}
            if(!valid)e.setCanceled(true);
        }
    }
}
