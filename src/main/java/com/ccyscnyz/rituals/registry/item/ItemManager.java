package com.ccyscnyz.rituals.registry.item;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

public class ItemManager {
    public static class CreativeTab {
        public CreativeTab(String name, DeferredItem<?> icon, Boolean isTranslatable, String title) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(icon);
            Objects.requireNonNull(isTranslatable);
            Objects.requireNonNull(title);
            this.name = name;
            this.icon = icon;
            this.isTranslatable = isTranslatable;
            this.title = title;
        }
        public String name;
        public DeferredItem<?> icon;
        public Boolean isTranslatable;
        public String title;
        public ArrayList<DeferredItem<?>> items = new ArrayList<>();
        public void addItem(DeferredItem<?> item){
            this.items.add(item);
        }
    }
    public <I extends Item> DeferredItem<I> register(String name, Function<ResourceLocation, ? extends I> func, CreativeTab creativeTab) {
        DeferredItem<I> registeredItem = RitualsItems.ITEMS.register(name,func);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public <I extends Item> DeferredItem<I> register(String name, Supplier<? extends I> sup, CreativeTab creativeTab) {
        DeferredItem<I> registeredItem = RitualsItems.ITEMS.register(name,sup);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, Item.Properties properties, CreativeTab creativeTab) {
        DeferredItem<BlockItem> registeredItem = RitualsItems.ITEMS.registerSimpleBlockItem(name,block,properties);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<BlockItem> registerSimpleBlockItem(String name, Supplier<? extends Block> block, CreativeTab creativeTab) {
        DeferredItem<BlockItem> registeredItem = RitualsItems.ITEMS.registerSimpleBlockItem(name,block);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, Item.Properties properties, CreativeTab creativeTab) {
        DeferredItem<BlockItem> registeredItem = RitualsItems.ITEMS.registerSimpleBlockItem(block,properties);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<BlockItem> registerSimpleBlockItem(Holder<Block> block, CreativeTab creativeTab) {
        DeferredItem<BlockItem> registeredItem = RitualsItems.ITEMS.registerSimpleBlockItem(block);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, Item.Properties props, CreativeTab creativeTab) {
        DeferredItem<I> registeredItem = RitualsItems.ITEMS.registerItem(name,func,props);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public <I extends Item> DeferredItem<I> registerItem(String name, Function<Item.Properties, ? extends I> func, CreativeTab creativeTab) {
        DeferredItem<I> registeredItem = RitualsItems.ITEMS.registerItem(name,func);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<Item> registerSimpleItem(String name, Item.Properties props, CreativeTab creativeTab) {
        DeferredItem<Item> registeredItem = RitualsItems.ITEMS.registerSimpleItem(name,props);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
    public DeferredItem<Item> registerSimpleItem(String name, CreativeTab creativeTab) {
        DeferredItem<Item> registeredItem = RitualsItems.ITEMS.registerSimpleItem(name);
        creativeTab.addItem(registeredItem);
        return registeredItem;
    }
}
