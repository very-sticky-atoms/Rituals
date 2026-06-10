package com.ccyscnyz.rituals.tags;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class RitualsTags {
    public static class Blocks {
        public static final TagKey<Block> INCORRECT_FOR_COPPER = tag("incorrect_for_copper_tool");
        public static final TagKey<Block> INCORRECT_FOR_STEEL = tag("incorrect_for_steel_tool");
        public static final TagKey<Block> INCORRECT_FOR_OBSIDIAN = tag("incorrect_for_obsidian_tool");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(Rituals.MODID, name));
        }
    }
}
