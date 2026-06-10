package com.ccyscnyz.rituals.registry.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Function;

public class ItemManager {
    public static DeferredItem<Item> registerItem(String name) {
        return RitualsItems.ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
    public static DeferredItem<Item> registerItem(String name, Item.Properties properties) {
        return RitualsItems.ITEMS.register(name, () -> new Item(properties));
    }
    public static DeferredItem<Item> registerItem(String name, Function<Item.Properties,Item.Properties> propertiesGenerator){
        return RitualsItems.ITEMS.register(name, () -> new Item(propertiesGenerator.apply(new Item.Properties())));
    }
    public static <T extends Item> DeferredItem<T> registerItem(String name, Function<Item.Properties,T> itemConstructor, Function<Item.Properties,Item.Properties> propertiesGenerator) {
        return RitualsItems.ITEMS.register(name, () -> itemConstructor.apply(propertiesGenerator.apply(new Item.Properties())));
    }
}
