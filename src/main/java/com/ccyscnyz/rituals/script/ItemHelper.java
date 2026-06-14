package com.ccyscnyz.rituals.script;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemHelper {
    private String id;
    private int count;
    private String dataComponentJson;
    public ItemHelper(){}

    public void setId(String id) {
        this.id = id;
    }
    public void setCount(int count) {
        this.count = count;
    }
    public void setDataComponentJson(String dataComponentJson) {
        this.dataComponentJson = dataComponentJson;
    }

    public Item getItem(){
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    public DataComponentMap makeDataComponent(){
        return DataComponentMap.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(dataComponentJson)).getOrThrow();
    }

    public ItemStack makeStack(){
        ItemStack itemStack = new ItemStack(this.getItem(),count);
        itemStack.applyComponents(this.makeDataComponent());
        return itemStack;
    }
}
