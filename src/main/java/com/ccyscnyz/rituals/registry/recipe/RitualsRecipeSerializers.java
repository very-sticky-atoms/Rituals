package com.ccyscnyz.rituals.registry.recipe;

import com.ccyscnyz.rituals.Rituals;
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

/*
    //    注册序列化器。
    //    使用 NeoForge 提供的 SimpleSerializer，它需要提供 Codec 和 StreamCodec。
    //    第二个参数传入配方的 Codec 和 StreamCodec。
    public static final Supplier<RecipeSerializer<ExampkeBlockRecipe>> EXAMPLE_BLOCK_SERIALIZER =
            SERIALIZERS.register("example_block",
                    () -> new RecipeSerializer<>() {
                        @Override
                        public MapCodec<ExampleBlockRecipe> codec() {
                            return ExampleBlockRecipe.CODEC;
                        }

                        @Override
                        public StreamCodec<RegistryFriendlyByteBuf, ExampleBlockRecipe> streamCodec() {
                            return ExampleBlockRecipe.STREAM_CODEC;
                        }
                    });
 */
}
