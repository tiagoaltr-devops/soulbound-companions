package br.com.soulbound.companion;

import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

/** One target policy for melee and area damage. Vanilla hurt events/protection are never bypassed. */
public final class JuniorCombat {
    private JuniorCombat() {}
    public static int ultimateState(String form,BehaviorMode mode,boolean available,boolean busy,long end,long now){
        if(!form.equals("JUNIOR") && !form.equals("PLENO") && !form.equals("GOD"))return 1;if(mode==BehaviorMode.STAY)return 2;if(mode!=BehaviorMode.FOLLOW_COMBAT)return 3;
        if(!available)return 4;if(busy)return 5;if(now<end)return 6;return 0;
    }
    public static boolean enabled(String form,BehaviorMode mode,boolean busy){return CompanionArchetype.form(form).combat().offensive() && mode==BehaviorMode.FOLLOW_COMBAT && !busy;}
    public static boolean targetAllowed(boolean hostile,boolean player,boolean ally,boolean protectedMob,boolean alive,boolean invulnerable){return hostile && !player && !ally && !protectedMob && alive && !invulnerable;}
    public static boolean target(LupusCompanionEntity c,LivingEntity target){
        return targetAllowed(target instanceof Mob && target instanceof Enemy,target instanceof Player,
            c.isAlliedTo(target) || c.getOwnerPlayer()!=null && c.getOwnerPlayer().isAlliedTo(target),
            target instanceof LupusCompanionEntity || target instanceof br.com.soulbound.entity.SpiritGuardianEntity
                || target instanceof net.minecraft.world.entity.TamableAnimal tame && tame.isTame(),target.isAlive(),target.isInvulnerable());
    }
    public static boolean hit(LupusCompanionEntity c,LivingEntity target,float damage){
        if(!(c.level() instanceof ServerLevel level) || !c.canFight() || !target(c,target))return false;
        var weapon=c.getMainHandItem();var source=weapon.getDamageSource(c,()->c.damageSources().mobAttack(c));
        damage=net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDamage(level,weapon,target,source,damage);
        damage+=weapon.getItem().getAttackDamageBonus(target,damage,source);
        float before=target.getHealth()+target.getAbsorptionAmount();var velocity=target.getDeltaMovement();boolean hit=target.hurtServer(level,source,damage);
        if(hit){c.equipmentKnockback(target,source,velocity);weapon.hurtEnemy(target,c);net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(level,target,source);weapon.postHurtEnemy(target,c);CompanionEquipment.capture(c);}
        if(hit && target.getHealth()+target.getAbsorptionAmount()<before){c.markCombat();br.com.soulbound.companion.progression.ProgressionService.participation(c,target);}return hit;
    }
}
