package com.ccyscnyz.rituals.registry.block;

import com.ccyscnyz.rituals.Rituals;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class RitualsBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Rituals.MODID);
    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
