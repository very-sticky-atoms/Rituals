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

public class OptionalIngredient implements ICustomIngredient {
    private final Ingredient base;
    public OptionalIngredient(Ingredient base) {
        this.base = base;
    }

    public Ingredient getBase() {
        return base;
    }

    @Override
    public boolean test(ItemStack stack) {return stack.isEmpty() || base.test(stack);}

    @Override
    public Stream<ItemStack> getItems() {return Stream.concat(Arrays.stream(base.getItems()),Stream.of(ItemStack.EMPTY));}

    @Override
    public boolean isSimple() {return base.isSimple();}

    @Override
    public IngredientType<OptionalIngredient> getType() {return RitualsIngredientTypes.OPTIONAL.get();}

    public static final MapCodec<OptionalIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(OptionalIngredient::getBase)
            ).apply(instance, OptionalIngredient::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf,OptionalIngredient> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC, OptionalIngredient::getBase,
            OptionalIngredient::new
    );
}
