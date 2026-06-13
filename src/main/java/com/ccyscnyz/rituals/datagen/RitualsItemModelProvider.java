package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Objects;
import java.util.Set;

public class RitualsItemModelProvider extends ItemModelProvider {

    // 排除列表：不自动生成模型的物品 ID
    private static final Set<ResourceLocation> EXCLUDED = Set.of(
            // ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "excluded_item")
    );

    public RitualsItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Rituals.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (!Objects.equals(itemId.getNamespace(), Rituals.MODID)) continue;
            if (EXCLUDED.contains(itemId)) continue;   // 跳过排除物品

            if (item instanceof BlockItem blockItem) {
                // BlockItem：继承方块模型
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                ItemModelBuilder builder = withExistingParent(itemId.getPath(),
                        ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()));

                builder.transforms()
                        .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        .rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
                        .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                        .rotation(75, 45, 0).translation(0, 2.5f, 0).scale(0.375f).end()
                        .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                        .rotation(0, 45, 0).scale(0.4f).end()
                        .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                        .rotation(0, 225, 0).scale(0.4f).end()
                        .transform(ItemDisplayContext.GROUND)
                        .translation(0, 3, 0).scale(0.25f).end()
                        .transform(ItemDisplayContext.GUI)
                        .rotation(30, 225, 0).scale(0.625f).end()
                        .transform(ItemDisplayContext.FIXED)
                        .scale(0.5f).end()
                        .end(); // transforms()
            } else {
                // 普通物品：生成简单的 item/generated 模型
                withExistingParent(itemId.getPath(), ResourceLocation.withDefaultNamespace("item/generated"))
                        .texture("layer0", ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "item/" + itemId.getPath()));
            }
        }
    }
}