package com.ccyscnyz.rituals.datagen;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.annotation.AutoBlockItem;
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

public class RitualsItemModelProvider extends ItemModelProvider {

    public RitualsItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Rituals.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (Item item : BuiltInRegistries.ITEM) {
            if (
                item instanceof BlockItem blockItem &&
                Objects.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace(), Rituals.MODID)
            ) {

                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);

                // 创建继承方块模型的物品模型
                ItemModelBuilder builder = withExistingParent(itemId.getPath(),
                        ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), "block/" + blockId.getPath()));

                // 为所有显示上下文添加变换（使用 ItemDisplayContext 枚举）
                builder.transforms()
                        .transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        .rotation(75, 45, 0)
                        .translation(0, 2.5f, 0)
                        .scale(0.375f)
                        .end()
                        .transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                        .rotation(75, 45, 0)
                        .translation(0, 2.5f, 0)
                        .scale(0.375f)
                        .end()
                        .transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                        .rotation(0, 45, 0)
                        .scale(0.4f)
                        .end()
                        .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND)
                        .rotation(0, 225, 0)
                        .scale(0.4f)
                        .end()
                        .transform(ItemDisplayContext.GROUND)
                        .translation(0, 3, 0)
                        .scale(0.25f)
                        .end()
                        .transform(ItemDisplayContext.GUI)
                        .rotation(30, 225, 0)
                        .scale(0.625f)
                        .end()
                        .transform(ItemDisplayContext.FIXED)
                        .scale(0.5f)
                        .end()
                        .end(); // 结束 transforms()
            }
        }
    }
}