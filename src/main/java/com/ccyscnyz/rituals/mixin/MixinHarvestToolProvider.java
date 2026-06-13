package com.ccyscnyz.rituals.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import snownee.jade.addon.harvest.HarvestToolProvider;
import com.ccyscnyz.rituals.registry.item.RitualsItems;


//666Jade挖掘等级居然是硬编码的,只能mixin了
@Mixin(value = HarvestToolProvider.class, remap = false)
public class MixinHarvestToolProvider {

    @Inject(method = "getTool", at = @At("RETURN"), cancellable = true)
    private static void onGetTool(BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<ImmutableList<ItemStack>> cir) {
        ImmutableList<ItemStack> originalTools = cir.getReturnValue();

        // 检查返回列表是否有本模组工具
        boolean hasCustomTool = originalTools.stream().anyMatch(stack ->
                stack.getItem() == RitualsItems.COPPER_PICKAXE.get() ||
                        stack.getItem() == RitualsItems.STEEL_PICKAXE.get() ||
                        stack.getItem() == RitualsItems.OBSIDIAN_PICKAXE.get()
        );

        // 如果列表中有模组工具，则把原版的工具丢进垃圾桶里面
        if (hasCustomTool) {
            ImmutableList.Builder<ItemStack> cleanedTools = ImmutableList.builder();
            for (ItemStack stack : originalTools) {
                ResourceLocation registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
                // 如果这个镐子是原版命名空间 "minecraft" 的,丢进垃圾桶里面
                if ("minecraft".equals(registryName.getNamespace())) {
                    continue;
                }
                cleanedTools.add(stack);
            }
            // 强行替换返回值
            cir.setReturnValue(cleanedTools.build());
        }
    }
}