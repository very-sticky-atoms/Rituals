package com.ccyscnyz.rituals.compat.jei;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class RitualsJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new HighOvenRecipeCategory(guiHelper));
        registration.addRecipeCategories(new EarthAltarRecipeCategory(guiHelper));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            var recipeManager = Minecraft.getInstance().level.getRecipeManager();

            var highOvenRecipes = recipeManager.getAllRecipesFor(
                            RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get())
                    .stream()
                    .map(holder -> holder.value())
                    .toList();
            registration.addRecipes(HighOvenRecipeCategory.TYPE, highOvenRecipes);

            var earthAltarRecipes = recipeManager.getAllRecipesFor(
                            RitualsRecipeTypes.EARTH_ALTAR_RECIPE_TYPE.get())
                    .stream()
                    .map(holder -> holder.value())
                    .toList();
            registration.addRecipes(EarthAltarRecipeCategory.TYPE, earthAltarRecipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(RitualsBlocks.HIGH_OVEN.get()), HighOvenRecipeCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(RitualsBlocks.EARTH_ALTAR.get()), EarthAltarRecipeCategory.TYPE);
    }
}