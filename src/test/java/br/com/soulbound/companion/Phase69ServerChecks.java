package br.com.soulbound.companion;

import br.com.soulbound.companion.identity.*;
import br.com.soulbound.entity.LupusCompanionEntity;
import br.com.soulbound.registry.ModEntities;
import br.com.soulbound.ritual.AltarInventory;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.*;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in fixture in the existing native test infrastructure, never packaged in the mod JAR. */
@EventBusSubscriber(modid="soulbound")
public final class Phase69ServerChecks {
    private static LupusCompanionEntity c;private static FakePlayer owner;private static int tick,checks;private static float shield;private static Mob danger;private static net.minecraft.world.phys.Vec3 start,stay;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError("Phase69 native: "+message);}
    @SubscribeEvent public static void start(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("soulbound.survivalChecks"))return;var s=event.getServer();
        if(!s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toAbsolutePath().toString().contains("phase69-server"))throw new IllegalStateException("Requires isolated phase69 world");
        if(Boolean.getBoolean("soulbound.restartChecks")){
            var snapshot=s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("phase610-snapshot.json");
            var expected=SoulboundRecord.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(snapshot))).getOrThrow();
            var actual=SoulboundSavedData.get(s).find(expected.ownerUuid());
            if(actual!=null && !actual.withRecall(expected.recall()).equals(expected)){
                var a=SoulboundRecord.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,actual).getOrThrow().getAsJsonObject();
                var b=SoulboundRecord.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,expected).getOrThrow().getAsJsonObject();
                for(var key:b.keySet())if(!java.util.Objects.equals(a.get(key),b.get(key)))System.out.println("Restart changed field: "+key);
            }
            // Shutdown/load legitimately refresh location timestamps; reserved IDs, inventory and cooldowns must not change.
            check(actual!=null && actual.withRecall(expected.recall()).equals(expected),"restart retains identity, forms, progression, inventory and cooldowns");
            check(actual.recallCooldownEnd()==expected.recallCooldownEnd() && actual.lastKnownUpdate()>=expected.lastKnownUpdate(),"restart retains Recall cooldown and refreshes location safely");
            System.out.println("Phase 6.10 restart snapshot check passed");s.halt(false);return;
        }
        Phase69Checks.nativeEquipment(s.registryAccess());var level=s.overworld();for(int x=-1;x<=0;x++)for(int z=-1;z<=0;z++)level.setChunkForced(x,z,true);
        for(int x=0;x<9;x++)for(int z=0;z<9;z++){level.setBlockAndUpdate(new BlockPos(x,139,z),Blocks.STONE.defaultBlockState());for(int y=140;y<145;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        owner=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"SurvivalTest"));owner.setPos(2,140,3);owner.setInvulnerable(true);level.players().add(owner);
        var r=SoulboundRecord.create(owner.getUUID(),"SurvivalTest").unlockJunior().unlockPleno();r=r.withForms(r.forms().unlockGod()).withState(CompanionStatus.ACTIVE,false,"FOLLOW_COMBAT");
        SoulboundSavedData.get(s).put(r);c=ModEntities.LUPUS_COMPANION.get().create(level,EntitySpawnReason.EVENT);c.setUUID(r.companionUuid());c.applyIdentity(r);c.setPos(3,140,3);c.setNoAi(true);
        check(level.addFreshEntity(c),"canonical entity admitted");check(Math.abs(c.getMaxHealth()-33.6)<.001,"Baby base 33.6");
    }
    private static void form(String form){var s=c.level().getServer();var d=SoulboundSavedData.get(s);var r=d.find(owner.getUUID());var f=r.forms();r=r.withForms(new CompanionForms(f.archetypeId(),f.unlockedForms(),form,0,f.ultimateEnd(),f.plenoUltimateEnd(),f.chargeEnd(),f.quakeEnd(),f.god()));d.put(r);c.applyIdentity(r);}
    @SubscribeEvent public static void tick(ServerTickEvent.Post event)throws Exception{
        if(c==null)return;var s=event.getServer();var level=s.overworld();var data=SoulboundSavedData.get(s);tick++;
        if(tick==1)for(var entity:level.getEntitiesOfClass(Mob.class,new net.minecraft.world.phys.AABB(-10,130,-10,15,150,15)))if(entity!=c){if(entity instanceof LupusCompanionEntity previous){var old=data.find(previous.getOwnerUuid());if(old!=null && old.ownerLastKnownName().equals("SurvivalTest"))previous.discard();}else if(entity.getType()==EntityType.ZOMBIE && entity.isNoAi())entity.discard();}
        if(tick==5){c.setHealth(10);form("JUNIOR");check(c.getHealth()==10,"Junior switch no healing");form("PLENO");check(c.getHealth()==10,"Pleno switch no healing");
            var r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);
            armor.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,armor.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).withModifierAdded(Attributes.MAX_HEALTH,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("soulbound","test_health"),20,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.CHEST));
            slots.set(1,AltarInventory.encode(level,armor));slots.set(4,AltarInventory.encode(level,new ItemStack(Items.DIAMOND_SWORD)));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);
        }
        if(tick==7){check(Math.abs(c.getMaxHealth()-53.6)<.001 && c.getHealth()==10,"health equipment adds maximum without healing");
            c.setHealth(40);var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());c.saveWithoutId(output);
            var clone=ModEntities.LUPUS_COMPANION.get().create(level,EntitySpawnReason.LOAD);clone.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            check(Math.abs(clone.getHealth()-40)<.001 && Math.abs(clone.getMaxHealth()-53.6)<.001,"NBT reload retains HP above naked maximum");c.setHealth(10);
            var r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());slots.set(1,AltarInventory.encode(level,new ItemStack(Items.DIAMOND_CHESTPLATE)));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);}
        if(tick==10){check(c.getArmorValue()>=8,"native armor attributes");check(c.getAttributeValue(Attributes.ARMOR_TOUGHNESS)>=2,"native toughness");check(c.getAttributeValue(Attributes.ATTACK_DAMAGE)>5.2,"native weapon damage");check(c.getAttributeValue(Attributes.ATTACK_SPEED)<4,"native weapon speed");check(c.getHealth()==10,"equipment must not heal");
            double damage=c.getAttributeValue(Attributes.ATTACK_DAMAGE);c.applyIdentity(data.find(owner.getUUID()));c.applyIdentity(data.find(owner.getUUID()));check(c.getAttributeValue(Attributes.ATTACK_DAMAGE)==damage,"refresh does not stack equipment attributes");
            c.setHealth(c.getMaxHealth());CompanionSurvival.tick(c);var victim=EntityType.ZOMBIE.create(level,EntitySpawnReason.NATURAL);victim.finalizeSpawn(level,level.getCurrentDifficultyAt(new BlockPos(4,140,3)),EntitySpawnReason.NATURAL,null);victim.setPos(4,140,3);victim.setHealth(3);check(level.addFreshEntity(victim),"natural enemy inserted");check(JuniorCombat.hit(c,victim,20),"native weapon hit");
        }
        if(tick==14){
            CompanionEquipment.capture(c);var menu=new br.com.soulbound.menu.CompanionInventoryMenu(1,owner.getInventory(),data.find(owner.getUUID()));owner.containerMenu=menu;owner.getInventory().setItem(9,new ItemStack(Items.COOKED_CHICKEN,5));
            menu.clicked(9,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,owner);check(AltarInventory.decode(level,data.find(owner.getUUID()).survival().slots().get(5)).getCount()==5 && owner.getInventory().getItem(9).isEmpty(),"shift-click deposits one stack");
            menu.clicked(5,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,owner);int count=0;for(int i=0;i<36;i++)if(owner.getInventory().getItem(i).is(Items.COOKED_CHICKEN))count+=owner.getInventory().getItem(i).getCount();check(count==5 && data.find(owner.getUUID()).survival().slots().get(5).isEmpty(),"shift-click withdrawal does not duplicate");
            owner.getInventory().setItem(9,new ItemStack(Items.DIRT));menu.clicked(9,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,owner);check(owner.getInventory().getItem(9).is(Items.DIRT),"incompatible item stays with player");
            var stranger=FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Unauthorized"));
            data.put(SoulboundRecord.create(stranger.getUUID(),"Unauthorized"));
            check(!menu.stillValid(stranger),"foreign player cannot access equipment");
            var before=data.find(owner.getUUID());
            menu.clicked(1,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,stranger);
            check(menu.quickMoveStack(stranger,1).isEmpty() && before.equals(data.find(owner.getUUID())),"foreign direct shift-click cannot mutate owner items");
            check(stranger.getInventory().isEmpty(),"foreign player receives no equipment");
            var external=new ArrayList<>(before.survival().slots());external.set(5,AltarInventory.encode(level,new ItemStack(Items.COOKED_CHICKEN,2)));
            data.put(before.withSurvival(before.survival().contents(external)));
            menu.clicked(5,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,owner);
            check(AltarInventory.decode(level,data.find(owner.getUUID()).survival().slots().get(5)).getCount()==2,"stale click refreshes without transferring changed stack");
            menu.clicked(5,0,net.minecraft.world.inventory.ClickType.QUICK_MOVE,owner);
            int after=0;for(int i=0;i<36;i++)if(owner.getInventory().getItem(i).is(Items.COOKED_CHICKEN))after+=owner.getInventory().getItem(i).getCount();
            check(after==7 && data.find(owner.getUUID()).survival().slots().get(5).isEmpty(),"refreshed click transfers exactly once");
            menu.removed(owner);owner.containerMenu=owner.inventoryMenu;
            c.setHealth(10);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.MUSHROOM_STEW));check(CompanionFood.manual(c,owner,net.minecraft.world.InteractionHand.MAIN_HAND) && owner.getMainHandItem().is(Items.BOWL),"food container returned to hand");
            c.setHealth(10);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.GOLDEN_APPLE));check(CompanionFood.manual(c,owner,net.minecraft.world.InteractionHand.MAIN_HAND) && c.getAbsorptionAmount()==0,"golden apple heals without special effects");
            c.setHealth(c.getMaxHealth());owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COOKED_BEEF));check(!CompanionFood.manual(c,owner,net.minecraft.world.InteractionHand.MAIN_HAND) && owner.getMainHandItem().getCount()==1,"full HP does not consume food");CompanionSurvival.tick(c);
        }
        if(tick==12){
            var r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());var weapon=AltarInventory.decode(level,slots.get(4));weapon.enchant(s.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SMITE),3);slots.set(4,AltarInventory.encode(level,weapon));var armor=AltarInventory.decode(level,slots.get(1));armor.enchant(s.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION),4);slots.set(1,AltarInventory.encode(level,armor));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);
            var target=EntityType.ZOMBIE.create(level,EntitySpawnReason.EVENT);target.setPos(4,140,4);target.setNoAi(true);level.addFreshEntity(target);check(JuniorCombat.hit(c,target,4) && target.getHealth()<16,"native offensive enchantment contributes once");
            c.setHealth(c.getMaxHealth());c.invulnerableTime=0;float full=c.getHealth();c.hurtServer(level,c.damageSources().mobAttack(target),4);float protectedDamage=full-c.getHealth();
            r=data.find(owner.getUUID());slots=new ArrayList<>(r.survival().slots());slots.set(1,AltarInventory.encode(level,new ItemStack(Items.DIAMOND_CHESTPLATE)));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);c.setHealth(c.getMaxHealth());c.invulnerableTime=0;full=c.getHealth();c.hurtServer(level,c.damageSources().mobAttack(target),4);check(full-c.getHealth()>protectedDamage,"native Protection reduces damage");target.discard();c.setHealth(c.getMaxHealth());
        }
        if(tick==35){check(data.find(owner.getUUID()).progression().xp()==8,"natural kill grants XP once");check(c.getMainHandItem().getDamageValue()>0,"weapon durability consumed");
            c.setHealth(20);owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.COOKED_BEEF,2));check(CompanionFood.manual(c,owner,net.minecraft.world.InteractionHand.MAIN_HAND),"manual feeding");check(Math.abs(c.getHealth()-32.8)<.01,"manual saturation healing");check(owner.getMainHandItem().getCount()==1,"one manual food consumed");
            var r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());slots.set(5,AltarInventory.encode(level,new ItemStack(Items.GOLDEN_CARROT,2)));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.setHealth(10);CompanionSurvival.tick(c);check(CompanionSurvival.defensive(c),"defense at low real HP");check(CompanionFood.automatic(c),"automatic food");check(Math.abs(c.getHealth()-24.4)<.01,"auto saturation healing");check(!CompanionFood.automatic(c),"food cooldown blocks duplicates");
            CompanionSurvival.tick(c);check(!CompanionSurvival.defensive(c),"recovered beyond 65 percent");var before=data.find(owner.getUUID()).survival().foodEnd();check(GodFormService.activate(owner),"God activation remains valid");check(Math.abs(c.getHealth()/c.getMaxHealth()-24.4/33.6)<.001,"God HP proportion");check(data.find(owner.getUUID()).survival().foodEnd()==before,"God keeps food cooldown");shield=data.find(owner.getUUID()).forms().god().shield();
        }
        if(tick==65){float hp=c.getHealth();c.hurtServer(level,c.damageSources().generic(),3);check(c.getHealth()==hp,"God barrier shields HP");check(data.find(owner.getUUID()).forms().god().shield()<shield,"God barrier consumed");
            c.setHealth(20);var r=data.find(owner.getUUID());data.put(r.withSurvival(new SurvivalState(r.survival().slots(),0,r.survival().foodEnd(),true,0)));c.showAction(0,1);shield=data.find(owner.getUUID()).forms().god().shield();CompanionSurvival.regenerate(c);check(c.getHealth()==22,"God natural regeneration");check(data.find(owner.getUUID()).forms().god().shield()==shield,"healing does not refill shield");
        }
        if(tick==75){c.setPos(3,140,3);start=c.position();danger=EntityType.ZOMBIE.create(level,EntitySpawnReason.EVENT);danger.setPos(4,140,3);danger.setNoAi(true);danger.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.NETHERITE_HELMET));level.addFreshEntity(danger);c.setNoAi(false);var retreat=new br.com.soulbound.entity.ai.DefensiveRetreatGoal(c);check(retreat.canUse(),"defensive path can be planned on supported ground");}
        if(tick==105){check(c.position().distanceToSqr(start)>.25,"defensive ground retreat actually moves");check(danger.getHealth()==danger.getMaxHealth() && !c.canFight(),"defense suppresses attacks: hostileHP="+danger.getHealth()+" companionHP="+c.getHealth()+" canFight="+c.canFight());check(c.commandBehaviorMode(owner,BehaviorMode.STAY),"Stay accepted while defending");stay=c.position();}
        if(tick==115){check(c.position().distanceToSqr(stay)<1,"Stay does not continue retreat");c.setNoAi(true);var r=data.find(owner.getUUID());var slots=new ArrayList<>(r.survival().slots());var armor=new ItemStack(Items.DIAMOND_CHESTPLATE);armor.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,armor.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).withModifierAdded(Attributes.MAX_HEALTH,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.Identifier.fromNamespaceAndPath("soulbound","test_health"),20,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.CHEST));slots.set(1,AltarInventory.encode(level,armor));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);}
        if(tick==119){check(Math.abs(c.getMaxHealth()-87.2)<.001,"God accepts additive HP equipment");float normalized=c.getHealth()/2;var r=data.find(owner.getUUID()).withForms(data.find(owner.getUUID()).forms().endGod());data.put(r);c.applyIdentity(r);check(Math.abs(c.getHealth()-normalized)<.001,"equipment added during God cannot heal on return");var slots=new ArrayList<>(r.survival().slots());slots.set(1,AltarInventory.encode(level,new ItemStack(Items.DIAMOND_CHESTPLATE)));r=r.withSurvival(r.survival().contents(slots));data.put(r);c.applyIdentity(r);check(Math.abs(c.getHealth()-normalized)<.001,"removing additive HP equipment cannot heal");}
        if(tick==122){var r=data.find(owner.getUUID());var recovered=r.withState(CompanionStatus.DEAD,true,"STAY").recoverAfterDeath();check(recovered.survival().slots().equals(r.survival().slots()),"resurrection retains items");check(recovered.godCooldownEnd()==r.godCooldownEnd() && recovered.forms().god().shield()==0,"resurrection retains cooldown without barrier");data.flush(s);
            System.out.println("Phase 6.9/6.10 native integration checks passed: "+checks);danger.discard();UnifiedGrimoireChecks.run(s,owner,c);GrimoireServerChecks.begin(s,owner,c);
        }
        if(tick==164){GrimoireServerChecks.finish(s,owner,c);level.players().remove(owner);
            data.flush(s);var snapshot=SoulboundRecord.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,data.find(owner.getUUID())).getOrThrow();
            java.nio.file.Files.writeString(s.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("phase610-snapshot.json"),snapshot.toString());
            c=null;s.halt(false);
        }
    }
}
