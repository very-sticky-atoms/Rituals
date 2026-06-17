package com.ccyscnyz.rituals.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

public class RitualsShapedRecipe extends ShapedRecipe {

    // 匹配原版常用的 4 参数构造函数
    public RitualsShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result) {
        super(group, category, pattern, result);
    }

    // 匹配原版 5 参数构造函数
    public RitualsShapedRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
    }


}