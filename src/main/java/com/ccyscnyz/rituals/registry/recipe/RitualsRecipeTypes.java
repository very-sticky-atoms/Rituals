package com.ccyscnyz.rituals.registry.recipe;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RitualsRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Rituals.MODID);

/*
    public static final Supplier<RecipeType<ExampleBlockRecipe>> EXAMPLE_BLOCK_RECIPE_TYPE =
            RECIPE_TYPES.register("example_block", () -> ExampleBlockRecipe.Type.INSTANCE);
 */
}
