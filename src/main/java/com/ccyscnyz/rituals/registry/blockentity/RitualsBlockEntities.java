package com.ccyscnyz.rituals.registry.blockentity;


import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Rituals.MODID);

/*
    public static final Supplier<BlockEntityType<ExampleBlockEntity>> EXAMPLE_BLOCK =
            BLOCK_ENTITIES.register("example_block",
                    () -> BlockEntityType.Builder.of(
                            ExampleBlockEntity::new,
                            ModBlocks.TEST_MACHINE.get()
                    ).build(null)
                    // build(null) 中的 null 是给数据修复系统用的，通常传 null 即可。
            );
 */
}