package com.ccyscnyz.rituals.block;

import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HighOvenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public HighOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HighOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, RitualsBlockEntities.HIGH_OVEN.get(), HighOvenBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(HighOvenBlock::new);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HighOvenBlockEntity entity)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        Direction hitFace = hitResult.getDirection();
        Direction facing = state.getValue(FACING);

        // ---- 正面：火种槽 ----
        if (hitFace == facing) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) {
                // 空手取出火种
                ItemStack extracted = entity.inventory.extractItem(3, 1, false);
                if (!extracted.isEmpty()) {
                    player.setItemInHand(hand, extracted);
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            } else {
                // 手持物品放置火种
                ItemStack remaining = entity.inventory.insertItem(3, held.copy(), false);
                if (remaining.getCount() < held.getCount()) {
                    held.setCount(remaining.getCount());
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        // ---- 顶部：输入槽 ----
        else if (hitFace == Direction.UP) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) {
                // 空手取出整个输入物品堆栈（取最后一个非空槽）
                for (int slot : new int[]{2, 1, 0}) {  // 从后往前遍历，优先取最后一个非空槽
                    ItemStack stackInSlot = entity.inventory.getStackInSlot(slot);
                    if (!stackInSlot.isEmpty()) {
                        ItemStack extracted = entity.inventory.extractItem(slot, stackInSlot.getCount(), false);
                        if (!extracted.isEmpty()) {
                            player.setItemInHand(hand, extracted);
                            entity.setChanged();
                            return ItemInteractionResult.SUCCESS;
                        }
                    }
                }
            } else {
                // 手持物品放入输入槽（依次尝试三个槽）
                ItemStack remaining = held.copy();
                for (int slot : new int[]{0, 1, 2}) {
                    remaining = entity.inventory.insertItem(slot, remaining, false);
                    if (remaining.isEmpty()) break;
                }
                if (remaining.getCount() < held.getCount()) {
                    held.setCount(remaining.getCount());
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        // ---- 背面：输出槽 ----
        else if (hitFace == facing.getOpposite()) {
            // 取出全部输出物品
            ItemStack outputStack = entity.inventory.getStackInSlot(4);
            if (!outputStack.isEmpty()) {
                ItemStack extracted = entity.inventory.extractItem(4, outputStack.getCount(), false);
                if (!extracted.isEmpty()) {
                    // 尝试放入玩家物品栏，放不下的丢在地上(是的，连这个都得手动？！)
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HighOvenBlockEntity entity) {
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