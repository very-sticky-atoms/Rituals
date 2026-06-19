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

public class TransformIngredient implements ICustomIngredient {
    private final Ingredient base;
    private final ItemStack remnant;

    public TransformIngredient(Ingredient base, ItemStack remnant) {
        this.base = base;
        this.remnant = remnant;
    }

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public Stream<ItemStack> getItems() {
        return Arrays.stream(base.getItems());
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return RitualsIngredientTypes.TRANSFORM.get();
    }

    public ItemStack consume() {
        return remnant.copy();
    }

    public static final MapCodec<TransformIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(i -> i.base),
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