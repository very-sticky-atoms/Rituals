package com.ccyscnyz.rituals.recipe;

import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeSerializers;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class HighOvenRecipe implements Recipe<HighOvenRecipeInput> {

    private final List<Ingredient> inputs; // 固定3个
    private final Ingredient fuel;
    private final ItemStack output;
    private final float chance;
    private final int processingTime;

    public HighOvenRecipe(List<Ingredient> inputs, Ingredient fuel, ItemStack output, float chance, int processingTime) {
        if (inputs.size() != 3) {
            throw new IllegalArgumentException("HighOvenRecipe must have exactly 3 inputs");
        }
        this.inputs = List.copyOf(inputs);
        this.fuel = fuel;
        this.output = output;
        this.chance = chance;
        this.processingTime = processingTime;
    }

    @Override
    public boolean matches(HighOvenRecipeInput input, Level level) {
        if (!fuel.test(input.fuel())) return false;

        // 无序匹配三个输入
        List<ItemStack> remaining = new ArrayList<>();
        remaining.add(input.input0());
        remaining.add(input.input1());
        remaining.add(input.input2());

        for (Ingredient ingredient : inputs) {
            boolean matched = false;
            for (int i = 0; i < remaining.size(); i++) {
                if (ingredient.test(remaining.get(i))) {
                    remaining.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(HighOvenRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.HolderLookup.Provider registries) {
        return output.copy();
    }
    // 无参数版，JEI 和其他地方常用
    public ItemStack getResultItem() {
        return output.copy();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RitualsRecipeSerializers.HIGH_OVEN_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RitualsRecipeTypes.HIGH_OVEN_RECIPE_TYPE.get();
    }

    public List<Ingredient> getInputs() { return inputs; }
    public Ingredient getFuel() { return fuel; }
    public float getChance() { return chance; }
    public int getProcessingTime() { return processingTime; }

    // ---- 序列化 ----
    public static final MapCodec<HighOvenRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.listOf().fieldOf("inputs").forGetter(r -> r.inputs),
                    Ingredient.CODEC.fieldOf("fuel").forGetter(r -> r.fuel),
                    ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                    Codec.FLOAT.optionalFieldOf("chance", 1.0F).forGetter(r -> r.chance),
                    Codec.INT.fieldOf("processingTime").forGetter(r -> r.processingTime)
            ).apply(instance, HighOvenRecipe::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, HighOvenRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> {
                        // 编码 inputs 列表
                        buf.writeVarInt(recipe.inputs.size());
                        for (Ingredient ing : recipe.inputs) {
                            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                        }
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.fuel);
                        ItemStack.STREAM_CODEC.encode(buf, recipe.output);
                        buf.writeFloat(recipe.chance);
                        buf.writeInt(recipe.processingTime);
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<Ingredient> inputs = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            inputs.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                        }
                        Ingredient fuel = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                        float chance = buf.readFloat();
                        int time = buf.readInt();
                        return new HighOvenRecipe(inputs, fuel, output, chance, time);
                    }
            );

    public static class Type implements RecipeType<HighOvenRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
}