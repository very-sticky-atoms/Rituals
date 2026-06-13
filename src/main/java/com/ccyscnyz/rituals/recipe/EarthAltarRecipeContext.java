package com.ccyscnyz.rituals.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import oshi.annotation.concurrent.Immutable;

import java.util.List;

public record EarthAltarRecipeContext(ItemStack center, List<List<ItemStack>> directionItems, Level level, BlockPos position) implements RecipeInput{
    @Override
    public ItemStack getItem(int index) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    public EarthAltarRecipeContext copy(){
        return new EarthAltarRecipeContext(center.copy(),directionItems.stream().map(l->l.stream().map(ItemStack::copy).toList()).toList(),level,position);
    }
    public EarthAltarRecipeContext with(ItemStack center){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(List<List<ItemStack>> directionItems){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(Level level){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
    public EarthAltarRecipeContext with(BlockPos position){
        return new EarthAltarRecipeContext(center,directionItems,level,position);
    }
}