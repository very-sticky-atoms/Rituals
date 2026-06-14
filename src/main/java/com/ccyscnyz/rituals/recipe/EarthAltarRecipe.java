package com.ccyscnyz.rituals.recipe;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeSerializers;
import com.ccyscnyz.rituals.registry.recipe.RitualsRecipeTypes;
import com.ccyscnyz.rituals.script.RitualsScriptEngine;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import java.util.*;

public class EarthAltarRecipe implements Recipe<EarthAltarRecipeContext> {

    private final Ingredient center;
    private final List<List<Ingredient>> inputs;
    private final ItemStack output;
    private final int processingTime;
    private final Optional<String> craftStartScript;
    private final Optional<String> craftFinishScript; // 新增：JS 脚本内容

    public EarthAltarRecipe(Ingredient center, List<List<Ingredient>> inputs, ItemStack output,
                            int processingTime, Optional<String> craftStartScript, Optional<String> craftFinishScript) {
        if (inputs.size() != 8) throw new IllegalArgumentException("Must have exactly 8 directions");
        this.center = center;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.processingTime = processingTime;
        this.craftStartScript = craftStartScript;
        this.craftFinishScript = craftFinishScript;
    }

    @Override
    public boolean matches(EarthAltarRecipeContext input, Level level) {
        if (!center.test(input.center())) return false;
        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> required = inputs.get(dir);
            List<ItemStack> actual = input.directionItems().get(dir);
            if (required.size() != actual.size()) return false;
            for (int i = 0; i < required.size(); i++) {
                if (!required.get(i).test(actual.get(i))) return false;
            }
        }
        return true;
    }

    public EarthAltarRecipeContext.StartScriptResult runStartScript(ResourceLocation scriptSource, EarthAltarRecipeContext context, int processingTime, EarthAltarRecipeContext.Callback callback) {
        // JSON脚本
        if (craftStartScript.isPresent() && !craftStartScript.get().isEmpty()) {
            Rituals.LOGGER.debug("Executing script {}: {}", scriptSource, craftStartScript.get());
            try {
                Map<String, Object> bindings = new java.util.HashMap<>();
                int[] ptContainer = {processingTime};
                EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);
                bindings.put("context",context.copy());
                bindings.put("processingTime",ptContainer);
                bindings.put("callback",cbkContainer);
                RitualsScriptEngine.executeCached(scriptSource, craftStartScript.get(), bindings);
                return new EarthAltarRecipeContext.StartScriptResult(ptContainer[0],cbkContainer.value);
            } catch (ScriptException e) {
                Rituals.LOGGER.error("Failed to execute script {}: {}", scriptSource, e.getMessage());
            }
        }
        return new EarthAltarRecipeContext.StartScriptResult(processingTime, callback);
    }
    //获取最终产物（优先级: 脚本 > 默认输出）
    public EarthAltarRecipeContext.FinishScriptResult runFinishScript(ResourceLocation scriptSource, EarthAltarRecipeContext context, EarthAltarRecipeContext.Callback callback) {

        // JSON脚本
        if (craftFinishScript.isPresent() && !craftFinishScript.get().isEmpty()) {
            Rituals.LOGGER.debug("Executing script {}: {}", scriptSource, craftFinishScript.get());
            try {
                Map<String, Object> bindings = new java.util.HashMap<>();
                EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();
                EarthAltarRecipeContext.CallbackContainer cbkContainer =new EarthAltarRecipeContext.CallbackContainer(callback);
                bindings.put("context",ctxContainer);
                bindings.put("callback",cbkContainer);
                RitualsScriptEngine.executeCached(scriptSource, craftFinishScript.get(), bindings);
                EarthAltarRecipeContext.FinishScriptResult scriptResult = new EarthAltarRecipeContext.FinishScriptResult(ctxContainer.value.copy(),cbkContainer.value);
                Rituals.LOGGER.debug("Script returned: {}", scriptResult);
                return scriptResult;
            } catch (ScriptException e) {
                Rituals.LOGGER.error("Failed to execute script {}: {}", scriptSource, e.getMessage());
            }
        }
        List<List<ItemStack>> consumed = new ArrayList<>();
        for(int dir = 0; dir < 8; dir++){
            List<ItemStack> consumedDirection = new ArrayList<>();
            for (int i = 0; i < this.inputs.get(dir).size(); i++) {
                consumedDirection.add(ItemStack.EMPTY);
            }
            consumed.add(consumedDirection);
        }
        // 默认输出
        return new EarthAltarRecipeContext.FinishScriptResult(context.with(output.copy()).with(consumed),callback);
    }

    @Override
    public ItemStack assemble(EarthAltarRecipeContext input, net.minecraft.core.HolderLookup.Provider registries) {
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

    // ---- Codec ----
    public static final MapCodec<EarthAltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("center").forGetter(r -> r.center),
                    Codec.list(Codec.list(Ingredient.CODEC)).fieldOf("inputs").forGetter(r -> r.inputs),
                    ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                    Codec.INT.fieldOf("processingTime").forGetter(r -> r.processingTime),
                    Codec.STRING.optionalFieldOf("craftStartScript").forGetter(r -> r.craftStartScript),
                    Codec.STRING.optionalFieldOf("craftFinishScript").forGetter(r -> r.craftFinishScript)
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
                        buf.writeBoolean(recipe.craftStartScript.isPresent());
                        recipe.craftStartScript.ifPresent(buf::writeUtf);
                        buf.writeBoolean(recipe.craftFinishScript.isPresent());
                        recipe.craftFinishScript.ifPresent(buf::writeUtf);
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
                        Optional<String> craftStartScript = buf.readBoolean() ?
                                Optional.of(buf.readUtf()) : Optional.empty();
                        Optional<String> craftFinishScript = buf.readBoolean() ?
                                Optional.of(buf.readUtf()) : Optional.empty();
                        return new EarthAltarRecipe(center, inputs, output, time, craftStartScript, craftFinishScript);
                    }
            );

    public static class Type implements RecipeType<EarthAltarRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
}