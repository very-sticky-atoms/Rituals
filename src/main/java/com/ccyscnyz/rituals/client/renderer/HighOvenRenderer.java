package com.ccyscnyz.rituals.client.renderer;

import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HighOvenRenderer implements BlockEntityRenderer<HighOvenBlockEntity> {

    public HighOvenRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(HighOvenBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(HighOvenBlock.FACING);

        poseStack.pushPose();
        // 平移至方块中心
        poseStack.translate(0.5, 0.5, 0.5);
        // 根据朝向旋转坐标（让物品正面朝向玩家）
        float rotation = switch (facing) {
            case NORTH -> 0;
            case SOUTH -> 180;
            case WEST -> 90;
            case EAST -> -90;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // 渲染三个输入槽（槽0,1,2）
        float[][] inputX = {{-0.2f, -0.2f}, {0.2f, 0.2f}, {0f, 0f}};
        float[][] inputY = {{0f, 0f}, {0f, 0f}, {0.2f, 0.2f}};
        float[][] inputZ = {{0f, -0.3f}, {0f, -0.3f}, {0f, -0.3f}};
        for (int i = 0; i < 3; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            int itemCount = stack.getCount();
            if (!stack.isEmpty()) {
                poseStack.pushPose();
                poseStack.translate(inputX[i][0], inputY[i][0], inputZ[i][0]); // 移动到正面开口内
                poseStack.mulPose(Axis.XP.rotationDegrees(45)); // 水平放置
                poseStack.scale(0.5f, 0.5f, 0.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                        be.getLevel(), 0);
                poseStack.popPose();

                if (itemCount > 1) {
                    poseStack.pushPose();
                    poseStack.translate(inputX[i][1], inputY[i][1], inputZ[i][1]); // 移动到正面开口内
                    poseStack.mulPose(Axis.XP.rotationDegrees(45));
                    poseStack.scale(0.5f, 0.5f, 0.5f);
                    Minecraft.getInstance().getItemRenderer().renderStatic(
                            stack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                            be.getLevel(), 0);
                    poseStack.popPose();
                }
            }
        }

        // 渲染输出槽（槽3）
        ItemStack outputStack = be.inventory.getStackInSlot(4);
        if (!outputStack.isEmpty()) {
            int count = outputStack.getCount();
            int layers = Math.min(count, 2);
            for (int i = 0; i < layers; i++) {
                poseStack.pushPose();
                poseStack.translate(0f, -0.1f, 0f - i * 0.3f);
                poseStack.mulPose(Axis.XN.rotationDegrees(90));
                poseStack.scale(0.5f, 0.5f, 0.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        outputStack, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource,
                        be.getLevel(), 0);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }
}