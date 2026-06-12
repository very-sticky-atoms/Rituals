package com.ccyscnyz.rituals.client;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.client.renderer.HighOvenRenderer;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Rituals.MODID, value = Dist.CLIENT)
public class RitualsClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                RitualsBlockEntities.HIGH_OVEN.get(),
                HighOvenRenderer::new
        );
    }
}