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

public class DamageIngredient implements ICustomIngredient {
    private final Ingredient base;
    private final int damage;

    public DamageIngredient(Ingredient base, int damage) {
        this.base = base;
        this.damage = damage;
    }

    @Override
    public boolean test(ItemStack stack) {
        return base.test(stack) && stack.isDamageableItem();
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
        return RitualsIngredientTypes.DAMAGE.get();
    }

    public ItemStack consume(ItemStack stack) {
        ItemStack result = stack.copy();
        int currentDamage = result.getDamageValue();
        int newDamage = currentDamage + damage;

        if (newDamage >= result.getMaxDamage()) {
            return ItemStack.EMPTY;
        }

        result.setDamageValue(newDamage);
        return result;
    }

    public static final MapCodec<DamageIngredient> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("ingredient").forGetter(i -> i.base),
                    net.minecraft.util.ExtraCodecs.POSITIVE_INT.fieldOf("amount").forGetter(i -> i.damage)
            ).apply(instance, DamageIngredient::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageIngredient> STREAM_CODEC = StreamCodec.of(
            (buf, i) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, i.base);
                buf.writeVarInt(i.damage);
            },
            buf -> new DamageIngredient(
                    Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                    buf.readVarInt()
            )
    );
}