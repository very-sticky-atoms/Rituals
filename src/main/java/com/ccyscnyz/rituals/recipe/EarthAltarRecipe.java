package com.ccyscnyz.rituals.recipe;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeSerializers;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import com.ccyscnyz.rituals.script.EarthAltarScriptEngine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class EarthAltarRecipe implements Recipe<EarthAltarRecipeInput> {

    private final Ingredient center;
    private final List<List<Ingredient>> inputs;
    private final ItemStack output;
    private final int processingTime;
    private final Optional<String> script; // 新增：JS 脚本内容

    // 输出修改器注册表
    private static final Map<ResourceLocation, OutputModifier> MODIFIERS = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface OutputModifier {
        ItemStack modify(EarthAltarRecipe recipe, EarthAltarRecipeInput input, Level level, BlockPos pos);
    }

    public static void registerModifier(ResourceLocation recipeId, OutputModifier modifier) {
        MODIFIERS.put(recipeId, modifier);
    }

    public static void unregisterModifier(ResourceLocation recipeId) {
        MODIFIERS.remove(recipeId);
    }

    public static void registerModifiers(List<ResourceLocation> recipeIds, OutputModifier modifier) {
        recipeIds.forEach(id -> MODIFIERS.put(id, modifier));
    }

    public static void unregisterModifiers(List<ResourceLocation> recipeIds) {
        recipeIds.forEach(MODIFIERS::remove);
    }

    public EarthAltarRecipe(Ingredient center, List<List<Ingredient>> inputs, ItemStack output,
                            int processingTime, Optional<String> script) {
        if (inputs.size() != 8) throw new IllegalArgumentException("Must have exactly 8 directions");
        this.center = center;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.processingTime = processingTime;
        this.script = script;
    }

    @Override
    public boolean matches(EarthAltarRecipeInput input, Level level) {
        if (!center.test(input.getCenter())) return false;
        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> required = inputs.get(dir);
            List<ItemStack> actual = input.getDirection(dir);
            List<ItemStack> nonEmptyActual = new ArrayList<>();
            for (ItemStack stack : actual) {
                if (!stack.isEmpty()) nonEmptyActual.add(stack);
            }
            if (required.size() != nonEmptyActual.size()) return false;
            for (int i = 0; i < required.size(); i++) {
                if (!required.get(i).test(nonEmptyActual.get(i))) return false;
            }
        }
        return true;
    }

    //获取最终产物（优先级：修改器 > 脚本 > 默认输出）
    public ItemStack getAssembledOutput(ResourceLocation recipeId, EarthAltarRecipeInput input,
                                        Level level, BlockPos pos) {
        // 外部注册的修改器
        OutputModifier modifier = MODIFIERS.get(recipeId);
        if (modifier != null) {
            ItemStack modified = modifier.modify(this, input, level, pos);
            if (modified != null) return modified;
        }

        // JSON脚本
        if (script.isPresent() && !script.get().isEmpty()) {
            Rituals.LOGGER.debug("Executing script for recipe {}: {}", recipeId, script.get());
            try {
                Map<String, Object> bindings = new java.util.HashMap<>();
                bindings.put("level", level);
                bindings.put("pos", pos);
                bindings.put("center", input.getCenter());
                bindings.put("directions", input.directionItems());
                ItemStack scriptResult = EarthAltarScriptEngine.executeCached(recipeId, script.get(), bindings);
                if (scriptResult != null) {
                    Rituals.LOGGER.debug("Script returned: {}", scriptResult);
                    return scriptResult;
                } else {
                    Rituals.LOGGER.debug("Script returned null, using default output.");
                }
            } catch (ScriptException e) {
                Rituals.LOGGER.error("Failed to execute script for recipe {}: {}", recipeId, e.getMessage());
            }
        }

        // 默认输出
        return output.copy();
    }

    @Override
    public ItemStack assemble(EarthAltarRecipeInput input, net.minecraft.core.HolderLookup.Provider registries) {
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

    public ItemStack getResultItem() { return output.copy(); }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RitualsRecipeSerializers.EARTH_ALTAR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RitualsRecipeTypes.EARTH_ALTAR_RECIPE_TYPE.get();
    }

    public Ingredient getCenter() { return center; }
    public List<Ingredient> getInputsForDirection(int dir) { return inputs.get(dir); }
    public int getProcessingTime() { return processingTime; }
    public Optional<String> getScript() { return script; }

    // ---- Codec ----
    public static final MapCodec<EarthAltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("center").forGetter(r -> r.center),
                    Codec.list(Codec.list(Ingredient.CODEC)).fieldOf("inputs").forGetter(r -> r.inputs),
                    ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                    Codec.INT.fieldOf("processingTime").forGetter(r -> r.processingTime),
                    Codec.STRING.optionalFieldOf("script").forGetter(r -> r.script)
            ).apply(instance, EarthAltarRecipe::new)
    );

    // ---- StreamCodec ----
    public static final StreamCodec<RegistryFriendlyByteBuf, EarthAltarRecipe> STREAM_CODEC =
            StreamCodec.of(
                    (buf, recipe) -> {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.center);
                        buf.writeVarInt(recipe.inputs.size());
                        for (List<Ingredient> list : recipe.inputs) {
                            buf.writeVarInt(list.size());
                            for (Ingredient ing : list) {
                                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                            }
                        }
                        ItemStack.STREAM_CODEC.encode(buf, recipe.output);
                        buf.writeInt(recipe.processingTime);
                        buf.writeBoolean(recipe.script.isPresent());
                        recipe.script.ifPresent(buf::writeUtf);
                    },
                    buf -> {
                        Ingredient center = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        int outerSize = buf.readVarInt();
                        List<List<Ingredient>> inputs = new ArrayList<>();
                        for (int i = 0; i < outerSize; i++) {
                            int innerSize = buf.readVarInt();
                            List<Ingredient> inner = new ArrayList<>();
                            for (int j = 0; j < innerSize; j++) {
                                inner.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                            }
                            inputs.add(inner);
                        }
                        ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                        int time = buf.readInt();
                        Optional<String> script = buf.readBoolean() ?
                                Optional.of(buf.readUtf()) : Optional.empty();
                        return new EarthAltarRecipe(center, inputs, output, time, script);
                    }
            );

    public static class Type implements RecipeType<EarthAltarRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
}