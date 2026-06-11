package com.ccyscnyz.rituals;

import com.ccyscnyz.rituals.registry.block.BlockItemRegistrar;
import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.item.RitualsCreativeTabs;
import com.ccyscnyz.rituals.registry.item.RitualsItems;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeSerializers;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import com.ccyscnyz.rituals.util.TabItemCollector;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Rituals.MODID)
public class Rituals {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "rituals";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public Rituals(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        TabItemCollector.collect(MODID);

        RitualsCreativeTabs.CREATIVE_TABS.register(modEventBus);
        RitualsItems.ITEMS.register(modEventBus);
        RitualsBlocks.BLOCKS.register(modEventBus);

        BlockItemRegistrar.process(MODID);

        RitualsBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        RitualsRecipeTypes.RECIPE_TYPES.register(modEventBus);
        RitualsRecipeSerializers.SERIALIZERS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
        
        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    @SubscribeEvent
    public void onServerStarting(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("rituals_debug")
                        .executes(ctx -> {
                            var recipes = ctx.getSource().getServer().getRecipeManager()
                                    .getAllRecipesFor(RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get());
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("HighOven recipes loaded: " + recipes.size()), false);
                            return 1;
                        })
        );
    }
}
