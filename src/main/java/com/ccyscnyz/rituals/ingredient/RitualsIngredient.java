package com.ccyscnyz.rituals.ingredient;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract class RitualsIngredient implements ICustomIngredient {
    protected Ingredient base;

    public RitualsIngredient(Ingredient base) {
        this.base = Objects.requireNonNull(base);
    }

    public Ingredient getBase(){
        return base;
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
        return base.isSimple();
    }

    public boolean customConsumption() {
        return false;
    }

    public ItemStack consume(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    public static ItemStack handleConsumption(Ingredient ingredient,ItemStack stack) {
        if(ingredient.isCustom()){
            if(ingredient.getCustomIngredient() instanceof CompoundIngredient(List<Ingredient> children)) {
                for (Ingredient child : children) {
                    if(child.test(stack)) {
                        return handleConsumption(child, stack);
                    }
                }
            } else if (ingredient.getCustomIngredient() instanceof RitualsIngredient ritualsIng) {
                return ritualsIng.consume(stack);
            }
        }
        return ItemStack.EMPTY;
    }
}
