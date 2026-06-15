package com.ccyscnyz.rituals.block.entity;

import com.ccyscnyz.rituals.recipe.EarthAltarRecipe;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipeContext;
import com.ccyscnyz.rituals.registry.blockentity.RitualsBlockEntities;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import com.ccyscnyz.rituals.script.RitualsContextHolder;
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
        public boolean isItemValid(int slot, ItemStack stack) { return true; }

        @Override
        public int getSlotLimit(int slot) { return 1; }
    };

    private int craftProgress = 0;
    private int maxCraftTime;
    private ResourceLocation currentRecipe = null;
    private EarthAltarRecipeContext.Callback callback = ctx -> {};

    // ---- 自动销毁上下文进行保护 ----
    private RitualsContextHolder ritualContext = null;

    public EarthAltarBlockEntity(BlockPos pos, BlockState state) {
        super(RitualsBlockEntities.EARTH_ALTAR.get(), pos, state);
    }

    private static final Vec3i[] DIRECTION_OFFSETS = {
            new Vec3i(0, 0, -1),   // 北
            new Vec3i(1, 0, -1),   // 东北
            new Vec3i(1, 0, 0),    // 东
            new Vec3i(1, 0, 1),    // 东南
            new Vec3i(0, 0, 1),    // 南
            new Vec3i(-1, 0, 1),   // 西南
            new Vec3i(-1, 0, 0),   // 西
            new Vec3i(-1, 0, -1),  // 西北
    };

    private Map<Integer, List<RitualPillarBlockEntity>> detectPillars() {
        Map<Integer, List<RitualPillarBlockEntity>> entries = new HashMap<>();
        if (level == null) return new HashMap<>();
        int radius = 5;
        for (int dir = 0; dir < 8; dir++) {
            List<RitualPillarBlockEntity> detectResult = new ArrayList<>();
            for (int r = 1; r <= radius; r++) {
                BlockEntity block = level.getBlockEntity(this.worldPosition.offset(DIRECTION_OFFSETS[dir].multiply(r)));
                if (block instanceof RitualPillarBlockEntity pillar) {
                    detectResult.add(pillar);
                }
            }
            entries.put(dir, detectResult);
        }
        return entries;
    }

    // 辅助安全重置合成和清理沙箱的方法
    private void resetCraft() {
        this.craftProgress = 0;
        this.currentRecipe = null;
        this.callback = ctx -> {};
        if (this.ritualContext != null) {
            this.ritualContext.close();
            this.ritualContext = null;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EarthAltarBlockEntity entity) {
        ItemStack centerStack = entity.inventory.getStackInSlot(0);
        if (centerStack.isEmpty()) {
            entity.resetCraft();
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
                RitualsRecipeTypes.EARTH_ALTAR_RECIPE_TYPE.get(), context, level, entity.currentRecipe);

        if (recipeHolder.isEmpty()) {
            entity.resetCraft();
            return;
        }

        EarthAltarRecipe recipe = recipeHolder.get().value();
        ResourceLocation recipeId = recipeHolder.get().id();

        if (!recipeId.equals(entity.currentRecipe)) {
            entity.resetCraft(); // 清理旧配方对应的一切环境
            entity.currentRecipe = recipeId;
            entity.maxCraftTime = recipe.getProcessingTime();

            // 为当前新匹配上的仪式建立托管沙箱
            entity.ritualContext = RitualsContextHolder.create();

            // 传入长生命周期沙箱（内部使用 .get() 传参）并执行 start 脚本
            EarthAltarRecipeContext.StartScriptResult startScriptResult = recipe.runStartScript(
                    entity.ritualContext.get(), recipeId.withSuffix("/start"), context, entity.maxCraftTime, entity.callback);

            entity.maxCraftTime = startScriptResult.processingTime();
            entity.callback = startScriptResult.callback();
        }

        entity.craftProgress++;

        if (entity.craftProgress > 0 && level.getGameTime() % 8 == 0) {
            float progressRatio = (float) entity.craftProgress / entity.maxCraftTime;
            float volume = 0.2f + progressRatio * 1.2f;
            float pitch = 0.5f + progressRatio * 1.5f;
            level.playSound(null, pos, SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, volume, pitch);
        }

        // 合成完毕
        if (entity.craftProgress >= entity.maxCraftTime) {
            // 1. 正常执行 finish 脚本
            EarthAltarRecipeContext.FinishScriptResult finishScriptResult = recipe.runFinishScript(
                    entity.ritualContext.get(), recipeId.withSuffix("/finish"), context, entity.callback);

            EarthAltarRecipeContext contextNew = finishScriptResult.context();
            EarthAltarRecipeContext.Callback finalCallback = finishScriptResult.callback();

            // 2. 🌟 核心修正：断开实体的 ritualContext 与 setRemoved 的强绑定
            // 在调用可能导致方块自毁的闭包前，先把上下文从实体中“偷”出来存为局部变量
            RitualsContextHolder activeContext = entity.ritualContext;

            // 将实体的指针清空，这样即使闭包内触发了 setRemoved()，也不会触发 entity.ritualContext.close() 导致自噬
            entity.ritualContext = null;

            // 3. 触发由 JS 产生的闭包完成回调（此时自毁不会伤及正在运行的 activeContext）
            if (finalCallback != null) {
                try {
                    finalCallback.call(contextNew);
                } catch (Exception e) {
                    com.ccyscnyz.rituals.Rituals.LOGGER.error("Error invoking ritual complete callback", e);
                }
            }

            // 4. 判断一下方块是否已经被脚本自毁了
            boolean alreadyRemoved = entity.isRemoved();

            // 5. 如果方块还没被摧毁，我们继续正常把产物塞进去
            if (!alreadyRemoved) {
                for (int dir = 0; dir < 8; dir++) {
                    int index = 0;
                    for (RitualPillarBlockEntity pillar : pillars.get(dir)) {
                        pillar.inventory.extractItem(0, 1, false);
                        pillar.inventory.setStackInSlot(0, contextNew.directionItems().get(dir).get(index).copy());
                        pillar.setChanged();
                        level.sendBlockUpdated(pillar.getBlockPos(), pillar.getBlockState(), pillar.getBlockState(), 2);
                        index++;
                    }
                }
                entity.inventory.extractItem(0, 1, false);
                entity.inventory.setStackInSlot(0, contextNew.center().copy());
            }

            // 6. 重置基础状态
            entity.craftProgress = 0;
            entity.currentRecipe = null;
            entity.callback = ctx -> {};

            // 7. 🌟 此时脚本、物品操作全部安全落幕，局部变量里干净地关闭沙箱
            if (activeContext != null) {
                activeContext.close();
            }

            // 8. 播放视听特效（如果方块还在的话）
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.1);
                serverLevel.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 2.0F, 1.0F);
            }

            return;
        }

        entity.setChanged();
        level.sendBlockUpdated(pos, state, state, 2);

        if (entity.craftProgress > 0 && level instanceof ServerLevel serverLevel) {
            int particleCount = entity.craftProgress / 5 + 1;
            for (int i = 0; i < particleCount; i++) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double distance = 1.5 + level.random.nextDouble() * 2.5;
                double x = pos.getX() + 0.5 + Math.cos(angle) * distance;
                double z = pos.getZ() + 0.5 + Math.sin(angle) * distance;
                double y = pos.getY() + 0.2 + level.random.nextDouble() * 0.6;

                float progress = (float) entity.craftProgress / entity.maxCraftTime;
                double dx = (pos.getX() + 0.5 - x) * 12 * progress;
                double dy = (pos.getY() + 0.8 - y) * 7.5 * progress;
                double dz = (pos.getZ() + 0.5 - z) * 12 * progress;

                serverLevel.sendParticles(ParticleTypes.DUST_PLUME, x, y, z, 0, dx, dy, dz, 0.05);
            }
        }
    }

    // ---- 清除常驻上下文 ----
    @Override
    public void setRemoved() {
        if (this.ritualContext != null) {
            this.ritualContext.close(); // 优雅关机
            this.ritualContext = null;
        }
        super.setRemoved();
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
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    public int getCraftProgress() { return craftProgress; }
    public int getMaxCraftTime() { return maxCraftTime; }
}