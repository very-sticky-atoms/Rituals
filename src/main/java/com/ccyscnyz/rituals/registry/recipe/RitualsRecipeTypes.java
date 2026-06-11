package com.ccyscnyz.rituals.registry.recipe;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RitualsRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Rituals.MODID);

    public static final Supplier<RecipeType<HighOvenRecipe>> HIGH_OVEN_RECIPE_TYPE =
            RECIPE_TYPES.register("high_oven", () -> HighOvenRecipe.Type.INSTANCE);
}