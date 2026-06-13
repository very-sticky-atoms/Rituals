package com.ccyscnyz.rituals.script;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class MutableItemStack {
    private String id;
    private int count;
    private String dataComponentsJson; // JSON 字符串，存储整个 DataComponentMap

    public MutableItemStack(String id, int count) {
        this.id = id;
        this.count = count;
        this.dataComponentsJson = "{}"; // 默认空组件
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    /** 设置整个组件的 JSON 字符串 */
    public void setDataComponentsJson(String json) {
        this.dataComponentsJson = json;
    }

    /** 获取组件的 JSON 字符串 */
    public String getDataComponentsJson() {
        return dataComponentsJson;
    }

    /**
     * 根据 DataComponentType ID 获取单个组件的 JSON 字符串（如果存在）。
     * 需要解析整个 JSON，简单起见可返回 null，鼓励脚本直接操作 JSON。
     */
    public String getDataComponent(String componentId) {
        // 简单实现：解析 JSON 并提取，若不需可省略。这里返回 null 表示用其他方式。
        return null;
    }

    public void setDataComponent(String componentId, String jsonValue) {
        // 修改内部 JSON 中对应字段，需解析并重新设置。这里提供简单实现：
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(dataComponentsJson);
            if (root.isJsonObject()) {
                root.getAsJsonObject().add(componentId, com.google.gson.JsonParser.parseString(jsonValue));
                dataComponentsJson = root.toString();
            }
        } catch (Exception ignored) {}
    }

    public boolean hasDataComponent(String componentId) {
        try {
            JsonElement root = com.google.gson.JsonParser.parseString(dataComponentsJson);
            return root.isJsonObject() && root.getAsJsonObject().has(componentId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转换为 ItemStack，使用 DataComponentMap.CODEC 解析 JSON
     */
    public ItemStack toItemStack() {
        ResourceLocation itemKey = ResourceLocation.tryParse(id);
        if (itemKey == null || !BuiltInRegistries.ITEM.containsKey(itemKey)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemKey);
        ItemStack stack = new ItemStack(item, count);
        if (!dataComponentsJson.isEmpty() && !dataComponentsJson.equals("{}")) {
            try {
                JsonElement json = com.google.gson.JsonParser.parseString(dataComponentsJson);
                DataComponentMap components = DataComponentMap.CODEC.parse(JsonOps.INSTANCE, json)
                        .getOrThrow();
                stack.applyComponents(components);
            } catch (Exception ignored) {}
        }
        return stack;
    }

    /**
     * 从 ItemStack 构建 MutableItemStack，将整个组件映射为 JSON
     */
    public static MutableItemStack fromItemStack(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        MutableItemStack mutable = new MutableItemStack(id.toString(), stack.getCount());
        DataComponentMap components = stack.getComponents();
        if (!components.isEmpty()) {
            try {
                JsonElement json = DataComponentMap.CODEC.encodeStart(JsonOps.INSTANCE, components)
                        .getOrThrow();
                mutable.dataComponentsJson = json.toString();
            } catch (Exception ignored) {}
        }
        return mutable;
    }
}