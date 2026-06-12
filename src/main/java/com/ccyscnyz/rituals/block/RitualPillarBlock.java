package com.ccyscnyz.rituals.block;

import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class RitualPillarBlock extends Block implements EntityBlock {

    // 定义柱子的碰撞形状（中心0.5x0.5的柱体）

    protected static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 12, 10);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return SHAPE;
    }

    public RitualPillarBlock(Properties properties) {
        super(properties.noOcclusion()); // 不遮挡光线
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RitualPillarBlockEntity(pos, state);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RitualPillarBlockEntity entity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            ItemStack extracted = entity.inventory.extractItem(0, entity.inventory.getStackInSlot(0).getCount(), false);
            if (!extracted.isEmpty()) {
                player.setItemInHand(hand, extracted);
                entity.setChanged();
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        } else {
            ItemStack remaining = entity.inventory.insertItem(0, held.copy(), false);
            if (remaining.getCount() < held.getCount()) {
                held.setCount(remaining.getCount());
                entity.setChanged();
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RitualPillarBlockEntity entity) {
                for (int i = 0; i < entity.inventory.getSlots(); i++) {
                    ItemStack stack = entity.inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}