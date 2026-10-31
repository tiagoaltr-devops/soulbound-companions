package br.com.soulbound.registry;

import br.com.soulbound.SoulboundCompanions;
import br.com.soulbound.entity.LupusCompanionEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(SoulboundCompanions.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<LupusCompanionEntity>> LUPUS_COMPANION =
            ENTITIES.registerEntityType("lupus_companion", LupusCompanionEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.45F, 0.9F).clientTrackingRange(10));

    private ModEntities() {}
    public static final DeferredHolder<EntityType<?>, EntityType<br.com.soulbound.entity.SpiritGuardianEntity>> SPIRIT_GUARDIAN =
            ENTITIES.registerEntityType("spirit_guardian", br.com.soulbound.entity.SpiritGuardianEntity::new, MobCategory.MONSTER,
                    builder -> builder.sized(.6F,1.95F).clientTrackingRange(10));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LUPUS_COMPANION.get(), LupusCompanionEntity.createAttributes().build());
        event.put(SPIRIT_GUARDIAN.get(),net.minecraft.world.entity.monster.zombie.Zombie.createAttributes().build());
    }
}
