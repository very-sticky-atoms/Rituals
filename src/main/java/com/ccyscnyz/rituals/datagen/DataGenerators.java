package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.Rituals;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = Rituals.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var lookupProvider = event.getLookupProvider();
        var existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(
                event.includeClient(),
                new RitualsItemModelProvider(generator.getPackOutput(), existingFileHelper)
        );

        generator.addProvider(
                event.includeServer(),
                new ModLootTableProvider(generator.getPackOutput(), lookupProvider)
        );

    }
}