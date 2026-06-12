package com.ccyscnyz.rituals.block.entity;

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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
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

    // 溢出物品列表，允许不同类型的物品堆积
    public final List<ItemStack> overflowItems = new ArrayList<>();
    public float pressure = 0f;
    public static final float MAX_PRESSURE = 100f; // 压力爆炸阈值

    public HighOvenBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.HIGH_OVEN.get(), pos, state);
    }

    //  服务端每 tick
    public static void serverTick(Level level, BlockPos pos, BlockState state, HighOvenBlockEntity entity) {
        ItemStack fuel = entity.inventory.getStackInSlot(3);

        if (fuel.isEmpty()) {
            entity.progress = 0;
            entity.tickPressure(level);
            return;
        }

        HighOvenRecipeInput recipeInput = new HighOvenRecipeInput(
                entity.inventory.getStackInSlot(0),
                entity.inventory.getStackInSlot(1),
                entity.inventory.getStackInSlot(2),
                fuel
        );
        var recipeHolder = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get(), recipeInput, level
        );

        if (recipeHolder.isEmpty()) {
            entity.progress = 0;
            entity.tickPressure(level);
            return;
        }

        HighOvenRecipe recipe = recipeHolder.get().value();
        ItemStack result = recipe.getResultItem();

        // 直接允许生产,直到爆炸
        entity.maxProgress = recipe.getProcessingTime();
        entity.progress++;

        if (entity.progress >= entity.maxProgress) {
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

        entity.tickPressure(level);
        entity.setChanged();
    }

    private void insertOutputWithOverflow(ItemStack outputType, int amount) {
        ItemStack outputStack = inventory.getStackInSlot(4);

        // 检查输出槽是否可以与产物合并
        boolean canMerge = !outputStack.isEmpty()
                && ItemStack.isSameItemSameComponents(outputStack, outputType);

        int space = 0;
        if (outputStack.isEmpty()) {
            // 输出槽为空，可以放满一整组
            space = outputType.getMaxStackSize();
        } else if (canMerge) {
            // 同类型物品，计算剩余空间
            space = outputType.getMaxStackSize() - outputStack.getCount();
        }
        // 类型不同：space 保持为 0，全部溢出

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

        // 剩余的（包括类型不匹配的全部）放入溢出列表
        if (amount > 0) {
            addToOverflow(outputType, amount);
        }
    }

    private void addToOverflow(ItemStack item, int amount) {
        while (amount > 0) {
            int max = item.getMaxStackSize();
            int toAdd = Math.min(amount, max);
            // 尝试合并到已有的同物品且未满的堆栈
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
        for (ItemStack stack : overflowItems) {
            sum += stack.getCount();
        }
        return sum;
    }


    private boolean isExploding = false;    //防止循环转动
    /**
     * 自然爆炸（压力达到上限）
     */
    public void explode(Level level) {
        if (level.isClientSide()) return;
        if (isExploding) return;
        isExploding = true;

        //爆炸
        level.explode(
                null,                           // 爆炸源实体（无）
                worldPosition.getX() + 0.5,     // X
                worldPosition.getY() + 0.5,     // Y
                worldPosition.getZ() + 0.5,     // Z
                3.0F,                           // 威力（TNT为4）
                true,                          // 生成火焰
                Level.ExplosionInteraction.BLOCK // 破坏方块模式
        );

        isExploding = false;
    }

    // 压力增长/消退行为
    private void tickPressure(Level level) {
        int totalOverflow = getTotalOverflowCount();
        if (totalOverflow > 0) {
            pressure += totalOverflow / 20.0f;
            if (pressure >= MAX_PRESSURE) {
                // 触发方块移除
                if (!level.isClientSide()) {
                    level.destroyBlock(worldPosition, false);
                }
                return;
            }
        } else {
            if (pressure > 0) pressure = Math.max(0, pressure - 0.5f);
        }
    }

    // 客户端粒子
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

    // ---------- 玩家取出输出（包括溢出） ----------
    public List<ItemStack> extractOutput(Player player) {
        List<ItemStack> extracted = new ArrayList<>();
        // 1. 优先取输出槽
        ItemStack output = inventory.getStackInSlot(4);
        if (!output.isEmpty()) {
            ItemStack taken = inventory.extractItem(4, output.getCount(), false);
            if (!taken.isEmpty()) extracted.add(taken);
        }
        // 2. 输出槽空时，从溢出列表取出一组
        if (output.isEmpty() && !overflowItems.isEmpty()) {
            ItemStack first = overflowItems.get(0).copy();
            int takeAmount = Math.min(first.getCount(), first.getMaxStackSize());
            first.setCount(takeAmount);
            extracted.add(first);
            overflowItems.get(0).shrink(takeAmount);
            if (overflowItems.get(0).isEmpty()) {
                overflowItems.remove(0);
            }
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

        ListTag overflowTag = new ListTag();
        for (ItemStack stack : overflowItems) {
            if (!stack.isEmpty()) {
                CompoundTag stackTag = new CompoundTag();
                stack.save(registries, stackTag);
                overflowTag.add(stackTag);
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

        overflowItems.clear();
        if (tag.contains("overflowItems")) {
            ListTag list = tag.getList("overflowItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack stack = ItemStack.parse(registries, list.getCompound(i)).orElse(ItemStack.EMPTY);
                if (!stack.isEmpty()) overflowItems.add(stack);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}