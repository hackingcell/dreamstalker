package com.playze.dreamstalker;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DreamStalker.MODID);

    public static final RegistryObject<EntityType<Watcher>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.of(Watcher::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.95f)
                    .clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, DreamStalker.id("watcher"))));

    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(WATCHER.get(), Monster.createMonsterAttributes().build());
    }
}
