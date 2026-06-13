package com.ccyscnyz.rituals.block;

import com.ccyscnyz.rituals.block.entity.HighOvenBlockEntity;
import com.ccyscnyz.rituals.entity.InvulnerableItemEntity;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import java.util.List;

public class HighOvenBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public HighOvenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(HighOvenBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0; // 完全不阻挡光照
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true; // 天空光向下穿透
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false; // 非完整遮光方块
    }

    // 发光
    @Override
    public int getLightEmission(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(LIT) ? 13 : 0;
    }

    // 放置
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(LIT, false);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // 方块实体
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HighOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return createTickerHelper(type, RitualsBlockEntities.HIGH_OVEN.get(),
                    HighOvenBlockEntity::clientTick);
        }
        return createTickerHelper(type, RitualsBlockEntities.HIGH_OVEN.get(),
                HighOvenBlockEntity::serverTick);
    }

    // 交互
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

        // 超压时灼伤玩家，播放蒸汽和音效
        if (entity.pressure > 0) {
            player.hurt(level.damageSources().hotFloor(), 8 * entity.pressure / 1000F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        5, 0.2, 0.1, 0.2, 0.02);
                serverLevel.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS, 0.7f, 2.0f);
            }
        }

        Direction hitFace = hitResult.getDirection();
        Direction facing = state.getValue(FACING);
        Direction leftSide = facing.getClockWise(); // 左面

        ItemStack held = player.getItemInHand(hand);

        // 正面：输入槽
        if (hitFace == facing) {
            if (held.isEmpty()) {
                // 空手取出整组输入物品
                for (int slot : new int[]{2, 1, 0}) {
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
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            } else {
                // 手持物品放入输入槽
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
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
        }

        // 左面：火种槽
        else if (hitFace == leftSide) {
            if (held.isEmpty()) {
                ItemStack extracted = entity.inventory.extractItem(3, 1, false);
                if (!extracted.isEmpty()) {
                    player.setItemInHand(hand, extracted);
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            } else {
                ItemStack remaining = entity.inventory.insertItem(3, held.copy(), false);
                if (remaining.getCount() < held.getCount()) {
                    held.setCount(remaining.getCount());
                    entity.setChanged();
                    return ItemInteractionResult.SUCCESS;
                }
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
        }

        // 背面：输出槽
        else if (hitFace == facing.getOpposite()) {
            List<ItemStack> items = entity.extractOutput(player);
            if (!items.isEmpty()) {
                for (ItemStack s : items) {
                    if (!player.getInventory().add(s)) {
                        player.drop(s, false);
                    }
                }
                entity.setChanged();
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // 破坏掉落
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HighOvenBlockEntity entity) {
                boolean isOverpressure = entity.pressure > 0;

                double dropX = pos.getX() + 0.5;
                double dropY = pos.getY() + 0.5;
                double dropZ = pos.getZ() + 0.5;
                int invulnerableDuration = 10; // 0.5秒

                // 掉落槽内物品
                for (int i = 0; i < entity.inventory.getSlots(); i++) {
                    ItemStack stack = entity.inventory.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        level.addFreshEntity(new InvulnerableItemEntity(
                                level, dropX, dropY, dropZ, stack.copy(), invulnerableDuration));
                        entity.inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                // 掉落溢出物品
                for (ItemStack stack : entity.overflowItems) {
                    level.addFreshEntity(new InvulnerableItemEntity(
                            level, dropX, dropY, dropZ, stack.copy(), invulnerableDuration));
                }
                entity.overflowItems.clear();

                // 超压时爆炸
                if (isOverpressure && level instanceof ServerLevel serverLevel) {
                    serverLevel.explode(
                            null,
                            dropX, dropY, dropZ,
                            3.0F,
                            false,
                            Level.ExplosionInteraction.BLOCK
                    );
                }
                entity.pressure = 0f;

                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}