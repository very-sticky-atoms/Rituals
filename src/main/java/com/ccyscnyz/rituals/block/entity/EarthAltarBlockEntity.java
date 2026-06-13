package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.recipe.EarthAltarRecipe;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipeContext;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
    private int maxCraftTime;
    private Optional<EarthAltarRecipe> currentRecipe = Optional.empty();

    public EarthAltarBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.EARTH_ALTAR.get(), pos, state);
    }

    // 八个方向偏移（依次北、东北、东、东南、南、西南、西、西北）
    private static final Vec3i[] DIRECTION_OFFSETS = {
            new Vec3i(0, 0, -1),
            new Vec3i(1, 0, -1),
            new Vec3i(1, 0, 0),
            new Vec3i(1, 0, 1),
            new Vec3i(0, 0, 1),
            new Vec3i(-1, 0, 1),
            new Vec3i(-1, 0, 0),
            new Vec3i(-1, 0, -1),
    };


    //检测仪式柱
    private Map<Integer, List<RitualPillarBlockEntity>> detectPillars() {
        Map<Integer, List<RitualPillarBlockEntity>> entries = new HashMap<>();
        if (level == null) return new HashMap<>();
        int radius = 5;
        for (int dir = 0; dir < 8; dir++) {
            List<RitualPillarBlockEntity> detectResult = new ArrayList<>();
            for (int r = 1; r<=radius; r++) {
                BlockEntity block = level.getBlockEntity(this.worldPosition.offset(DIRECTION_OFFSETS[dir]));
                if (block instanceof RitualPillarBlockEntity pillar) {
                    detectResult.add(pillar);
                }
            }
            entries.put(dir, detectResult);
        }

        return entries;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EarthAltarBlockEntity entity) {
        ItemStack centerStack = entity.inventory.getStackInSlot(0);
        if (centerStack.isEmpty()) {
            entity.craftProgress = 0;
            return;
        }

        Map<Integer, List<RitualPillarBlockEntity>> pillars = entity.detectPillars();
        List<List<ItemStack>> directionItems = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            List<ItemStack> items = new ArrayList<>();
            for (RitualPillarBlockEntity pillar : pillars.get(i)) {
                items.add(pillar.inventory.getStackInSlot(0));
            }
            directionItems.add(items);
        }

        EarthAltarRecipeContext context = new EarthAltarRecipeContext(centerStack, directionItems, level, entity.worldPosition);
        var recipeHolder = level.getRecipeManager().getRecipeFor(
                RitualsRecipeTypes.EARTH_ALTAR_RECIPE_TYPE.get(), context, level);

        if (recipeHolder.isEmpty()) {
            entity.craftProgress = 0;
            return;
        }

        EarthAltarRecipe recipe = recipeHolder.get().value();
        ResourceLocation recipeId = recipeHolder.get().id();
        if(entity.currentRecipe.isEmpty() || !entity.currentRecipe.get().equals(recipe)){
            entity.craftProgress = 0;
            entity.currentRecipe = Optional.of(recipe);
            entity.maxCraftTime = recipe.runStartScript(recipeId,context);
        }
        entity.craftProgress++;

        if (entity.craftProgress > 0 && level.getGameTime() % 8 == 0) { // 每4 tick播放一次
            float progressRatio = (float) entity.craftProgress / entity.maxCraftTime;
            float volume = 0.2f + progressRatio * 1.2f; // 音量从 0.2 到 1.5
            float pitch = 0.5f + progressRatio * 1.5f;  // 音高从 0.5 到 2.0
            level.playSound(null, pos, SoundEvents.WEATHER_RAIN,
                    SoundSource.BLOCKS, volume, pitch);
        }

        if (entity.craftProgress >= entity.maxCraftTime) {
            EarthAltarRecipeContext contextNew = recipe.runFinishScript(recipeId,context);
            for (int dir = 0; dir < 8; dir++) {
                for (int i = 1; i < pillars.get(dir).size(); i++) {
                    RitualPillarBlockEntity pillar = pillars.get(dir).get(i);
                    pillar.inventory.extractItem(0, 1, false);
                    pillar.inventory.setStackInSlot(0,contextNew.directionItems().get(dir).get(i));
                }
            }

            entity.inventory.extractItem(0, 1, false);
            entity.inventory.setStackInSlot(0, contextNew.center().copy());

            entity.craftProgress = 0;

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL,
                        SoundSource.BLOCKS, 2.0F, 1.0F);
            }
        }

        entity.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);

        // 合成进度粒子效果
        if (entity.craftProgress > 0 && level instanceof ServerLevel serverLevel) {
            int particleCount = entity.craftProgress / 5 + 1; // 进度越多粒子越多
            for (int i = 0; i < particleCount; i++) {
                // 在祭坛周围随机位置生成粒子
                double angle = level.random.nextDouble() * Math.PI * 2;
                double distance = 1.5 + level.random.nextDouble() * 2.5;
                double x = pos.getX() + 0.5 + Math.cos(angle) * distance;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * distance;
                double y = pos.getY() + 0.2 + level.random.nextDouble() * 0.6;

                float progress = (float) entity.craftProgress/entity.maxCraftTime;
                // 粒子向祭坛中心移动
                double dx = (pos.getX() + 0.5 - x) * 12 * progress;
                double dy = (pos.getY() + 0.8 - y) * 7.5 * progress;
                double dz = (pos.getZ() + 0.5 - z) * 12 * progress;

                serverLevel.sendParticles(
                        ParticleTypes.DUST_PLUME, // 土黄色粉尘粒子，大地主题
                        x, y, z,
                        0,    // 数量
                        dx, dy, dz,  // 向中心移动的速度
                        0.05  // 粒子速度偏差
                );
            }
        }
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