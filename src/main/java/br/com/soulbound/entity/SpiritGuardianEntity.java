package br.com.soulbound.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** Temporary trial actor. Vanilla zombie geometry is explicitly provisional. */
public final class SpiritGuardianEntity extends Zombie {
    public SpiritGuardianEntity(EntityType<? extends Zombie> type,Level level){super(type,level);targetSelector.removeAllGoals(goal->true);setCanPickUpLoot(false);}
    @Override protected boolean convertsInWater(){return false;}
    @Override protected boolean isSunSensitive(){return false;}
    @Override protected int getBaseExperienceReward(ServerLevel level){return 0;}
    @Override public boolean convertVillagerToZombieVillager(ServerLevel level,net.minecraft.world.entity.npc.villager.Villager villager){return false;}
}
