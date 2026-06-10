package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.util.TabItemCollector; // 引入收集器
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RitualsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Rituals.MODID);

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }

    public static final Supplier<CreativeModeTab> RITUALS_TAB = CREATIVE_TABS.register(
            "rituals_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.rituals"))
                    .icon(() -> new ItemStack(RitualsItems.COPPER_PICKAXE.get()))
                    .displayItems((params, output) -> {
                        // 自动添加所有标记为 "rituals_tab" 的物品
                        TabItemCollector.getItemsForTab("rituals_tab")
                                .forEach(sup -> output.accept(sup.get().getDefaultInstance()));
                    })
                    .build()
    );
}