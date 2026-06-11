package com.ccyscnyz.rituals.compat.jei;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

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
        registration.addRecipeCategories(new HighOvenRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level != null) {
            var recipes = Minecraft.getInstance().level.getRecipeManager()
                    .getAllRecipesFor(RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get())
                    .stream()
                    .map(holder -> holder.value())
                    .toList();
            registration.addRecipes(HighOvenRecipeCategory.TYPE, recipes);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(RitualsBlocks.HIGH_OVEN.get()), HighOvenRecipeCategory.TYPE);
    }
}