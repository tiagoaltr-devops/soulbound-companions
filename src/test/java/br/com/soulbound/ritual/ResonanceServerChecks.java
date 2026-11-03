package br.com.soulbound.ritual;

import java.util.UUID;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import br.com.soulbound.companion.BehaviorMode;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Dev-run only (-Pphase611LiveChecks); excluded from the published jar and normal runs. */
@EventBusSubscriber(modid="soulbound")
public final class ResonanceServerChecks {
    private static LupusCompanionEntity companion;
    private static FakePlayer owner;
    private static UUID uuid,bond;
    private static Vec3 anchor;
    private static int ticks,stage,checks;
    private static double farthest;
    private static void require(boolean condition,String text){if(!condition)throw new AssertionError(text);checks++;}
    @SubscribeEvent public static void start(ServerStartedEvent e){
        if(Boolean.getBoolean("soulbound.combatChecks") || Boolean.getBoolean("soulbound.survivalChecks"))return;
        var s=e.getServer();
        if(!s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().toString().contains("phase611-server"))throw new IllegalStateException("Live checks require isolated phase611-server directory");
        var level=s.overworld();
        for(int x=-1;x<=2;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,true);
        for(int x=-4;x<=25;x++)for(int z=-4;z<=4;z++){
            level.setBlockAndUpdate(new BlockPos(x,139,z),Blocks.STONE.defaultBlockState());
            for(int y=140;y<=147;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        owner=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"SoulboundFlightTest"));owner.setPos(0,140,0);
        level.players().add(owner);
        var record=SoulboundRecord.create(owner.getUUID(),"SoulboundFlightTest").withState(CompanionStatus.ACTIVE,false,"FOLLOW_PASSIVE");
        var data=SoulboundSavedData.get(s);data.put(record);
        companion=ModEntities.LUPUS_COMPANION.get().create(level,EntitySpawnReason.EVENT);companion.setUUID(record.companionUuid());companion.applyIdentity(record);companion.setPos(1,140.7,0);
        require(level.addFreshEntity(companion),"Canonical fixture entity admitted");uuid=companion.getUUID();bond=companion.getBondUuid();anchor=companion.position();
        require(companion.resonanceGoal().begin(new Vec3(1,0,0)),"Safe outbound path prepared");
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(companion==null)return;
        try {
            ticks++;farthest=Math.max(farthest,companion.position().distanceTo(anchor));
            if(ticks%40==0)LogUtils.getLogger().info("RESONANCE_SERVER_CHECKS progress stage={} elapsed={} entityTicks={} distance={}",stage,ticks,companion.tickCount,farthest);
            require(companion.getUUID().equals(uuid) && companion.getBondUuid().equals(bond),"Canonical identity preserved during flight");
            if(stage==0 && !companion.resonanceGoal().active()){
                require(ticks<205,"Flight ended before hard timeout");require(farthest>=7.5 && farthest<=14,"Real flight indicated approximately 8-12 blocks; observed="+farthest);
                require(companion.distanceToSqr(owner)<16,"Returned to owner");require(companion.getBehaviorMode()==BehaviorMode.FOLLOW_PASSIVE,"Follow restored");
                companion.commandBehaviorMode(owner,BehaviorMode.STAY);anchor=companion.getStayAnchor();farthest=0;ticks=0;stage=1;
                require(companion.resonanceGoal().begin(new Vec3(1,0,0)),"Stay guidance prepared");
            }else if(stage==1 && !companion.resonanceGoal().active()){
                require(farthest>=7.5 && farthest<=14,"Stay flight really moved 8-12 blocks; observed="+farthest);
                require(companion.getBehaviorMode()==BehaviorMode.STAY && anchor.equals(companion.getStayAnchor()),"Stay and original anchor preserved");
                require(companion.distanceToSqr(owner)<16,"Stay guidance returned near owner");
                require(companion.resonanceGoal().begin(new Vec3(1,0,0)),"Recall cancellation fixture prepared");
                require(companion.completeSoulRecall(owner) && !companion.resonanceGoal().active(),"Recall clears guidance without spawning");
                require(companion.resonanceGoal().begin(new Vec3(1,0,0)),"Disconnect fixture prepared");
                ((ServerLevel)companion.level()).players().remove(owner);ticks=0;stage=2;
            }else if(stage==2 && !companion.resonanceGoal().active()){
                require(ticks<10,"Missing owner cancels promptly");require(companion.getBehaviorMode()==BehaviorMode.FOLLOW_PASSIVE,"Recall's normal Follow remains after cancellation");
                require(((ServerLevel)companion.level()).getEntity(uuid)==companion,"Same native entity after indication and Recall cleanup");
                require(owner.experienceLevel==0,"Indication never charges experience levels");
                LogUtils.getLogger().info("RESONANCE_SERVER_CHECKS PASSED: {} checks; real navigation Follow/Stay/return and Recall cancellation",checks);
                cleanup();
            }
            if(ticks>240)throw new AssertionError("Resonance navigation fixture timed out");
        }catch(Throwable error){LogUtils.getLogger().error("RESONANCE_SERVER_CHECKS FAILED",error);cleanup();throw new IllegalStateException("Native resonance checks failed",error);}
    }
    private static void cleanup(){if(companion!=null){var level=(ServerLevel)companion.level();level.players().remove(owner);companion.discard();companion=null;for(int x=-1;x<=2;x++)for(int z=-1;z<=1;z++)level.setChunkForced(x,z,false);}}
}
