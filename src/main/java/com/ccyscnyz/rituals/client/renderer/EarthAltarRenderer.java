package com.ccyscnyz.rituals.client.renderer;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.entity.EarthAltarBlockEntity;
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
import org.checkerframework.checker.units.qual.Angle;

@OnlyIn(Dist.CLIENT)
public class EarthAltarRenderer implements BlockEntityRenderer<EarthAltarBlockEntity> {

    public EarthAltarRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(EarthAltarBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = be.inventory.getStackInSlot(0);
        if (stack.isEmpty()) return;

        // 根据进度调整旋转速度
        int progress = be.getCraftProgress();
        int maxProgress = be.getMaxCraftTime();
        float speed = 2.0f;
        if (maxProgress > 0) {
            speed = 2.0f + 0.2f * (float) Math.pow((float) progress / maxProgress , 2) ;
        }

        long gameTime = be.getLevel().getGameTime();
        float angle = gameTime * speed;

        poseStack.pushPose();
        // 移动到方块中心上方
        poseStack.translate(0.5, 1.1 + 0.1 * Math.sin(gameTime * (speed * 0.02 + 0.06)), 0.5);
        // 绕 Y 轴旋转
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        // 缩放
        poseStack.scale(0.6f, 0.6f, 0.6f);
        // 渲染物品
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);
        poseStack.popPose();
    }
}