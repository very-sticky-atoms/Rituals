package com.ccyscnyz.rituals.client.renderer;

import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RitualPillarRenderer implements BlockEntityRenderer<RitualPillarBlockEntity> {

    public RitualPillarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(RitualPillarBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = be.inventory.getStackInSlot(0);
        if (stack.isEmpty()) return;

        // 获取游戏时间，用于旋转
        long gameTime = be.getLevel().getGameTime();
        float angle = (gameTime + partialTick) * 2.0f; // 每秒旋转 2 度

        poseStack.pushPose();
        // 移动到方块中心上方
        poseStack.translate(0.5, 1.0 + 0.05 * Math.cos(gameTime * 0.1), 0.5);
        // 绕 Y 轴旋转
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        // 缩放
        poseStack.scale(0.5f, 0.5f, 0.5f);
        // 渲染物品
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
        poseStack.popPose();
    }
}