package com.ccyscnyz.rituals.compat.jade;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.registry.item.RitualsItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.addon.harvest.ToolHandler;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.IWailaClientRegistration;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@WailaPlugin
public class RitualsJadePlugin implements IWailaPlugin {

    // 自定义挖掘等级标签
    private static final TagKey<Block> NEEDS_COPPER_TOOL   = createTag("needs_copper_tool");
    private static final TagKey<Block> NEEDS_STEEL_TOOL    = createTag("needs_steel_tool");
    private static final TagKey<Block> NEEDS_OBSIDIAN_TOOL = createTag("needs_obsidian_tool");

    private static TagKey<Block> createTag(String name) {
        return BlockTags.create(ResourceLocation.fromNamespaceAndPath(Rituals.MODID, name));
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        HarvestToolProvider.registerHandler(new CustomToolHandler(
                NEEDS_COPPER_TOOL,
                () -> new ItemStack(RitualsItems.COPPER_PICKAXE.get()),
                ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "copper")
        ));

        HarvestToolProvider.registerHandler(new CustomToolHandler(
                NEEDS_STEEL_TOOL,
                () -> new ItemStack(RitualsItems.STEEL_PICKAXE.get()),
                ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "steel")
        ));

        HarvestToolProvider.registerHandler(new CustomToolHandler(
                NEEDS_OBSIDIAN_TOOL,
                () -> new ItemStack(RitualsItems.OBSIDIAN_PICKAXE.get()),
                ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "obsidian")
        ));
    }

    static class CustomToolHandler implements ToolHandler {
        private final TagKey<Block> tag;
        private final Supplier<ItemStack> toolSupplier;
        private final ResourceLocation uid;

        CustomToolHandler(TagKey<Block> tag, Supplier<ItemStack> toolSupplier, ResourceLocation uid) {
            this.tag = tag;
            this.toolSupplier = toolSupplier;
            this.uid = uid;
        }

        @Override
        public ResourceLocation getUid() {
            return uid;
        }

        @Override
        public ItemStack test(BlockState state, Level world, BlockPos pos) {
            return state.is(tag) ? toolSupplier.get() : ItemStack.EMPTY;
        }

        @Override
        public List<ItemStack> getTools() {
            return List.of(toolSupplier.get());
        }
    }
}