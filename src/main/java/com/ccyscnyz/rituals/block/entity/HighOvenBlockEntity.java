package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.recipe.HighOvenRecipeInput;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HighOvenBlockEntity extends BlockEntity {

    // 槽位：0,1,2 输入；3 废弃（火种已移除）；4 输出
    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot != 4; // 输出槽禁止外部放入
        }

        @Override
        public int getSlotLimit(int slot) {
            return super.getSlotLimit(slot);
        }
    };

    private int progress = 0;
    private int maxProgress = 100;

    // 点燃状态：当前火种类型（空表示未点燃）
    private ItemStack currentFuel = ItemStack.EMPTY;

    public HighOvenBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.HIGH_OVEN.get(), pos, state);
    }

    // ---- 服务端每 tick ----
    public static void serverTick(Level level, BlockPos pos, BlockState state, HighOvenBlockEntity entity) {
        boolean doorOpen = state.getValue(HighOvenBlock.DOOR_OPEN);

        if (!level.isClientSide() && level.getGameTime() % 10 == 0) {
            level.sendBlockUpdated(pos, state, state, 2);
        }

        // 熄灭状态：当前火种为空 -> 进度归零，灯灭
        if (entity.currentFuel.isEmpty()) {
            entity.progress = 0;
            entity.updateLitState(level, pos, state, false);
            return;
        }

        // 门开启 -> 强制熄火并重置
        if (doorOpen) {
            entity.extinguish(); // 清空 currentFuel + 进度归零
            entity.updateLitState(level, pos, state, false);
            return;
        }

        // 检查当前输入 + 火种是否还能匹配配方
        HighOvenRecipeInput testInput = new HighOvenRecipeInput(
                entity.inventory.getStackInSlot(0),
                entity.inventory.getStackInSlot(1),
                entity.inventory.getStackInSlot(2),
                entity.currentFuel
        );
        var testRecipe = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get(), testInput, level
        );
        if (testRecipe.isEmpty()) {
            entity.extinguish(); // 配方失效 -> 熄火
            entity.updateLitState(level, pos, state, false);
            return;
        }

        // 正常生产
        HighOvenRecipe recipe = testRecipe.get().value();
        entity.maxProgress = recipe.getProcessingTime();
        entity.progress++;

        if (entity.progress >= entity.maxProgress) {
            // 消耗输入物品
            List<ItemStack> inputs = new ArrayList<>();
            inputs.add(entity.inventory.getStackInSlot(0).copy());
            inputs.add(entity.inventory.getStackInSlot(1).copy());
            inputs.add(entity.inventory.getStackInSlot(2).copy());
            for (Ingredient ingredient : recipe.getInputs()) {
                for (int i = 0; i < inputs.size(); i++) {
                    ItemStack stack = inputs.get(i);
                    if (!stack.isEmpty() && ingredient.test(stack)) {
                        stack.shrink(1);
                        break;
                    }
                }
            }
            entity.inventory.setStackInSlot(0, inputs.get(0));
            entity.inventory.setStackInSlot(1, inputs.get(1));
            entity.inventory.setStackInSlot(2, inputs.get(2));

            // 概率产出
            ItemStack result = recipe.getResultItem();
            float chance = recipe.getChance();
            int guaranteed = (int) chance;
            float extraProb = chance - guaranteed;
            int totalProduced = 0;
            if (guaranteed > 0) {
                totalProduced = result.getCount() * guaranteed;
            }
            if (extraProb > 0 && level.random.nextFloat() < extraProb) {
                totalProduced += result.getCount();
            }
            if (totalProduced > 0) {
                insertOutput(entity, result, totalProduced);
            }
            entity.progress = 0;
        }

        entity.updateLitState(level, pos, state, true);
        entity.setChanged();
    }


    private static void insertOutput(HighOvenBlockEntity entity, ItemStack outputType, int amount) {
        ItemStack outputStack = entity.inventory.getStackInSlot(4);
        boolean canMerge = !outputStack.isEmpty()
                && ItemStack.isSameItemSameComponents(outputStack, outputType);
        int space = 0;
        if (outputStack.isEmpty()) {
            space = outputType.getMaxStackSize();
        } else if (canMerge) {
            space = outputType.getMaxStackSize() - outputStack.getCount();
        }
        if (space > 0) {
            int toInsert = Math.min(amount, space);
            ItemStack toPut = outputType.copy();
            toPut.setCount(toInsert);
            if (outputStack.isEmpty()) {
                entity.inventory.setStackInSlot(4, toPut);
            } else {
                outputStack.grow(toInsert);
            }
        }
        // 若放不下则剩余物品直接丢弃（无溢出系统）
    }

    // 更新 LIT 状态
    private void updateLitState(Level level, BlockPos pos, BlockState state, boolean isWorking) {
        if (state.getValue(HighOvenBlock.LIT) != isWorking) {
            level.setBlock(pos, state.setValue(HighOvenBlock.LIT, isWorking), 3);
        }
    }

    // 点火逻辑（由 Block 调用，消耗玩家手中的物品）
    public void ignite(ItemStack fuelStack, ServerLevel level) {
        if (fuelStack.isDamageableItem()) {
            fuelStack.hurtAndBreak(1, level, null, item -> {});
        } else {
            fuelStack.shrink(1);
        }
        this.currentFuel = fuelStack.copy();
        setChanged();
    }

    // 熄灭（由 Block 调用，例如门被打开或配方不再匹配）
    public void extinguish() {
        this.currentFuel = ItemStack.EMPTY;
        this.progress = 0;
        setChanged();
    }

    public boolean isLit() {
        return !currentFuel.isEmpty();
    }

    // ---- 持久化 ----
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        if (!currentFuel.isEmpty()) {
            tag.put("currentFuel", currentFuel.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        maxProgress = tag.contains("maxProgress") ? tag.getInt("maxProgress") : 100;
        if (tag.contains("currentFuel")) {
            currentFuel = ItemStack.parse(registries, tag.getCompound("currentFuel")).orElse(ItemStack.EMPTY);
        } else {
            currentFuel = ItemStack.EMPTY;
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}