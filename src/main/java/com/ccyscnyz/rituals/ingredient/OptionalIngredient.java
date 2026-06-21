package com.ccyscnyz.rituals.ingredient;

import com.ccyscnyz.rituals.registry.RitualsIngredientTypes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.Arrays;
import java.util.stream.Stream;

public class OptionalIngredient extends RitualsIngredient {
    public OptionalIngredient(Ingredient base) {
        super(base);
    }

    @Override
    public boolean test(ItemStack stack) {return stack.isEmpty() || base.test(stack);}

    @Override
    public Stream<ItemStack> getItems() {return Arrays.stream(base.getItems());}

    @Override
    public IngredientType<OptionalIngredient> getType() {return RitualsIngredientTypes.OPTIONAL.get();}

    public static final MapCodec<OptionalIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(OptionalIngredient::getBase)
            ).apply(instance, OptionalIngredient::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf,OptionalIngredient> STREAM_CODEC = StreamCodec.of(
            (buf,i) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf,i.base);
            },
            buf -> new OptionalIngredient(
                Ingredient.CONTENTS_STREAM_CODEC.decode(buf)
            )

    );
}
