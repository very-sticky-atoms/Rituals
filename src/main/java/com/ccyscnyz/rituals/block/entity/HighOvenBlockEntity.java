package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.recipe.HighOvenRecipeInput;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class HighOvenBlockEntity extends BlockEntity {

    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // 输出槽禁止手动放入
            return slot != 4;
        }

        @Override
        public int getSlotLimit(int slot) {
            // 火种槽只能存一个物品
            if (slot == 3) return 1;
            return super.getSlotLimit(slot);
        }
    };

    private int progress = 0;
    private int maxProgress = 100;

    public HighOvenBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.HIGH_OVEN.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HighOvenBlockEntity entity) {
        ItemStack in0 = entity.inventory.getStackInSlot(0);
        ItemStack in1 = entity.inventory.getStackInSlot(1);
        ItemStack in2 = entity.inventory.getStackInSlot(2);
        ItemStack fuel = entity.inventory.getStackInSlot(3);

        // 必须有全部输入和火种才能工作
        if (in0.isEmpty() || in1.isEmpty() || in2.isEmpty() || fuel.isEmpty()) {
            entity.progress = 0;
            return;
        }

        HighOvenRecipeInput recipeInput = new HighOvenRecipeInput(in0, in1, in2, fuel);
        var recipeHolder = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get(),
                recipeInput,
                level
        );

        if (recipeHolder.isEmpty()) {
            entity.progress = 0;
            return;
        }

        HighOvenRecipe recipe = recipeHolder.get().value();
        ItemStack result = recipe.getResultItem();
        ItemStack outputStack = entity.inventory.getStackInSlot(4);

        // 检查输出槽空间
        if (!outputStack.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(outputStack, result)
                    || outputStack.getCount() + result.getCount() > outputStack.getMaxStackSize()) {
                entity.progress = 0;
                return;
            }
        }

        entity.maxProgress = recipe.getProcessingTime();
        entity.progress++;

        if (entity.progress >= entity.maxProgress) {
            // 消耗输入
            entity.inventory.extractItem(0, 1, false);
            entity.inventory.extractItem(1, 1, false);
            entity.inventory.extractItem(2, 1, false);
            // 消耗火种
            ItemStack fuelStack = entity.inventory.getStackInSlot(3);
            if (fuelStack.isDamageableItem()) {
                if (level instanceof ServerLevel serverLevel) {
                    fuelStack.hurtAndBreak(1, serverLevel, null, item -> {});
                    entity.inventory.setStackInSlot(3, fuelStack);
                }
            } else {
                entity.inventory.extractItem(3, 1, false);
            }

            // 概率产出
            if (level.random.nextFloat() < recipe.getChance()) {
                if (outputStack.isEmpty()) {
                    entity.inventory.setStackInSlot(4, result.copy());
                } else {
                    outputStack.grow(result.getCount());
                    entity.inventory.setStackInSlot(4, outputStack);
                }
            }
            entity.progress = 0;
        }

        entity.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        maxProgress = tag.contains("maxProgress") ? tag.getInt("maxProgress") : 100;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}