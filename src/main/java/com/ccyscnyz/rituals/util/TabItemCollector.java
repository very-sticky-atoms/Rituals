// com/ccyscnyz/rituals/util/TabItemCollector.java
package com.ccyscnyz.rituals.util;

import com.ccyscnyz.rituals.annotation.TabItem;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.minecraft.world.item.Item;

import java.lang.annotation.ElementType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Supplier;

public class TabItemCollector {
    private static final Map<String, List<Supplier<? extends Item>>> TAB_ITEMS = new HashMap<>();

    public static void collect(String modId) {
        ModFileScanData scanData = ModList.get().getModFileById(modId).getFile().getScanResult();

        // 获取所有字段上的 @TabItem 注解（注意第二个参数是 ElementType.FIELD）
        scanData.getAnnotatedBy(TabItem.class, ElementType.FIELD)
                .forEach(TabItemCollector::processAnnotation);
    }

    private static void processAnnotation(ModFileScanData.AnnotationData ad) {
        try {
            // 加载类
            Class<?> clazz = Class.forName(ad.clazz().getClassName());
            // 获取字段
            Field field = clazz.getDeclaredField(ad.memberName());

            // 确保是静态字段，且类型为 Supplier<Item>
            if (Modifier.isStatic(field.getModifiers()) && Supplier.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Supplier<? extends Item> supplier = (Supplier<? extends Item>) field.get(null);

                // 从注解数据中提取 value（即标签页注册名）
                String tabName = (String) ad.annotationData().get("value");
                if (tabName != null) {
                    TAB_ITEMS.computeIfAbsent(tabName, k -> new ArrayList<>()).add(supplier);
                }
            }
        } catch (Exception e) {
            // 可记录日志，方便排查
        }
    }

    public static List<Supplier<? extends Item>> getItemsForTab(String tabName) {
        return TAB_ITEMS.getOrDefault(tabName, Collections.emptyList());
    }

    public static void addItemToTab(String tabName, Supplier<? extends Item> itemSupplier) {
        TAB_ITEMS.computeIfAbsent(tabName, k -> new ArrayList<>()).add(itemSupplier);
    }

    public static void clear() {
        TAB_ITEMS.clear();
    }
}