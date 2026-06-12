package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.block.HighOvenBlock;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.recipe.HighOvenRecipeInput;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
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

    // 0-2: 输入槽, 3: 火种槽, 4: 输出槽
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
            return slot != 4;
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot == 3) return 1;
            return super.getSlotLimit(slot);
        }
    };

    private int progress = 0;
    private int maxProgress = 100;

    // 溢出压力系统
    public final List<ItemStack> overflowItems = new ArrayList<>();
    public float pressure = 0f;
    public static final float MAX_PRESSURE = 1000f;

    // 点燃状态仅通过 currentFuel 表示：非空即已点火
    private ItemStack currentFuel = ItemStack.EMPTY;

    public HighOvenBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.HIGH_OVEN.get(), pos, state);
    }

    // ---------- 服务端每 tick ----------
    public static void serverTick(Level level, BlockPos pos, BlockState state, HighOvenBlockEntity entity) {
        ItemStack fuelSlot = entity.inventory.getStackInSlot(3);

        // 1. 如果已点火，检查 currentFuel 是否仍然有效
        if (!entity.currentFuel.isEmpty()) {
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
                // 当前火种与输入无法匹配任何配方 → 熄灭（清空火种记录）
                entity.currentFuel = ItemStack.EMPTY;
                entity.progress = 0;
                entity.setChanged();
            }
        }

        // 2. 如果未点火，尝试用火种槽物品点火
        if (entity.currentFuel.isEmpty()) {
            if (!fuelSlot.isEmpty()) {
                HighOvenRecipeInput trialInput = new HighOvenRecipeInput(
                        entity.inventory.getStackInSlot(0),
                        entity.inventory.getStackInSlot(1),
                        entity.inventory.getStackInSlot(2),
                        fuelSlot
                );
                var trialRecipe = level.getRecipeManager().getRecipeFor(
                        RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get(), trialInput, level
                );
                if (trialRecipe.isPresent()) {
                    // 消耗火种并记录
                    entity.ignite(fuelSlot.copy());
                }
            }
            // 仍无法点火，停止生产
            if (entity.currentFuel.isEmpty()) {
                entity.progress = 0;
                entity.tickPressure(level);
                entity.updateLitState(level, pos, state);
                return;
            }
        }

        // 3. 用 currentFuel 查找配方进行生产
        HighOvenRecipeInput recipeInput = new HighOvenRecipeInput(
                entity.inventory.getStackInSlot(0),
                entity.inventory.getStackInSlot(1),
                entity.inventory.getStackInSlot(2),
                entity.currentFuel
        );
        var recipeHolder = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get(), recipeInput, level
        );

        if (recipeHolder.isEmpty()) {
            entity.progress = 0;
            entity.tickPressure(level);
            entity.updateLitState(level, pos, state);
            return;
        }

        HighOvenRecipe recipe = recipeHolder.get().value();
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
                entity.insertOutputWithOverflow(result, totalProduced);
            }
            entity.progress = 0;
        }

        // 4. 压力更新
        entity.tickPressure(level);
        entity.setChanged();

        // 5. 根据进度更新 LIT 状态（工作时发光）
        entity.updateLitState(level, pos, state);

        // 6. 超压声音
        if (entity.pressure > 0 && level.getGameTime() % 20 == 0) {
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS, 0.5f, 2.0f);
        }
    }

    private void updateLitState(Level level, BlockPos pos, BlockState state) {
        boolean shouldLit = progress > 0;
        if (state.getValue(HighOvenBlock.LIT) != shouldLit) {
            level.setBlock(pos, state.setValue(HighOvenBlock.LIT, shouldLit), 3);
        }
    }

    // 点火
    private void ignite(ItemStack fuelStack) {
        if (fuelStack.isDamageableItem()) {
            if (level instanceof ServerLevel serverLevel) {
                fuelStack.hurtAndBreak(1, serverLevel, null, item -> {});
                inventory.setStackInSlot(3, fuelStack.isEmpty() ? ItemStack.EMPTY : fuelStack);
            }
        } else {
            inventory.extractItem(3, 1, false);
        }
        this.currentFuel = fuelStack.copy();
        setChanged();
    }

    // ---------- 输出与溢出 ----------
    private void insertOutputWithOverflow(ItemStack outputType, int amount) {
        ItemStack outputStack = inventory.getStackInSlot(4);
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
                inventory.setStackInSlot(4, toPut);
            } else {
                outputStack.grow(toInsert);
            }
            amount -= toInsert;
        }
        if (amount > 0) {
            addToOverflow(outputType, amount);
        }
    }

    private void addToOverflow(ItemStack item, int amount) {
        while (amount > 0) {
            int max = item.getMaxStackSize();
            int toAdd = Math.min(amount, max);
            boolean merged = false;
            for (ItemStack existing : overflowItems) {
                if (ItemStack.isSameItemSameComponents(existing, item)) {
                    int space = max - existing.getCount();
                    if (space > 0) {
                        int add = Math.min(toAdd, space);
                        existing.grow(add);
                        toAdd -= add;
                        if (toAdd <= 0) {
                            merged = true;
                            break;
                        }
                    }
                }
            }
            if (!merged || toAdd > 0) {
                ItemStack newStack = item.copy();
                newStack.setCount(toAdd);
                overflowItems.add(newStack);
                amount -= toAdd;
            } else {
                amount -= toAdd;
            }
        }
    }

    private int getTotalOverflowCount() {
        int sum = 0;
        for (ItemStack stack : overflowItems) sum += stack.getCount();
        return sum;
    }

    private void tickPressure(Level level) {
        int total = getTotalOverflowCount();
        if (total > 0) {
            pressure += total / 20.0f;
            if (pressure >= MAX_PRESSURE) {
                if (!level.isClientSide()) level.destroyBlock(worldPosition, false);
                return;
            }
        } else {
            if (pressure > 0) pressure = Math.max(0, pressure - 0.5f);
        }
    }

    // ---------- 客户端粒子 ----------
    public static void clientTick(Level level, BlockPos pos, BlockState state, HighOvenBlockEntity entity) {
        if (entity.pressure > 0 && level.isClientSide()) {
            float density = entity.pressure / MAX_PRESSURE;
            if (level.random.nextFloat() < density) {
                double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 0.7;
                double y = pos.getY() + 1.0;
                double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 0.7;
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.05, 0);
            }
        }
    }

    // ---------- 玩家取出输出 ----------
    public List<ItemStack> extractOutput(Player player) {
        List<ItemStack> extracted = new ArrayList<>();
        ItemStack output = inventory.getStackInSlot(4);
        if (!output.isEmpty()) {
            ItemStack taken = inventory.extractItem(4, output.getCount(), false);
            if (!taken.isEmpty()) extracted.add(taken);
        }
        if (output.isEmpty() && !overflowItems.isEmpty()) {
            ItemStack first = overflowItems.get(0).copy();
            int takeAmount = Math.min(first.getCount(), first.getMaxStackSize());
            first.setCount(takeAmount);
            extracted.add(first);
            overflowItems.get(0).shrink(takeAmount);
            if (overflowItems.get(0).isEmpty()) overflowItems.remove(0);
            setChanged();
        }
        return extracted;
    }

    // ---------- 持久化 ----------
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putFloat("pressure", pressure);
        if (!currentFuel.isEmpty()) {
            tag.put("currentFuel", currentFuel.save(registries));
        }

        ListTag overflowTag = new ListTag();
        for (ItemStack stack : overflowItems) {
            if (!stack.isEmpty()) {
                overflowTag.add(stack.save(registries));
            }
        }
        tag.put("overflowItems", overflowTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("progress");
        maxProgress = tag.contains("maxProgress") ? tag.getInt("maxProgress") : 100;
        pressure = tag.getFloat("pressure");
        if (tag.contains("currentFuel")) {
            currentFuel = ItemStack.parse(registries, tag.getCompound("currentFuel")).orElse(ItemStack.EMPTY);
        } else {
            currentFuel = ItemStack.EMPTY;
        }

        overflowItems.clear();
        if (tag.contains("overflowItems")) {
            ListTag list = tag.getList("overflowItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.parse(registries, list.getCompound(i)).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) overflowItems.add(stack);
            }
        }
    }

    // 在类中添加
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries); // 已有，没问题
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public float getPressure() { return pressure; }
    // isLit 不再需要，但保留也行
    public boolean isBurning() { return !currentFuel.isEmpty(); }
}