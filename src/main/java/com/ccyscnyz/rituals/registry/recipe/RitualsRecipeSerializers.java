package com.ccyscnyz.rituals.registry.recipe;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipe;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RitualsRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Rituals.MODID);

    public static final Supplier<RecipeSerializer<HighOvenRecipe>> HIGH_OVEN_SERIALIZER =
            SERIALIZERS.register("high_oven", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<HighOvenRecipe> codec() {
                    return HighOvenRecipe.CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, HighOvenRecipe> streamCodec() {
                    return HighOvenRecipe.STREAM_CODEC;
                }
            });

    public static final Supplier<RecipeSerializer<EarthAltarRecipe>> EARTH_ALTAR_SERIALIZER =
            SERIALIZERS.register("earth_altar", () -> new RecipeSerializer<>() {
                @Override
                public MapCodec<EarthAltarRecipe> codec() { return EarthAltarRecipe.CODEC; }
                @Override
                public StreamCodec<RegistryFriendlyByteBuf, EarthAltarRecipe> streamCodec() {
                    return EarthAltarRecipe.STREAM_CODEC;
                }
            });


}