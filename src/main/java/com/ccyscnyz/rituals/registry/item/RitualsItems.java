package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.tags.RitualsTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class RitualsItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Rituals.MODID);

    public static final DeferredItem<Item> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(RitualsTiers.COPPER, new Item.Properties()));
    public static final DeferredItem<Item> STEEL_PICKAXE = ITEMS.register("steel_pickaxe",
            () -> new PickaxeItem(RitualsTiers.STEEL, new Item.Properties()));
    public static final DeferredItem<Item> OBSIDIAN_PICKAXE = ITEMS.register("obsidian_pickaxe",
            () -> new PickaxeItem(RitualsTiers.OBSIDIAN, new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
