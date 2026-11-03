package br.com.soulbound.companion;

import java.util.UUID;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.registry.ModEntities;
import br.com.soulbound.registry.ModItems;
import br.com.soulbound.item.SoulEggIdentity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in native AI/damage fixture in the existing isolated dev world, never included in the mod jar. */
@EventBusSubscriber(modid="soulbound")
public final class CombatServerChecks {
    private static LupusCompanionEntity c;private static FakePlayer owner;private static Mob enemy;
    private static int ticks,stage,checks;private static float before;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    @SubscribeEvent public static void start(ServerStartedEvent event){
        if(!Boolean.getBoolean("soulbound.combatChecks"))return;var s=event.getServer();
        if(!s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().toString().contains("phase611-server"))throw new IllegalStateException("Requires isolated world");
        var level=s.overworld();level.setChunkForced(0,0,true);
        for(int x=0;x<12;x++)for(int z=0;z<10;z++){level.setBlockAndUpdate(new BlockPos(x,139,z),Blocks.STONE.defaultBlockState());for(int y=140;y<145;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        owner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"JuniorCombatTest"));owner.setPos(2,140,3);owner.setInvulnerable(true);level.players().add(owner);
        var r=SoulboundRecord.create(owner.getUUID(),"JuniorCombatTest").unlockJunior().withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT");r=r.withForms(r.forms().select("JUNIOR",0));SoulboundSavedData.get(s).put(r);
        c=ModEntities.LUPUS_COMPANION.get().create(level,EntitySpawnReason.EVENT);c.setUUID(r.companionUuid());c.applyIdentity(r);c.setPos(3,140,3);check(level.addFreshEntity(c),"Canonical Junior admitted");
        check(c.getMaxHealth()==33.6f && c.getHealth()==33.6f,"Junior initializes with normalized full health");
        c.setHealth(12);var halfBaby=r.withForms(new CompanionForms(r.archetypeId(),r.unlockedForms(),"BABY",0,0));SoulboundSavedData.get(s).put(halfBaby);c.applyIdentity(halfBaby);
        check(c.getMaxHealth()==33.6f && c.getHealth()==12,"Baby switch preserves absolute health");c.applyIdentity(halfBaby);check(c.getHealth()==12,"Refresh does not heal");
        SoulboundSavedData.get(s).put(r);c.applyIdentity(r);check(c.getMaxHealth()==33.6f && c.getHealth()==12,"Junior switch preserves absolute health");c.setHealth(33.6f);
        var stack=new net.minecraft.world.item.ItemStack(ModItems.COMPANION_EGG.get());SoulEggIdentity.refresh(stack,r);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        enemy=spawn(level,EntityType.ZOMBIE);before=enemy.getHealth();
        c.hurtServer(level,c.damageSources().mobAttack(enemy),3);check(Math.abs(c.getHealth()-30.6)<.001,"Three damage does not kill full-health Junior");c.setHealth(33.6f);c.invulnerableTime=0;
        check(c.canFight(),"Junior combat enabled");check(JuniorCombat.target(c,enemy),"Zombie valid target");
    }
    private static <T extends Mob> T spawn(ServerLevel level,EntityType<T> type){var mob=type.create(level,EntitySpawnReason.EVENT);mob.setPos(4.2,140,3);mob.setNoAi(true);mob.setInvulnerable(false);mob.setItemSlot(EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));level.addFreshEntity(mob);return mob;}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(c==null)return;try{
            ticks++;
            if(stage==0 || stage==1){
                if(enemy.getHealth()<before){check(true,stage==0?"Melee damaged zombie":"Melee damaged skeleton");enemy.discard();
                    if(stage++==0){enemy=spawn((ServerLevel)c.level(),EntityType.SKELETON);before=enemy.getHealth();ticks=0;}
                    else {enemy=spawn((ServerLevel)c.level(),EntityType.ZOMBIE);before=enemy.getHealth();c.commandBehaviorMode(owner,BehaviorMode.STAY);ticks=0;}
                }else if(ticks>100)throw new AssertionError("No melee damage: mode="+c.getBehaviorMode()+", canFight="+c.canFight()+", target="+c.getTarget()+", distance="+c.distanceTo(enemy));
            }else if(stage==2 && ticks>=30){
                check(enemy.getHealth()==before,"Stay does not attack");c.commandBehaviorMode(owner,BehaviorMode.FOLLOW_PASSIVE);stage=3;ticks=0;
            }else if(stage==3 && ticks>=30){
                check(enemy.getHealth()==before,"Passive does not attack");
                var villager=EntityType.VILLAGER.create((ServerLevel)c.level(),EntitySpawnReason.EVENT);var cow=EntityType.COW.create((ServerLevel)c.level(),EntitySpawnReason.EVENT);
                check(!JuniorCombat.target(c,villager),"Villager rejected");check(!JuniorCombat.target(c,cow),"Cow rejected");check(!JuniorCombat.target(c,owner),"Player rejected");
                var other=ModEntities.LUPUS_COMPANION.get().create((ServerLevel)c.level(),EntitySpawnReason.EVENT);check(!JuniorCombat.target(c,other),"Companion rejected");
                c.commandBehaviorMode(owner,BehaviorMode.FOLLOW_COMBAT);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
                check(CompanionFormService.ultimate(owner),"Ultimate accepted without held staff");check(enemy.getHealth()<before,"Ultimate damaged hostile");
                check(!CompanionFormService.ultimate(owner),"Duplicate ultimate blocked");
                var r=SoulboundSavedData.get(event.getServer()).find(owner.getUUID());check(r.forms().ultimateEnd()-event.getServer().overworld().getGameTime()==1200,"60-second cooldown recorded");
                long now=event.getServer().overworld().getGameTime();var baby=r.withForms(r.forms().select("BABY",now));SoulboundSavedData.get(event.getServer()).put(baby);c.applyIdentity(baby);
                check(!c.canFight(),"Baby cannot fight after switching from Junior");check(!CompanionFormService.ultimate(owner),"Baby ultimate blocked");
                check(c.getUUID().equals(r.companionUuid()),"Same entity after form switch");
                c.remove(Entity.RemovalReason.KILLED);stage=4;ticks=0;
            }else if(stage==4 && ticks>=2){
                var server=event.getServer();var identities=SoulboundSavedData.get(server);var dead=identities.find(owner.getUUID());
                check(dead.companionStatus()==CompanionStatus.DEAD && dead.resurrectionRequired(),"KILLED event persists DEAD");
                var d=br.com.soulbound.ritual.AltarSavedData.get(server);var token=br.com.soulbound.ritual.AltarTokens.issue(d,owner.getStringUUID(),"restoration");
                check(br.com.soulbound.ritual.CompanionEvolution.completeResurrection(server,d,dead,token),"Resurrection journal completed");
                var recovered=identities.find(owner.getUUID());check(recovered.canSummon() && recovered.activeForm().equals("BABY"),"Resurrection permits Baby invocation");
                check(recovered.unlockedForms().contains("JUNIOR") && recovered.evolutionStage().equals("JUNIOR"),"Permanent Junior preserved");
                check(!br.com.soulbound.ritual.CompanionEvolution.completeResurrection(server,d,dead,token),"Duplicate resurrection rejected");
                var next=recovered.withState(CompanionStatus.SUMMONING,false,"FOLLOW_PASSIVE");identities.put(next);identities.flush(server);
                var level=(ServerLevel)c.level();var old=c.getUUID();c=ModEntities.LUPUS_COMPANION.get().create(level,EntitySpawnReason.EVENT);c.setUUID(old);c.applyIdentity(next);c.setPos(3,140,3);
                check(level.addFreshEntity(c),"Same reserved UUID admitted after recovery");check(c.getMaxHealth()==12 && c.getHealth()==12,"Recovered Baby initializes at 12 health");
                check(identities.find(owner.getUUID()).companionStatus()==CompanionStatus.ACTIVE,"Recovered invocation is ACTIVE");
                check(identities.find(owner.getUUID()).forms().canSelect("JUNIOR",server.overworld().getGameTime()+60),"Junior selectable again after existing cooldown");
                var jr=identities.find(owner.getUUID()).withForms(new CompanionForms(recovered.archetypeId(),recovered.unlockedForms(),"JUNIOR",0,0)).withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT");
                identities.put(jr);c.applyIdentity(jr);stage=5;ticks=0;
            }else if(stage==5 && ticks>=35){
                enemy.discard();var level=(ServerLevel)c.level();enemy=spawn(level,EntityType.ZOMBIE);enemy.setNoAi(false);
                enemy.finalizeSpawn(level,level.getCurrentDifficultyAt(enemy.blockPosition()),EntitySpawnReason.NATURAL,null);
                check(c.canFight(),"Junior combat preserved with progression");check(JuniorCombat.hit(c,enemy,100),"Confirmed natural-hostile damage");stage=6;ticks=0;
            }else if(stage==6 && enemy.isRemoved()){
                var server=event.getServer();var identities=SoulboundSavedData.get(server);var r=identities.find(owner.getUUID());
                check(r.progression().xp()==8,"Natural hostile death granted 8 bond XP");
                check(!br.com.soulbound.companion.progression.ProgressionService.reward(server,owner.getUUID(),"combat:"+enemy.getUUID(),8,false),"Combat reward deduplicated");
                br.com.soulbound.companion.progression.ProgressionService.reward(server,owner.getUUID(),"fixture:level",200,false);r=identities.find(owner.getUUID());
                var before=r.progression();check(before.level()>1 && before.available()>0,"Native level-up earned talent points");
                check(br.com.soulbound.companion.progression.ProgressionService.invest(owner,r,"soulbound:vitalidade_ancestral"),"Native talent investment");
                check(Math.abs(c.getMaxHealth()-24.96)<.001,"Talent maximum health applied once");c.setHealth(c.getMaxHealth()/2);c.applyIdentity(identities.find(owner.getUUID()));
                check(Math.abs(c.getMaxHealth()-24.96)<.001 && Math.abs(c.getHealth()-12.48)<.001,"Attribute refresh does not stack or heal");
                var maximum=c.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);var extra=net.minecraft.resources.Identifier.fromNamespaceAndPath("soulbound","test_external_health");
                maximum.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(extra,2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
                c.applyIdentity(identities.find(owner.getUUID()));c.applyIdentity(identities.find(owner.getUUID()));
                check(Math.abs(c.getMaxHealth()-26.96)<.001 && Math.abs(c.getHealth()-12.48)<.001,"External health modifier does not cause repeated health scaling");maximum.removeModifier(extra);
                var d=br.com.soulbound.ritual.AltarSavedData.get(server);var token=br.com.soulbound.ritual.AltarTokens.issue(d,owner.getStringUUID(),"talent_reset");
                token.spent=true;token.delivered=true;token.applicationOwner=owner.getStringUUID();token.applicationBond=r.bondUuid().toString();token.applicationPending=true;d.flush(server);
                br.com.soulbound.ritual.TalentResetService.replay(server,d,token);var reset=identities.find(owner.getUUID());
                check(reset.progression().spent()==0 && reset.progression().available()==before.earnedPoints(),"Reset journal returns earned points");
                check(reset.progression().level()==before.level() && reset.progression().xp()==before.xp(),"Reset preserves XP/level");
                check(Math.abs(c.getMaxHealth()-24)<.001 && Math.abs(c.getHealth()-12)<.001,"Reset removes health bonus proportionally");
                check(reset.matches(r.ownerUuid(),r.bondUuid(),r.companionUuid()) && reset.forms().equals(r.forms()),"Reset preserves canonical identity and form");
                com.mojang.logging.LogUtils.getLogger().info("COMBAT_SERVER_CHECKS PASSED: {} native checks including progression, talents and reset journal",checks);cleanup();
            }
            if(stage==6 && ticks>60)throw new AssertionError("Natural hostile death did not complete");
        }catch(Throwable error){com.mojang.logging.LogUtils.getLogger().error("COMBAT_SERVER_CHECKS FAILED",error);cleanup();}
    }
    private static void cleanup(){var level=(ServerLevel)c.level();enemy.discard();c.discard();c=null;level.players().remove(owner);level.setChunkForced(0,0,false);}
}
