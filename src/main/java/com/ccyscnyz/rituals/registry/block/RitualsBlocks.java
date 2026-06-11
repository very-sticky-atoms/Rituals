package com.ccyscnyz.rituals.registry.block;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.annotation.AutoBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Rituals.MODID);

    @AutoBlockItem(tab = "rituals_tab")
    public static final DeferredBlock<Block> RITUAL_ALTAR = BLOCKS.registerSimpleBlock(
            "ritual_altar", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
    );
}

