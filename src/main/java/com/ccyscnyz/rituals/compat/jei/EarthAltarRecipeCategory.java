package com.ccyscnyz.rituals.compat.jei;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipe;
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

public class EarthAltarRecipeCategory implements IRecipeCategory<EarthAltarRecipe> {

    public static final RecipeType<EarthAltarRecipe> TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "earth_altar"),
                    EarthAltarRecipe.class);

    private static final ResourceLocation BACKGROUND_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Rituals.MODID, "textures/gui/jei/earth_altar.png");

    private final IDrawable background;
    private final IDrawable icon;
    private final Component title;

    // 八个方向单位向量(顺时针)
    private static final int[][] DIR_VECTORS = {
            { 0, -1 },
            { 1, -1 },
            { 1,  0 },
            { 1,  1 },
            { 0,  1 },
            {-1,  1 },
            {-1,  0 },
            {-1, -1 }
    };
    private static final int CENTER_X = 80;
    private static final int CENTER_Y = 80;
    private static final int SLOT_SPACING = 25; // 物品间距

    public EarthAltarRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(BACKGROUND_TEXTURE, 0, 0, 176, 176);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(RitualsBlocks.EARTH_ALTAR.get()));
        this.title = Component.translatable("jei.rituals.earth_altar");
    }

    @Override
    public RecipeType<EarthAltarRecipe> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() { return title; }

    @Override
    public int getWidth() { return 176; }

    @Override
    public int getHeight() { return 176; }

    @Nullable
    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EarthAltarRecipe recipe, IFocusGroup focuses) {
        // 中心物品
        builder.addSlot(RecipeIngredientRole.INPUT, CENTER_X, CENTER_Y)
                .addIngredients(recipe.getCenter());

        // 八个方向物品，按距离从近到远排列
        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> inputs = recipe.getInputsForDirection(dir);
            int dx = DIR_VECTORS[dir][0];
            int dy = DIR_VECTORS[dir][1];
            for (int i = 0; i < inputs.size(); i++) {
                int x = CENTER_X + dx * SLOT_SPACING * (i + 1);
                int y = CENTER_Y + dy * SLOT_SPACING * (i + 1);
                builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                        .addIngredients(inputs.get(i));
            }
        }

        // 输出物品（放在下方）
        builder.addSlot(RecipeIngredientRole.OUTPUT, CENTER_X, CENTER_Y + SLOT_SPACING * 4)
                .addItemStack(recipe.getResultItem());
    }

    @Override
    public void draw(EarthAltarRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);

        // 显示处理时间
        String time = "Time: " + recipe.getProcessingTime() + " ticks";
        guiGraphics.drawString(Minecraft.getInstance().font, time, 5, 5, 0xFF808080, false);

        // （可选）在每个方向物品上标注数字序号，用 drawString 绘制
        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> inputs = recipe.getInputsForDirection(dir);
            int dx = DIR_VECTORS[dir][0];
            int dy = DIR_VECTORS[dir][1];
            for (int i = 0; i < inputs.size(); i++) {
                int x = CENTER_X + dx * SLOT_SPACING * (i + 1) - 4;
                int y = CENTER_Y + dy * SLOT_SPACING * (i + 1) - 10;
                guiGraphics.drawString(Minecraft.getInstance().font, String.valueOf(i + 1), x, y, 0xFFFFAA00, true);
            }
        }
    }
}