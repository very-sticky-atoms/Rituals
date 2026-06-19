package com.ccyscnyz.rituals.registry;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.ingredient.DamageIngredient;
import com.ccyscnyz.rituals.ingredient.TransformIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class RitualsIngredientTypes {

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, Rituals.MODID);

    public static final Supplier<IngredientType<DamageIngredient>> DAMAGE =
            INGREDIENT_TYPES.register("damage", () -> new IngredientType<>(DamageIngredient.CODEC, DamageIngredient.STREAM_CODEC));

    public static final Supplier<IngredientType<TransformIngredient>> TRANSFORM =
            INGREDIENT_TYPES.register("transform", () -> new IngredientType<>(TransformIngredient.CODEC, TransformIngredient.STREAM_CODEC));

    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        INGREDIENT_TYPES.register(modEventBus);
    }
}