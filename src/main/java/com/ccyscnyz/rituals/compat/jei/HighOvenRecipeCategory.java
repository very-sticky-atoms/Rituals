package com.ccyscnyz.rituals.compat.jei;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.HighOvenRecipe;
import com.ccyscnyz.rituals.registry.block.RitualsBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HighOvenRecipeCategory implements IRecipeCategory<HighOvenRecipe> {

    public static final RecipeType<HighOvenRecipe> TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "high_oven"), HighOvenRecipe.class);

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "textures/gui/jei/high_oven.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    public HighOvenRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 0, 0, 176, 85);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(RitualsBlocks.HIGH_OVEN.get()));
        this.title = Component.translatable("jei.rituals.high_oven");
    }

    @Override
    public RecipeType<HighOvenRecipe> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public int getWidth() { return 176; }

    @Override
    public int getHeight() { return 85; }

    @Nullable
    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HighOvenRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> inputs = recipe.getInputs();
        int inputCount = inputs.size();

        // 动态计算输入槽的起始 Y，使它们垂直居中（30 到 51 范围内）
        int xInput = 30;
        int slotHeight = 18;
        int totalHeight = inputCount * slotHeight;
        int startY = 33 - totalHeight / 2; // 以 33 为中心

        for (int i = 0; i < inputCount; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, xInput, startY + i * slotHeight)
                    .addIngredients(inputs.get(i));
        }

        // 火种槽
        builder.addSlot(RecipeIngredientRole.INPUT, 65, 33)
                .addIngredients(recipe.getFuel());
        // 输出槽
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 33)
                .addItemStack(recipe.getResultItem());
    }

    @Override
    public void draw(HighOvenRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);

        String displaystring = "Time: " + recipe.getProcessingTime() + " ticks";
        if (recipe.getChance() != 1.0F) {
            displaystring += " (" + (int)(recipe.getChance() * 100) + "%)";
        }
        guiGraphics.drawString(Minecraft.getInstance().font, displaystring, 60, 65, 0xFF808080, false);
    }
}