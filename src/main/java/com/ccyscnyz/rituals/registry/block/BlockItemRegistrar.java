package com.ccyscnyz.rituals.registry.block;

import com.ccyscnyz.rituals.annotation.AutoBlockItem;
import com.ccyscnyz.rituals.registry.item.RitualsItems;
import com.ccyscnyz.rituals.util.TabItemCollector;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.fml.ModList;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Supplier;

public class BlockItemRegistrar {
    public static void process(String modId) {
        ModFileScanData scanData = ModList.get().getModFileById(modId).getFile().getScanResult();

        // 获取所有字段上的 @AutoBlockItem 注解
        scanData.getAnnotatedBy(AutoBlockItem.class, ElementType.FIELD)
                .forEach(ad -> {
                    try {
                        Class<?> clazz = Class.forName(ad.clazz().getClassName());
                        Field field = clazz.getDeclaredField(ad.memberName());

                        if (!Modifier.isStatic(field.getModifiers())) return;
                        if (!DeferredBlock.class.isAssignableFrom(field.getType())) return;

                        field.setAccessible(true);
                        DeferredBlock<?> deferredBlock = (DeferredBlock<?>) field.get(null);

                        // 读取注解数据
                        Map<String, Object> data = ad.annotationData();
                        String tab = (String) data.getOrDefault("tab", "");

                        // 注册 BlockItem
                        String name = deferredBlock.getId().getPath(); // 方块注册名
                        Supplier<? extends Item> itemSupplier =
                                RitualsItems.ITEMS.registerSimpleBlockItem(name,deferredBlock);

                        // 如果指定了标签页，则添加到 TabItemCollector
                        if (!tab.isEmpty()) {
                            TabItemCollector.addItemToTab(tab, itemSupplier);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}