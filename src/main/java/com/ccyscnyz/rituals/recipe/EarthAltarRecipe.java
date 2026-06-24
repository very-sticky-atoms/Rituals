package com.ccyscnyz.rituals.recipe;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.ingredient.RitualsIngredient;
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
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.*;

public class EarthAltarRecipe implements Recipe<EarthAltarRecipeContext> {

    private final Ingredient center;
    private final List<List<Ingredient>> inputs;
    private final ItemStack output;
    private final int processingTime;

    private final Optional<ResourceLocation> craftStartScriptUri;
    private final Optional<ResourceLocation> craftFinishScriptUri;

    public EarthAltarRecipe(Ingredient center, List<List<Ingredient>> inputs, ItemStack output,
                            int processingTime, Optional<ResourceLocation> craftStartScriptUri, Optional<ResourceLocation> craftFinishScriptUri) {
        if (inputs.size() != 8) throw new IllegalArgumentException("Must have exactly 8 directions");
        this.center = center;
        this.inputs = List.copyOf(inputs);
        this.output = output;
        this.processingTime = processingTime;
        this.craftStartScriptUri = craftStartScriptUri;
        this.craftFinishScriptUri = craftFinishScriptUri;
    }

    @Override
    public boolean matches(EarthAltarRecipeContext input, Level level) {

        if (!center.test(input.center())) return false;

        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> required = inputs.get(dir);
            List<ItemStack> actual = input.directionItems().get(dir);

            if(required.size() != actual.size()) return false;

            for (int i = 0; i < required.size(); i++) {
                Ingredient req = required.get(i);
                ItemStack act = actual.get(i);

                if(!req.test(act)) return false;
            }
        }

        Rituals.LOGGER.info("=== 配方完全匹配成功！ ===");
        return true;
    }

    public EarthAltarRecipeContext.StartScriptResult runStartScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, int processingTime, EarthAltarRecipeContext.Callback callback) {
        if (craftStartScriptUri.isEmpty()) {
            return new EarthAltarRecipeContext.StartScriptResult(false, processingTime, callback);
        }

        ResourceLocation finalScriptTarget = craftStartScriptUri.get();
        Rituals.LOGGER.debug("Loading and executing start script from DataPack URI: {}", finalScriptTarget);

        try {
            int[] ptContainer = {processingTime};
            boolean[] crContainer = {false};
            EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);
            EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();

            if (persistentContext != null) {
                Value jsBindings = persistentContext.getBindings("js");
                jsBindings.putMember("context", ctxContainer);
                jsBindings.putMember("cancelRecipe",crContainer);
                jsBindings.putMember("processingTime", ptContainer);
                jsBindings.putMember("callback", cbkContainer);

                RitualsScriptEngine.evalUriInContext(persistentContext, finalScriptTarget);
            } else {
                Map<String, Object> bindings = new java.util.HashMap<>();
                bindings.put("context", ctxContainer);
                bindings.put("cancelRecipe",crContainer);
                bindings.put("processingTime", ptContainer);
                bindings.put("callback", cbkContainer);

                RitualsScriptEngine.executeUriCached(finalScriptTarget, bindings);
            }

            return new EarthAltarRecipeContext.StartScriptResult(crContainer[0],ptContainer[0], cbkContainer.value);
        } catch (Exception e) {
            Rituals.LOGGER.error("==== RITUALS SCRIPT CRASH REPORT ====");
            Rituals.LOGGER.error("Script URI Pointer: {}", finalScriptTarget);
            Rituals.LOGGER.error("=====================================");
            Rituals.LOGGER.error("Failed to execute start script from resource", e);
        }
        return new EarthAltarRecipeContext.StartScriptResult(false,processingTime, callback);
    }

    public EarthAltarRecipeContext.FinishScriptResult runFinishScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, EarthAltarRecipeContext.Callback callback) {
        List<List<ItemStack>> consumed = new ArrayList<>();
        for (int dir = 0; dir < 8; dir++) {
            List<ItemStack> consumedDirection = new ArrayList<>();
            int i = 0;
            for (Ingredient ingredient : inputs.get(dir)) {
                if(ingredient.isCustom() && ingredient.getCustomIngredient() instanceof RitualsIngredient custom && custom.customConsumption()) {
                    consumedDirection.add(custom.consume(context.directionItems().get(dir).get(i)));
                } else {
                    consumedDirection.add(ItemStack.EMPTY);
                }
                i++;
            }
            consumed.add(consumedDirection);
        }

        if (craftFinishScriptUri.isEmpty())
            return new EarthAltarRecipeContext.FinishScriptResult(
                    context.withCenter(output.copy()).withDirectionItems(consumed), callback
            );

        ResourceLocation finalScriptTarget = craftFinishScriptUri.get();
        Rituals.LOGGER.debug("Loading and executing finish script from DataPack URI: {}", finalScriptTarget);

        try {
            EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();
            boolean[] oiContainer = {false};
            EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);

            if (persistentContext != null) {
                Value jsBindings = persistentContext.getBindings("js");
                jsBindings.putMember("context", ctxContainer);
                jsBindings.putMember("overwriteInputs",oiContainer);
                jsBindings.putMember("callback", cbkContainer);

                RitualsScriptEngine.evalUriInContext(persistentContext, finalScriptTarget);
            } else {
                Map<String, Object> bindings = new java.util.HashMap<>();
                bindings.put("context", ctxContainer);
                bindings.put("overwritesInputs",oiContainer);
                bindings.put("callback", cbkContainer);

                RitualsScriptEngine.executeUriCached(finalScriptTarget, bindings);
            }

            EarthAltarRecipeContext.FinishScriptResult scriptResult =
                    new EarthAltarRecipeContext.FinishScriptResult(
                            oiContainer[0] ?
                                    ctxContainer.value.copy() :
                                    context.withCenter(ctxContainer.value.center()).withDirectionItems(consumed),
                            cbkContainer.value
                    );
            return scriptResult;
        } catch (Exception e) {
            Rituals.LOGGER.error("==== RITUALS SCRIPT CRASH REPORT ====");
            Rituals.LOGGER.error("Script URI Pointer: {}", finalScriptTarget);
            Rituals.LOGGER.error("=====================================");
            Rituals.LOGGER.error("Failed to execute finish script from resource", e);
        }

        return new EarthAltarRecipeContext.FinishScriptResult(context.withCenter(output.copy()), callback);
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

    public static final MapCodec<EarthAltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("center").forGetter(r -> r.center),
                    Codec.list(Codec.list(Ingredient.CODEC)).fieldOf("inputs").forGetter(r -> r.inputs),
                    ItemStack.CODEC.fieldOf("output").forGetter(r -> r.output),
                    Codec.INT.fieldOf("processingTime").forGetter(r -> r.processingTime),
                    ResourceLocation.CODEC.optionalFieldOf("craftStartScript").forGetter(r -> r.craftStartScriptUri),
                    ResourceLocation.CODEC.optionalFieldOf("craftFinishScript").forGetter(r -> r.craftFinishScriptUri)
            ).apply(instance, EarthAltarRecipe::new)
    );

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

                        buf.writeBoolean(recipe.craftStartScriptUri.isPresent());
                        recipe.craftStartScriptUri.ifPresent(buf::writeResourceLocation);

                        buf.writeBoolean(recipe.craftFinishScriptUri.isPresent());
                        recipe.craftFinishScriptUri.ifPresent(buf::writeResourceLocation);
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

                        Optional<ResourceLocation> craftStartScriptUri = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
                        Optional<ResourceLocation> craftFinishScriptUri = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();

                        return new EarthAltarRecipe(center, inputs, output, time, craftStartScriptUri, craftFinishScriptUri);
                    }
            );

    public static class Type implements RecipeType<EarthAltarRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
}