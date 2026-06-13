package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ModLootTableProvider extends LootTableProvider {

    // 排除列表
    private static final Set<ResourceLocation> EXCLUDE_BLOCKS = Set.of(
            // ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "excluded_block")
    );

    public ModLootTableProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Set.of(), List.of(
                new SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)
        ), registries);
    }

    public static class ModBlockLoot extends BlockLootSubProvider {
        protected ModBlockLoot(HolderLookup.Provider registries) {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
        }

        @Override
        protected void generate() {
            for (var holder : RitualsBlocks.BLOCKS.getEntries()) {
                Block block = holder.get();
                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
                if (!EXCLUDE_BLOCKS.contains(id)) {
                    dropSelf(block);
                }
            }
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return RitualsBlocks.BLOCKS.getEntries().stream()
                    .map(holder -> (Block) holder.get())
                    .filter(block -> !EXCLUDE_BLOCKS.contains(BuiltInRegistries.BLOCK.getKey(block)))
                    .collect(Collectors.toList());
        }
    }
}