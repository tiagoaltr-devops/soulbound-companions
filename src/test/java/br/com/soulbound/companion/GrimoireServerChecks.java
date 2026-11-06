package br.com.soulbound.companion;

import java.util.*;
import br.com.soulbound.Config;
import br.com.soulbound.companion.identity.*;
import br.com.soulbound.companion.target.TargetCommandService;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.item.SoulEggIdentity;
import br.com.soulbound.network.*;
import br.com.soulbound.registry.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.*;

/** Extends the isolated survival fixture with actual server/item/menu/target integration. */
public final class GrimoireServerChecks {
    private static int count,timeout;private static Mob target,other;
    private static void check(boolean b,String message){count++;if(!b)throw new AssertionError("Grimoire: "+message);}
    private static void aim(FakePlayer p,LivingEntity t){p.lookAt(EntityAnchorArgument.Anchor.EYES,t.getEyePosition());}
    public static void begin(MinecraftServer s,FakePlayer owner,LupusCompanionEntity c)throws Exception{
        count=0;var level=s.overworld();var data=SoulboundSavedData.get(s);c.showAction(0,0);c.setHealth(c.getMaxHealth());c.setPos(3,140,3);owner.setPos(2,140,3);
        var bornOwner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GrimoireBirth"));bornOwner.setPos(-3,140,-3);level.players().add(bornOwner);
        SoulboundIdentity.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(bornOwner));
        var bornRecord=data.find(bornOwner.getUUID());check(bornRecord!=null && bornRecord.canSummon(),"first login creates canonical ready bond");
        ItemStack egg=bornOwner.getInventory().getItem(0);check(egg.is(ModItems.COMPANION_EGG.get()),"first login delivers bound egg");
        var floor=new net.minecraft.core.BlockPos(-3,139,-3);level.setBlockAndUpdate(floor,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        for(int y=140;y<143;y++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(-3,y,-3),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var context=new net.minecraft.world.item.context.UseOnContext(bornOwner,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(-2.5,140,-2.5),net.minecraft.core.Direction.UP,floor,false));
        egg.getItem().useOn(context);check(data.find(bornOwner.getUUID()).companionStatus()==CompanionStatus.ACTIVE && egg.get(net.minecraft.core.component.DataComponents.ITEM_MODEL).toString().equals("soulbound:soul_grimoire"),"actual first summon produces grimoire projection");
        bornOwner.getInventory().clearContent();s.getCommands().performPrefixedCommand(bornOwner.createCommandSourceStack(),"soulbound recover");SoulboundIdentity.onLogin(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(bornOwner));
        check(bornOwner.getInventory().getItem(0).get(net.minecraft.core.component.DataComponents.ITEM_MODEL).toString().equals("soulbound:soul_grimoire") && data.find(bornOwner.getUUID()).companionUuid().equals(bornRecord.companionUuid()),"recovery restores grimoire without replacing companion UUID");
        level.getEntity(bornRecord.companionUuid()).discard();level.players().remove(bornOwner);
        var before=data.find(owner.getUUID());var stack=new ItemStack(ModItems.COMPANION_EGG.get());stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.fromNamespaceAndPath("soulbound","soul_staff"));
        SoulEggIdentity.refresh(stack,before);check(stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL).toString().equals("soulbound:soul_grimoire"),"legacy staff migrates in place");
        SoulEggIdentity.refresh(stack,before);check(stack.getCount()==1 && SoulEggIdentity.matches(stack,before),"idempotent migration keeps credentials");check(before.equals(data.find(owner.getUUID())),"migration does not mutate canonical progression");
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);check(SoulStaffCommands.information(owner,stack)!=null,"owner opens informational hub");
        var stranger=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GrimoireOther"));stranger.setInvulnerable(false);stranger.setPos(5,140,3);
        check(SoulStaffCommands.information(stranger,stack)==null,"foreign grimoire rejected");
        var menu=new br.com.soulbound.menu.SoulCommandMenu(9,owner.getInventory(),net.minecraft.world.InteractionHand.MAIN_HAND,before);check(menu.stillValid(owner),"informational hub has no proximity limit");
        c.setPos(20,140,3);var equipment=new br.com.soulbound.menu.CompanionInventoryMenu(10,owner.getInventory(),before);check(!equipment.stillValid(owner),"remote physical inventory denied");
        var previousMenu=owner.containerMenu;owner.containerMenu=menu;
        check(!menu.clickMenuButton(owner,br.com.soulbound.menu.SoulCommandMenu.INVENTORY),"forged remote inventory button denied");
        data.put(before.withState(CompanionStatus.DEAD,true,before.behaviorMode()));check(menu.stillValid(owner),"dead companion information remains readable");
        check(!menu.clickMenuButton(owner,br.com.soulbound.menu.SoulCommandMenu.FOLLOW),"dead companion rejects commands");data.put(before);owner.containerMenu=previousMenu;
        c.setPos(3,140,3);check(equipment.stillValid(owner),"nearby physical inventory allowed");
        var projection=GrimoireSnapshot.create(9,owner,before);var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),s.registryAccess());
        try{GrimoirePayload.CODEC.encode(buffer,projection);check(projection.equals(GrimoirePayload.CODEC.decode(buffer)),"localized snapshot network roundtrip");}finally{buffer.release();}
        target=EntityType.COW.create(level,EntitySpawnReason.COMMAND);target.setPos(2,140,5);target.setNoAi(true);check(level.addFreshEntity(target),"passive test target spawned");
        other=EntityType.COW.create(level,EntitySpawnReason.COMMAND);other.setPos(4,140,5);other.setNoAi(true);level.addFreshEntity(other);
        c.commandBehaviorMode(owner,BehaviorMode.STAY);check(!JuniorCombat.target(c,target),"passive excluded automatically");aim(owner,target);
        check(TargetCommandService.request(owner,target.getId()),"explicit passive target accepted");check(c.canFight() && c.getBehaviorMode()==BehaviorMode.STAY,"temporary combat overrides without changing preference");
        check(new br.com.soulbound.entity.ai.JuniorCombatGoal(c,true).canUse() && !new br.com.soulbound.entity.ai.JuniorCombatGoal(c).canUse(),"directed goal takes precedence over automatic acquisition");
        check(data.find(owner.getUUID()).behaviorMode().equals("STAY"),"canonical Stay remains unchanged");check(JuniorCombat.target(c,target) && !JuniorCombat.target(c,other),"only designated passive authorized");
        float hp=other.getHealth();check(!JuniorCombat.hit(c,other,3) && other.getHealth()==hp,"area cannot damage undesignated passive");
        check(JuniorCombat.hit(c,target,1),"designated passive takes native damage");
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.player.AttackEntityEvent> protection=e->{if(e.getTarget()==target)e.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(protection);
        try{check(!JuniorCombat.hit(c,target,1),"attack protection hook respected");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(protection);}
        boolean[] observed={false};java.util.function.Consumer<net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent> sourceCheck=e->{if(e.getEntity()==target){observed[0]=true;check(e.getSource().getEntity()==owner && e.getSource().getDirectEntity()==c,"explicit damage exposes owner and actual direct attacker");check(e.getSource().getWeaponItem()==c.getMainHandItem(),"explicit damage keeps native companion weapon");}};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(sourceCheck);
        try{check(JuniorCombat.hit(c,target,2) && observed[0],"native damage resumes after protection listener removal");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(sourceCheck);}
        check(!TargetCommandService.request(stranger,target.getId()),"foreign player without bond cannot spoof target intent");
        check(TargetCommandService.request(owner,target.getId()) && !TargetCommandService.has(c),"same-target key cancels immediately");
        check(!c.canFight() && c.getBehaviorMode()==BehaviorMode.STAY,"cancel restores Stay effect");
        TargetCommandService.cancel(owner);check(!TargetCommandService.request(owner,Integer.MAX_VALUE),"nonexistent entity ID rejected");TargetCommandService.cancel(owner);
        target.setPos(90,140,5);aim(owner,target);check(!TargetCommandService.request(owner,target.getId()),"out of range rejected");target.setPos(2,140,5);TargetCommandService.cancel(owner);
        c.commandBehaviorMode(owner,BehaviorMode.FOLLOW_PASSIVE);aim(owner,target);check(TargetCommandService.request(owner,target.getId()),"Passive allows explicit temporary order");TargetCommandService.cancel(owner);check(!c.canFight() && c.getBehaviorMode()==BehaviorMode.FOLLOW_PASSIVE,"cancel restores Passive effect");
        var pvpOwner=new net.minecraft.server.level.ServerPlayer(s,level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GrimoirePvP"),net.minecraft.server.level.ClientInformation.createDefault());
        var rule=net.minecraft.world.level.gamerules.GameRules.PVP;boolean pvp=level.getGameRules().get(rule);
        try{level.getGameRules().set(rule,false,s);check(!TargetCommandService.legal(pvpOwner,c,stranger),"dimension PvP off rejects players");level.getGameRules().set(rule,true,s);check(TargetCommandService.legal(pvpOwner,c,stranger),"legal PvP target accepted by policy");}
        finally{level.getGameRules().set(rule,pvp,s);}
        var scoreboard=s.getScoreboard();var team=scoreboard.addPlayerTeam("soulbound_grim_test");scoreboard.addPlayerToTeam(pvpOwner.getScoreboardName(),team);scoreboard.addPlayerToTeam(stranger.getScoreboardName(),team);
        try{check(!TargetCommandService.legal(pvpOwner,c,stranger),"allied player rejected");}finally{scoreboard.removePlayerTeam(team);}
        check(!TargetCommandService.legal(owner,c,owner) && !TargetCommandService.legal(owner,c,c),"owner/self rejected");
        c.commandBehaviorMode(owner,BehaviorMode.STAY);TargetCommandService.cancel(owner);timeout=Config.TARGET_TIMEOUT.get();Config.TARGET_TIMEOUT.set(40);aim(owner,target);
        check(TargetCommandService.request(owner,target.getId()),"bounded timeout order starts");
        var json=SoulboundRecord.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data.find(owner.getUUID())).getOrThrow().toString();check(!json.contains(target.getUUID().toString()),"focus not persisted into identity");
    }
    public static void finish(MinecraftServer s,FakePlayer owner,LupusCompanionEntity c){
        check(!TargetCommandService.has(c) && !c.canFight() && c.getBehaviorMode()==BehaviorMode.STAY,"real server ticks expire order and restore Stay");Config.TARGET_TIMEOUT.set(timeout);target.discard();other.discard();
        System.out.println("Grimoire native integration checks passed: "+count);
    }
}
