package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.recipe.HighOvenRecipeInput;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

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
            // 输出槽禁止放入
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
        // 火种必须存在
        ItemStack fuel = entity.inventory.getStackInSlot(3);
        if (fuel.isEmpty()) {
            entity.progress = 0;
            return;
        }

        // 收集输入槽物品（允许部分空）
        ItemStack in0 = entity.inventory.getStackInSlot(0);
        ItemStack in1 = entity.inventory.getStackInSlot(1);
        ItemStack in2 = entity.inventory.getStackInSlot(2);

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
        entity.maxProgress = recipe.getProcessingTime();
        entity.progress++;

        if (entity.progress < entity.maxProgress) {
            entity.setChanged();
            return;
        }

        // --- 进度完成，开始消耗和产出 ---

        // 1. 消耗输入槽物品（与之前相同）
        List<ItemStack> slotContents = new ArrayList<>();
        slotContents.add(in0.copy());
        slotContents.add(in1.copy());
        slotContents.add(in2.copy());

        for (Ingredient ingredient : recipe.getInputs()) {
            for (int i = 0; i < slotContents.size(); i++) {
                ItemStack slotStack = slotContents.get(i);
                if (!slotStack.isEmpty() && ingredient.test(slotStack)) {
                    slotStack.shrink(1);
                    break;
                }
            }
        }
        entity.inventory.setStackInSlot(0, slotContents.get(0));
        entity.inventory.setStackInSlot(1, slotContents.get(1));
        entity.inventory.setStackInSlot(2, slotContents.get(2));

        // 2. 消耗火种（耐久或消耗）
        ItemStack fuelStack = entity.inventory.getStackInSlot(3);
        if (fuelStack.isDamageableItem()) {
            if (level instanceof ServerLevel serverLevel) {
                fuelStack.hurtAndBreak(1, serverLevel, null, item -> {});
                entity.inventory.setStackInSlot(3, fuelStack);
            }
        } else {
            entity.inventory.extractItem(3, 1, false);
        }

        // 3. 产出产物
        float chance = recipe.getChance();
        int guaranteed = (int) chance;
        float extraProb = chance - guaranteed;

        ItemStack result = recipe.getResultItem();
        List<ItemStack> outputList = new ArrayList<>();

        // 保底产出
        for (int i = 0; i < guaranteed; i++) {
            outputList.add(result.copy());
        }
        // 额外概率产出
        if (extraProb > 0 && level.random.nextFloat() < extraProb) {
            outputList.add(result.copy());
        }

        // 背面位置，用于弹出多余物品
        BlockPos backPos = pos.relative(state.getValue(HighOvenBlock.FACING).getOpposite());
        for (ItemStack stack : outputList) {
            ItemStack remaining = entity.inventory.insertItem(4, stack, false);
            if (!remaining.isEmpty()) {
                // 弹出到世界
                Containers.dropItemStack(
                        level,
                        backPos.getX() + 0.5,
                        backPos.getY() + 0.5,
                        backPos.getZ() + 0.5,
                        remaining
                );
            }
        }

        entity.progress = 0;
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