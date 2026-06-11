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


/**
    @AutoBlockItem(tab = "rituals_tab", maxStackSize = 64, durability = 0)  //三个参数
    public static final DeferredBlock<Block> RITUAL_ALTAR = BLOCKS.registerSimpleBlock(
            "ritual_altar", BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
    );

    @AutoBlockItem  //不传入tab参数即不让BlockItem进入CreativeTab
    public static final DeferredBlock<Block> HIDDEN_BLOCK = BLOCKS.registerSimpleBlock(
            "hidden_block", BlockBehaviour.Properties.of()
    );
 **/
}

