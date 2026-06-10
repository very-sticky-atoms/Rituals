package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Rituals.MODID);
    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
