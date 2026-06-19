package com.ccyscnyz.rituals.registry;


import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.entity.EarthAltarBlockEntity;
import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RitualsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rituals.MODID);


    public static final Supplier<BlockEntityType<HighOvenBlockEntity>> HIGH_OVEN =
            BLOCK_ENTITIES.register("high_oven",
                    () -> BlockEntityType.Builder.of(
                            HighOvenBlockEntity::new,
                            RitualsBlocks.HIGH_OVEN.get()
                    ).build(null)
                    // build(null) 中的 null 是给数据修复系统用的，通常传 null 即可。(AI说的)
            );

    public static final Supplier<BlockEntityType<RitualPillarBlockEntity>> RITUAL_PILLAR =
            BLOCK_ENTITIES.register("ritual_pillar",
                    () -> BlockEntityType.Builder.of(
                            RitualPillarBlockEntity::new,
                            RitualsBlocks.EARTH_RITUAL_PILLAR.get()
                    ).build(null)
            );

    public static final Supplier<BlockEntityType<EarthAltarBlockEntity>> EARTH_ALTAR =
            BLOCK_ENTITIES.register("earth_altar",
                    () -> BlockEntityType.Builder.of(
                            EarthAltarBlockEntity::new,
                            RitualsBlocks.EARTH_ALTAR.get()
                    ).build(null)
            );

}