package com.ccyscnyz.rituals.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public record EarthAltarRecipeInput(ItemStack center, List<List<ItemStack>> directionItems) implements RecipeInput {

    public List<ItemStack> getDirection(int index) {
        return directionItems.get(index);
    }

    public ItemStack getCenter() { return center; }

    @Override
    public ItemStack getItem(int index) {
        return center;
    }

    @Override
    public int size() {
        return 1;
    }
}