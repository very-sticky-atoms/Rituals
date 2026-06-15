package com.ccyscnyz.rituals.block;

import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HighOvenBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty DOOR_OPEN = BooleanProperty.create("door_open");

    public HighOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(DOOR_OPEN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(HighOvenBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, DOOR_OPEN);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0; // 透光
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 13 : 0;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false)
                .setValue(DOOR_OPEN, false);
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
        return createTickerHelper(type, RitualsBlockEntities.HIGH_OVEN.get(),
                HighOvenBlockEntity::serverTick);
    }

    // ---- 交互 ----
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HighOvenBlockEntity entity)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Direction hitFace = hitResult.getDirection();
        Direction facing = state.getValue(FACING);
        Direction leftSide = facing.getClockWise(); // 左面（用于点火）

        boolean doorOpen = state.getValue(DOOR_OPEN);
        boolean lit = entity.isLit();
        ItemStack held = player.getItemInHand(hand);

        // 左面：点火（仅当炉门关闭、未点燃且手持有效物品）
        if (hitFace == leftSide) {
            if (!doorOpen && !lit && !held.isEmpty()) {
                // 尝试点火
                entity.ignite(held, (ServerLevel) level);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                entity.setChanged();
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        // 正面交互
        if (hitFace == facing) {
            // 潜行空手：开关炉门
            if (player.isShiftKeyDown() && held.isEmpty()) {
                // 如果已点燃且门关闭，禁止打开
                if (!doorOpen && lit) {
                    return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
                }
                boolean newDoorState = !doorOpen;
                level.setBlock(pos, state.setValue(DOOR_OPEN, newDoorState), 3);
                level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0f, 1.0f);
                // 如果门被打开且正在燃烧，则熄灭
                if (newDoorState && lit) {
                    entity.extinguish();
                }
                return ItemInteractionResult.SUCCESS;
            }

            // 炉门打开时的物品交互
            if (doorOpen) {
                // 优先处理输出槽
                ItemStack outputStack = entity.inventory.getStackInSlot(4);
                if (!outputStack.isEmpty()) {
                    // 取出全部输出物品
                    ItemStack taken = entity.inventory.extractItem(4, outputStack.getCount(), false);
                    if (!taken.isEmpty()) {
                        if (!player.getInventory().add(taken)) {
                            player.drop(taken, false);
                        }
                        entity.setChanged();
                        return ItemInteractionResult.SUCCESS;
                    }
                } else {
                    // 输出槽为空，则处理输入槽
                    if (held.isEmpty()) {
                        // 空手：取出输入槽中最后一个非空槽的整组物品
                        for (int slot : new int[]{2, 1, 0}) {
                            ItemStack slotStack = entity.inventory.getStackInSlot(slot);
                            if (!slotStack.isEmpty()) {
                                ItemStack extracted = entity.inventory.extractItem(slot, slotStack.getCount(), false);
                                if (!extracted.isEmpty()) {
                                    player.setItemInHand(hand, extracted);
                                    entity.setChanged();
                                    return ItemInteractionResult.SUCCESS;
                                }
                            }
                        }
                    } else {
                        // 手持物品：尝试放入输入槽
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
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            // 炉门关闭时正面无交互
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        // 其他面（背面、顶面等）无交互
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 破坏掉落
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HighOvenBlockEntity entity) {
                // 直接掉落所有槽位物品，无需无敌
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