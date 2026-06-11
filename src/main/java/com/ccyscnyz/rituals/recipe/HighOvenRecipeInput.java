package com.ccyscnyz.rituals.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record HighOvenRecipeInput(ItemStack input0, ItemStack input1, ItemStack input2, ItemStack fuel) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> input0;
            case 1 -> input1;
            case 2 -> input2;
            case 3 -> fuel;
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int size() {
        return 4;
    }
}