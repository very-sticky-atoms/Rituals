package com.ccyscnyz.rituals.registry.item;

import com.ccyscnyz.rituals.tags.RitualsTags;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;

public enum RitualsTiers implements Tier {
    COPPER(RitualsTags.Blocks.INCORRECT_FOR_COPPER, 250, 5.0F, 1.0F, 14, () -> Ingredient.of(Items.COPPER_INGOT)),
    STEEL(RitualsTags.Blocks.INCORRECT_FOR_STEEL, 750, 7.0F, 2.0F, 14, () -> Ingredient.of(Items.IRON_INGOT)), // 假设用铁修
    OBSIDIAN(RitualsTags.Blocks.INCORRECT_FOR_OBSIDIAN, 2500, 9.0F, 4.0F, 10, () -> Ingredient.of(Items.OBSIDIAN));

    // 复制原版的结构即可...
    private final TagKey<Block> incorrectBlocksForDrops;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantmentValue;
    private java.util.function.Supplier<Ingredient> repairIngredient;

    RitualsTiers(TagKey<Block> incorrectBlocksForDrops, int uses, float speed, float damage, int enchantmentValue, java.util.function.Supplier<Ingredient> repairIngredient) {
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantmentValue = enchantmentValue;
        this.repairIngredient = com.google.common.base.Suppliers.memoize(repairIngredient::get);
    }

    @Override public int getUses() { return this.uses; }
    @Override public float getSpeed() { return this.speed; }
    @Override public float getAttackDamageBonus() { return this.damage; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return this.incorrectBlocksForDrops; }
    @Override public int getEnchantmentValue() { return this.enchantmentValue; }
    @Override public Ingredient getRepairIngredient() { return this.repairIngredient.get(); }
}