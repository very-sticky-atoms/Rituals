package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.Rituals;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.slf4j.Logger;

import java.util.Collections;

@EventBusSubscriber(modid = Rituals.MODID)
public class DataGenerators {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var lookupProvider = event.getLookupProvider();

        ExistingFileHelper rawHelper = event.getExistingFileHelper();

        ExistingFileHelper laxHelper = new ExistingFileHelper(Collections.emptyList(), Collections.emptySet(), false, null, null) {
            @Override
            public boolean exists(ResourceLocation loc, PackType type, String pathSuffix, String directory) {
                // 首先让原版的 helper 去查（原版知道正确的路径）
                if (rawHelper.exists(loc, type, pathSuffix, directory)) {
                    return true;
                }

                if (loc.getNamespace().equals(Rituals.MODID)) {
                    String fakePath = directory + "/" + loc.getPath() + pathSuffix;
                    LOGGER.error("[Datagen Resource Interception] Missing mod resource, automatically ignored: [{}]", fakePath);
                    return true;
                }

                return false;
            }
        };


        generator.addProvider(
                event.includeClient(),
                new RitualsItemModelProvider(generator.getPackOutput(), laxHelper)
        );


        generator.addProvider(
                event.includeServer(),
                new ModLootTableProvider(generator.getPackOutput(), lookupProvider)
        );

        generator.addProvider(
                event.includeClient(),
                new RitualsLanguageProvider(generator.getPackOutput(), "en_us")
        );
        generator.addProvider(
                event.includeClient(),
                new RitualsLanguageProvider(generator.getPackOutput(), "zh_cn")
        );
    }
}