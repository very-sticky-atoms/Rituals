package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.recipe.EarthAltarRecipe;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipeInput;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class EarthAltarBlockEntity extends BlockEntity {

    public final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }

        @Override
        public int getSlotLimit(int slot) { return 1; }
    };

    private int craftProgress = 0;
    private int maxCraftTime = 100;

    public EarthAltarBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.EARTH_ALTAR.get(), pos, state);
    }

    // 八个方向偏移（依次北、东北、东、东南、南、西南、西、西北）
    private static final BlockPos[] DIRECTION_OFFSETS = {
            new BlockPos(0, 0, -1),
            new BlockPos(1, 0, -1),
            new BlockPos(1, 0, 0),
            new BlockPos(1, 0, 1),
            new BlockPos(0, 0, 1),
            new BlockPos(-1, 0, 1),
            new BlockPos(-1, 0, 0),
            new BlockPos(-1, 0, -1),
    };


    //检测仪式柱
    private Map<Integer, List<BlockPos>> detectPillars() {
        Map<Integer, List<PillarEntry>> entries = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            entries.put(i, new ArrayList<>());
        }

        if (level == null) return new HashMap<>();

        int radius = 5;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos checkPos = worldPosition.offset(dx, 0, dz);
                if (level.getBlockEntity(checkPos) instanceof RitualPillarBlockEntity) {
                    int dirIndex = getDirectionIndex(dx, dz);
                    if (dirIndex >= 0) {
                        int distance = Math.abs(dx) + Math.abs(dz);
                        entries.get(dirIndex).add(new PillarEntry(checkPos, distance));
                    }
                }
            }
        }

        // 每个方向按距离排序，并提取坐标
        Map<Integer, List<BlockPos>> result = new HashMap<>();
        for (int i = 0; i < 8; i++) {
            result.put(i, entries.get(i).stream()
                    .sorted(Comparator.comparingInt(PillarEntry::distance))
                    .map(PillarEntry::pos)
                    .collect(Collectors.toList()));
        }
        return result;
    }

    private record PillarEntry(BlockPos pos, int distance) {}

    private int getDirectionIndex(int dx, int dz) {
        if (dx == 0 && dz < 0) return 0;
        if (dx > 0 && dz < 0) return 1;
        if (dx > 0 && dz == 0) return 2;
        if (dx > 0 && dz > 0) return 3;
        if (dx == 0 && dz > 0) return 4;
        if (dx < 0 && dz > 0) return 5;
        if (dx < 0 && dz == 0) return 6;
        if (dx < 0 && dz < 0) return 7;
        return -1;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EarthAltarBlockEntity entity) {
        ItemStack centerStack = entity.inventory.getStackInSlot(0);
        if (centerStack.isEmpty()) {
            entity.craftProgress = 0;
            return;
        }

        Map<Integer, List<BlockPos>> pillars = entity.detectPillars();
        List<List<ItemStack>> directionItems = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            List<ItemStack> items = new ArrayList<>();
            for (BlockPos pillarPos : pillars.get(i)) {
                if (level.getBlockEntity(pillarPos) instanceof RitualPillarBlockEntity pillar) {
                    items.add(pillar.inventory.getStackInSlot(0));
                }
            }
            directionItems.add(items);
        }

        EarthAltarRecipeInput input = new EarthAltarRecipeInput(centerStack, directionItems);
        var recipeHolder = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.EARTH_ALTAR_RECIPE_TYPE.get(), input, level);

        if (recipeHolder.isEmpty()) {
            entity.craftProgress = 0;
            return;
        }

        EarthAltarRecipe recipe = recipeHolder.get().value();
        entity.maxCraftTime = recipe.getProcessingTime();
        entity.craftProgress++;

        if (entity.craftProgress >= entity.maxCraftTime) {
            // 消耗物品
            for (int dir = 0; dir < 8; dir++) {
                int needed = recipe.getInputsForDirection(dir).size();
                int consumed = 0;
                for (BlockPos pillarPos : pillars.get(dir)) {
                    if (consumed >= needed) break;
                    if (level.getBlockEntity(pillarPos) instanceof RitualPillarBlockEntity pillar) {
                        pillar.inventory.extractItem(0, 1, false);
                        consumed++;
                    }
                }
            }

            // 替换中心物品
            entity.inventory.extractItem(0, 1, false);
            entity.inventory.setStackInSlot(0, recipe.getResultItem().copy());
            entity.craftProgress = 0;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }

        entity.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("craftProgress", craftProgress);
        tag.putInt("maxCraftTime", maxCraftTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        craftProgress = tag.getInt("craftProgress");
        maxCraftTime = tag.contains("maxCraftTime") ? tag.getInt("maxCraftTime") : 100;
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

    public int getCraftProgress() { return craftProgress; }
    public int getMaxCraftTime() { return maxCraftTime; }
}