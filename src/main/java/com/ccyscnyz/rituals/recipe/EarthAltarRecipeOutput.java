package com.ccyscnyz.rituals.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.List;

/**
 * 封装配方执行后的输出物品和可选的输入修改器。
 */
public record EarthAltarRecipeOutput(
        ItemStack output,
        InputModifier inputModifier
) {
    @FunctionalInterface
    public interface InputModifier {
        void modify(List<List<ItemStack>> consumedItems,
                    List<List<BlockPos>> pillarPositions,
                    Level level, BlockPos pos);
    }
}