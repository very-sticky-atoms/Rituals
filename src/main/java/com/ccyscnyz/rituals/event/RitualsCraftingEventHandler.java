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

@EventBusSubscriber(modid = Rituals.MODID) // 省略 bus，默认就是 GAME
public class RitualsCraftingEventHandler {

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        Container container = event.getInventory();
        if (!(container instanceof CraftingContainer craftingContainer)) return;

        // 1.21.1 强行包装出 RecipeManager 认识的 CraftingInput
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
                                    // 预扣除：调用你写好的 consume，算出扣完耐久后的新物品
                                    ItemStack ruined = dmg.consume(stackInSlot);

                                    // 因为原版马上就会执行 count - 1，所以如果物品没碎，我们得强行把 count 设为 2
                                    // 这样原版减完 1 之后，留在格子里的刚好就是 count = 1 且掉了耐久的武器！
                                    if (!ruined.isEmpty()) {
                                        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().execute(() -> {
                                            // 强制将扣除耐久后的物品放回对应索引
                                            craftingContainer.setItem(slotIndex, ruined);
                                        });
                                    }
                                }

                                // 物品转换逻辑（如岩浆桶变水桶）
                                else if (custom instanceof TransformIngredient trans) {
                                    ItemStack remnant = trans.consume();
                                    net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().execute(() -> {
                                        // 强制将扣除耐久后的物品放回对应索引
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