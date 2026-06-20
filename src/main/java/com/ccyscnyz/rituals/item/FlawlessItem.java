package com.ccyscnyz.rituals.item;

import com.ccyscnyz.rituals.registry.RitualsDataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class FlawlessItem extends Item {
    public FlawlessItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(RitualsDataComponents.FLAWLESSNESS.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        RitualsDataComponents.FlawlessnessInfo info = stack.get(RitualsDataComponents.FLAWLESSNESS.get());
        return info != null ? Math.round(info.getRatio() * 13.0F) : 0;
    }

    @Override
    public int getBarColor(ItemStack stack) {
        RitualsDataComponents.FlawlessnessInfo info = stack.get(RitualsDataComponents.FLAWLESSNESS.get());
        if (info == null) return 0xFFFFFF;
        return Mth.hsvToRgb(info.getRatio() / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // 获取组件实例
        var info = stack.get(RitualsDataComponents.FLAWLESSNESS.get());

        if (info != null) {

            int percentage = Math.round(info.getRatio() * 100);

            tooltip.add(Component.translatable("tooltip.rituals.flawlessness", percentage));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}