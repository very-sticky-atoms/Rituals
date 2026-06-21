package com.ccyscnyz.rituals.ingredient;

import com.ccyscnyz.rituals.registry.RitualsIngredientTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.stream.Stream;

public class TransformIngredient extends RitualsIngredient {
    private final ItemStack remnant;

    public TransformIngredient(Ingredient base, ItemStack remnant) {
        super(base);
        this.remnant = remnant;
    }

    @Override
    public IngredientType<?> getType() {
        return RitualsIngredientTypes.TRANSFORM.get();
    }

    @Override
    public boolean customConsumption() {
        return true;
    }

    @Override
    public ItemStack consume(ItemStack stack) {
        return remnant.copy();
    }

    public static final MapCodec<TransformIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(TransformIngredient::getBase),
                    ItemStack.CODEC.fieldOf("remnant").forGetter(i -> i.remnant)
            ).apply(instance, TransformIngredient::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, TransformIngredient> STREAM_CODEC = StreamCodec.of(
            (buf, i) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, i.base);
                ItemStack.STREAM_CODEC.encode(buf, i.remnant);
            },
            buf -> new TransformIngredient(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    ItemStack.STREAM_CODEC.decode(buf)
            )
    );
}