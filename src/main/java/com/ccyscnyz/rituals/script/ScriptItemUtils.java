package com.ccyscnyz.rituals.script;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ScriptItemUtils {

    // 通过 "namespace:id" 获取纯净的 Item 实例
    public static ItemStack getItem(String itemId, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId.contains(":") ? itemId : "minecraft:" + itemId);
        if (rl == null) return ItemStack.EMPTY;

        Item item = BuiltInRegistries.ITEM.get(rl);
        // 如果注册表里找不到（比如拼错了），返回 EMPTY 防止崩溃
        if (item == BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getDefaultKey())) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    // 通过 "namespace:id" 动态设置任意组件
    @SuppressWarnings("unchecked")
    public static void setComponent(ItemStack stack, String componentId, Object value) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ResourceLocation.tryParse(componentId.contains(":") ? componentId : "minecraft:" + componentId);
        if (rl == null) return;

        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
        if (componentType == null) return;

        // 处理 JS 传入的快捷基础类型转换
        Object processedValue = value;
        String path = rl.getPath();

        if (value instanceof Boolean b) {
            if ("unbreakable".equals(path)) {
                processedValue = new net.minecraft.world.item.component.Unbreakable(b);
            }
        } else if (value instanceof net.minecraft.nbt.CompoundTag tag) {
            if ("custom_data".equals(path)) {
                processedValue = net.minecraft.world.item.component.CustomData.of(tag);
            }
        }

        // 物理拍入组件
        stack.set((DataComponentType<Object>) componentType, processedValue);
    }

    // 通过 "namespace:id" 动态获取任意组件
    public static Object getComponent(ItemStack stack, String componentId) {
        if (stack.isEmpty()) return null;

        ResourceLocation rl = ResourceLocation.tryParse(componentId.contains(":") ? componentId : "minecraft:" + componentId);
        if (rl == null) return null;

        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
        if (componentType == null) return null;

        return stack.get(componentType);
    }


    //小工，用于快速处理customdata
    public static void mergeCustomData(ItemStack stack, net.minecraft.nbt.CompoundTag newTag) {
        if (stack.isEmpty() || newTag == null || newTag.isEmpty()) return;

        // 利用 1.21 官方推荐的 update 机制，如果原本没有 custom_data 会自动创建空的，如果有则传入闭包
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(existingTag -> {
                    // 将新传入的 tag 的所有键值对放入现有的 tag
                    existingTag.merge(newTag);
                })
        );
    }
}