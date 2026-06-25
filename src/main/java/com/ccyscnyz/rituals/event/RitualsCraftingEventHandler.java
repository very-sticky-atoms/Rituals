package com.ccyscnyz.rituals.event;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.ingredient.DamageIngredient;
import com.ccyscnyz.rituals.ingredient.RitualsIngredient;
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
        List<ItemStack> items = new ArrayList<>(craftingContainer.getItems());
        CraftingInput input = CraftingInput.of(width, height, items);

        // 寻找当前匹配的配方
        player.level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, player.level())
                .ifPresent(recipeHolder -> {

                    // 遍历合成网格
                    for (int slotIndex = 0; slotIndex < craftingContainer.getContainerSize(); slotIndex++) {
                        ItemStack stackInSlot = craftingContainer.getItem(slotIndex);

                        for (Ingredient ingredient : recipeHolder.value().getIngredients()) {
                            if (ingredient.test(stackInSlot)) {
                                craftingContainer.setItem(slotIndex, RitualsIngredient.handleConsumption(ingredient, stackInSlot));
                            }
                        }
                    }
                });
    }
}