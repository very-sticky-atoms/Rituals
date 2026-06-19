package com.ccyscnyz.rituals.script;

import com.ccyscnyz.rituals.Rituals;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.graalvm.polyglot.Value;

import java.util.*;

public class ScriptItemUtils {

    private static final Gson GSON = new Gson();

    public static ItemStack getItem(String itemId, int count) {
        ResourceLocation rl = ResourceLocation.tryParse(itemId.contains(":") ? itemId : "minecraft:" + itemId);
        if (rl == null) return ItemStack.EMPTY;
        Item item = BuiltInRegistries.ITEM.get(rl);
        if (item == BuiltInRegistries.ITEM.get(BuiltInRegistries.ITEM.getDefaultKey())) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }


    @SuppressWarnings("unchecked")
    public static void setComponent(ItemStack stack, String componentId, Object rawValue, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return;

        ResourceLocation rl = ResourceLocation.tryParse(componentId.contains(":") ? componentId : "minecraft:" + componentId);
        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
        if (componentType == null) return;

        Object javaValue = toJavaObject(rawValue);
        String json = (javaValue instanceof String s) ? s : GSON.toJson(javaValue);

        // 尝试一：先尝试将其作为“文本组件”解析
        // 只有 custom_name, lore 等 Component 类组件会成功，其他非文本组件会抛出异常
        try {
            Object component = Component.Serializer.fromJsonLenient(json, registries);
            // 如果这里没报错，说明它是 Component 类型，直接设置
            stack.set((DataComponentType<Object>) componentType, component);
            return;
        } catch (Exception ignored) {
            // 解析失败，说明不是文本组件，进入尝试二
        }

        // 尝试二：使用组件自带的 Codec 进行通用解析
        try {
            JsonElement jsonElement = JsonParser.parseString(json);
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            Object componentValue = componentType.codecOrThrow()
                    .parse(ops, jsonElement)
                    .getOrThrow();

            stack.set((DataComponentType<Object>) componentType, componentValue);
        } catch (Exception e) {
            Rituals.LOGGER.error("Failed to set component '{}' with JSON '{}': {}", componentId, json, e.getMessage());
        }
    }

    // 递归将 GraalJS Value 或 Java 对象转换为普通 Java 对象（Map、List、String、Number、Boolean）。
    private static Object toJavaObject(Object value) {
        if (value instanceof Value val) {
            if (val.isNull()) return null;
            if (val.isString()) return val.asString();
            if (val.isBoolean()) return val.asBoolean();
            if (val.isNumber()) return val.as(Number.class);
            if (val.hasMembers()) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (String key : val.getMemberKeys()) {
                    map.put(key, toJavaObject(val.getMember(key)));
                }
                return map;
            }
            if (val.hasArrayElements()) {
                List<Object> list = new ArrayList<>();
                for (int i = 0; i < val.getArraySize(); i++) {
                    list.add(toJavaObject(val.getArrayElement(i)));
                }
                return list;
            }
            return val.toString();
        } else if (value instanceof Map) {
            Map<String, Object> map = new LinkedHashMap<>();
            ((Map<?, ?>) value).forEach((k, v) -> map.put(String.valueOf(k), toJavaObject(v)));
            return map;
        } else if (value instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object o : (List<?>) value) list.add(toJavaObject(o));
            return list;
        }
        return value;
    }

    public static Object getComponent(ItemStack stack, String componentId) {
        if (stack.isEmpty()) return null;
        ResourceLocation rl = ResourceLocation.tryParse(componentId.contains(":") ? componentId : "minecraft:" + componentId);
        if (rl == null) return null;
        DataComponentType<?> componentType = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
        if (componentType == null) return null;
        return stack.get(componentType);
    }

    public static void removeComponent(ItemStack stack, String componentId) {
        if (stack.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(componentId.contains(":") ? componentId : "minecraft:" + componentId);
        if (rl == null) return;
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(rl);
        if (type != null) {
            stack.remove(type);
        }
    }

    public static void mergeCustomData(ItemStack stack, net.minecraft.nbt.CompoundTag newTag) {
        if (stack.isEmpty() || newTag == null || newTag.isEmpty()) return;
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(existingTag -> existingTag.merge(newTag))
        );
    }

    public static void addCustomData(ItemStack stack, String key, Object value) {
        if (stack.isEmpty() || key == null || value == null) return;
        stack.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY,
                customData -> customData.update(existingTag -> {
                    if (value instanceof String s) {
                        existingTag.putString(key, s);
                    } else if (value instanceof Number num) {
                        if (num instanceof Integer || num instanceof Long || num instanceof Short || num instanceof Byte) {
                            existingTag.putInt(key, num.intValue());
                        } else {
                            existingTag.putDouble(key, num.doubleValue());
                        }
                    } else if (value instanceof Boolean b) {
                        existingTag.putBoolean(key, b);
                    } else if (value instanceof Character c) {
                        existingTag.putString(key, c.toString());
                    }
                    else if (value instanceof net.minecraft.nbt.Tag tag) {
                        existingTag.put(key, tag);
                    }
                })
        );
    }

    public static void removeCustomData(ItemStack stack, String key) {
        if (stack.isEmpty() || key == null) return;
        var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (custom == null) return;
        var tag = custom.copyTag();
        tag.remove(key);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag.isEmpty() ? new net.minecraft.nbt.CompoundTag() : tag));
    }
}