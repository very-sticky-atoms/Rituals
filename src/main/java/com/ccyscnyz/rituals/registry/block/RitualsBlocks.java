package com.ccyscnyz.rituals.registry.block;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.annotation.AutoBlockItem;
import com.ccyscnyz.rituals.block.EarthAltarBlock;
import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.block.RitualPillarBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Rituals.MODID);

    @AutoBlockItem(tab = "rituals_tab")
    public static final DeferredBlock<Block> HIGH_OVEN = BLOCKS.register("high_oven",
            () -> new HighOvenBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
            )
    );

    @AutoBlockItem(tab = "rituals_tab")
    public static final DeferredBlock<Block> RITUAL_PILLAR = BLOCKS.register("ritual_pillar",
            () -> new RitualPillarBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );

    @AutoBlockItem(tab = "rituals_tab")
    public static final DeferredBlock<Block> EARTH_ALTAR = BLOCKS.register("earth_altar",
            () -> new EarthAltarBlock(BlockBehaviour.Properties.of()
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
            )
    );
}

