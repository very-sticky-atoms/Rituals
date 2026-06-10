package com.ccyscnyz.rituals.registry.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Function;

public class ItemManager {
    /**Registers a simple item
    * @param name The item's name.*/
    public static DeferredItem<Item> registerItem(String name) {
        return RitualsItems.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
    /**Registers an item in the "Item" class
     * @param name The item's name.
     * @param properties The item's property, like its durability.*/
    public static DeferredItem<Item> registerItem(String name, Item.Properties properties) {
        return RitualsItems.ITEMS.register(name, () -> new Item(properties));
    }
    /**Registers an item in the "Item" class
     * @param name The item's name.
     * @param propertiesGenerator The item's property.Written in the form of a lambda expression.*/
    public static DeferredItem<Item> registerItem(String name, Function<Item.Properties,Item.Properties> propertiesGenerator){
        return RitualsItems.ITEMS.register(name, () -> new Item(propertiesGenerator.apply(new Item.Properties())));
    }
    /**Registers any item
     * @param name The item's name.
     * @param itemConstructor Passes T through T::new
     * @param propertiesGenerator The item's property.Written in the form of a lambda expression.*/
    public static <T extends Item> DeferredItem<T> registerItem(String name, Function<Item.Properties,T> itemConstructor, Function<Item.Properties,Item.Properties> propertiesGenerator) {
        return RitualsItems.ITEMS.register(name, () -> itemConstructor.apply(propertiesGenerator.apply(new Item.Properties())));
    }
}
