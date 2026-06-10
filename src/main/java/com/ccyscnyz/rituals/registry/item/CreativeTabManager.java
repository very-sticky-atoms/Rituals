package com.ccyscnyz.rituals.registry.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;

public class CreativeTabManager {
    public static DeferredHolder<CreativeModeTab,CreativeModeTab> registerCreativeTab(ItemManager.CreativeTab creativeTab) {
        Component title;
        if(creativeTab.isTranslatable) {
            title = Component.translatable(creativeTab.title);
        } else {
            title = Component.literal(creativeTab.title);
        }
        return RitualsCreativeTabs.CREATIVE_TABS.register(creativeTab.name, () -> CreativeModeTab.builder()
                .icon(() -> new ItemStack(creativeTab.icon.get()))
                .title(title)
                .displayItems(((parameters, output) -> creativeTab.items.forEach(output::accept)))
                .build());
    }
}
