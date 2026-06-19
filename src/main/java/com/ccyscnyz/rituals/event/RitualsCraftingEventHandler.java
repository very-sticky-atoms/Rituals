package com.ccyscnyz.rituals.event;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.ingredient.DamageIngredient;
import com.ccyscnyz.rituals.ingredient.TransformIngredient;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.Container;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Rituals.MODID)
public class RitualsCraftingEventHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        Container container = event.getInventory();
        if (!(container instanceof CraftingContainer craftingContainer)) return;

        int width = craftingContainer.getWidth();
        int height = craftingContainer.getHeight();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
            items.add(craftingContainer.getItem(i));
        }
        CraftingInput input = CraftingInput.of(width, height, items);

        // 寻找当前匹配的配方
        player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .ifPresent(recipeHolder -> {

                    // 遍历合成网格
                    for (int i = 0; i < craftingContainer.getContainerSize(); i++) {
                        final int slotIndex = i;
                        ItemStack stackInSlot = craftingContainer.getItem(i);
                        if (stackInSlot.isEmpty()) continue;

                        for (Ingredient ingredient : recipeHolder.value().getIngredients()) {
                            if (ingredient.test(stackInSlot)) {
                                ICustomIngredient custom = ingredient.getCustomIngredient();

                                // 扣耐久逻辑
                                if (custom instanceof DamageIngredient dmg) {
                                    // 预扣除：算出扣完耐久后的新物品
                                    ItemStack ruined = dmg.consume(stackInSlot);

                                    if (!ruined.isEmpty()) {
                                        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().execute(() -> {
                                            craftingContainer.setItem(slotIndex, ruined);
                                        });
                                    }
                                }

                                // 物品转换逻辑
                                else if (custom instanceof TransformIngredient trans) {
                                    ItemStack remnant = trans.consume();
                                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().execute(() -> {
                                        craftingContainer.setItem(slotIndex, remnant);
                                    });
                                }
                                break;
                            }
                        }
                    }
                });
    }
}