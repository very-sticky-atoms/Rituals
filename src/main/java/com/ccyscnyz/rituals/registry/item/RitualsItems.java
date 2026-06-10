package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.Rituals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Rituals.MODID);
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
