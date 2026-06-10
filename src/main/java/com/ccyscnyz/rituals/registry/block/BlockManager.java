package com.ccyscnyz.rituals.registry.block;

import com.ccyscnyz.rituals.registry.item.RitualsItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Function;

public class BlockManager {
    /** Register Block and BlockItem at the same time
     *  @param blockBase The Block to modify.
     *  @param blockPropertiesModifier The modifier of the block's properties
     *  @param itemPropertiesGenerator The BlockItem's properties.Use lambda.*/
    public static DeferredBlock<Block> registerBlockAndItem(String name,Block blockBase, Function<BlockBehaviour.Properties,BlockBehaviour.Properties> blockPropertiesModifier, Function<Item.Properties,Item.Properties> itemPropertiesGenerator) {
        DeferredBlock<Block> registeredBlock = RitualsBlocks.BLOCKS.registerBlock(name, p->new Block(blockPropertiesModifier.apply(BlockBehaviour.Properties.ofFullCopy(blockBase))));
        RitualsItems.ITEMS.register(name,p->new BlockItem(registeredBlock.get(),itemPropertiesGenerator.apply(new Item.Properties())));
        return registeredBlock;
    }
    /** Register Block and BlockItem at the same time
     *  @param blockPropertiesGenerator The Block's properties.Use lambda.
     *  @param itemPropertiesGenerator The BlockItem's properties.Use lambda.*/
    public static DeferredBlock<Block> registerBlockAndItem(String name, Function<BlockBehaviour.Properties,BlockBehaviour.Properties> blockPropertiesGenerator, Function<Item.Properties,Item.Properties> itemPropertiesGenerator) {
        DeferredBlock<Block> registeredBlock = RitualsBlocks.BLOCKS.registerBlock(name, p->new Block(blockPropertiesGenerator.apply(BlockBehaviour.Properties.of())));
        RitualsItems.ITEMS.register(name,p->new BlockItem(registeredBlock.get(),itemPropertiesGenerator.apply(new Item.Properties())));
        return registeredBlock;
    }
    /** Register Block and BlockItem at the same time
     *  @param block The block to register.
     *  @param itemPropertiesGenerator The BlockItem's properties.Use lambda.*/
    public static <T extends Block> DeferredBlock<T> registerBlockAndItem(String name, T block, Function<Item.Properties,Item.Properties> itemPropertiesGenerator) {
        DeferredBlock<T> registeredBlock = RitualsBlocks.BLOCKS.register(name,()->block);
        RitualsItems.ITEMS.register(name,p->new BlockItem(registeredBlock.get(),itemPropertiesGenerator.apply(new Item.Properties())));
        return  registeredBlock;
    }
}
