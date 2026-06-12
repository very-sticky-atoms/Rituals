package com.ccyscnyz.rituals.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

public class EarthAltarRecipeInput implements RecipeInput {
    private final ItemStack center;
    private final List<List<ItemStack>> directionItems;

    public EarthAltarRecipeInput(ItemStack center, List<List<ItemStack>> directionItems) {
        this.center = center;
        this.directionItems = directionItems;
    }

    public ItemStack getCenter() { return center; }

    public List<ItemStack> getDirection(int index) {
        return directionItems.get(index);
    }

    @Override
    public ItemStack getItem(int index) {
        return center;
    }

    @Override
    public int size() {
        return 1;
    }
}