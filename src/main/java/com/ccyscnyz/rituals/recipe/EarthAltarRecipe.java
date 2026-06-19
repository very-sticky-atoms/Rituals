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
        // ---- 诊断日志开始 ----
        Rituals.LOGGER.info("=== 开始大地祭坛配方匹配检查 ===");
        Rituals.LOGGER.info("配方预期产物: {}", this.output.getItem().toString());

        // 1. 检查中心物品
        boolean centerMatch = center.test(input.center());
        Rituals.LOGGER.info("中心物品检查: 配方要求={}, 实际放入={}, 结果={}",
                Arrays.toString(center.getItems()), input.center(), centerMatch);
        if (!centerMatch) {
            Rituals.LOGGER.info("匹配失败: 中心物品不匹配");
            return false;
        }

        // 2. 检查八个方向
        String[] dirNames = {"北(0)", "东北(1)", "东(2)", "东南(3)", "南(4)", "西南(5)", "西(6)", "西北(7)"};

        for (int dir = 0; dir < 8; dir++) {
            List<Ingredient> required = inputs.get(dir);
            List<ItemStack> actual = input.directionItems().get(dir);

            int maxCheck = Math.max(required.size(), actual.size());
            Rituals.LOGGER.info("方向 {} 检查: 配方要求槽数={}, 实际世界槽数={}", dirNames[dir], required.size(), actual.size());

            for (int i = 0; i < maxCheck; i++) {
                Ingredient req = i < required.size() ? required.get(i) : Ingredient.EMPTY;
                ItemStack act = i < actual.size() ? actual.get(i) : ItemStack.EMPTY;

                // 判断配方是否期望此处为空气
                // 1.21.1 中，如果 JSON 写了空气，req.isEmpty() 为 true，或者内部包含了 description 带有 air 的项
                boolean reqExpectsEmpty = req.isEmpty() || req == Ingredient.EMPTY;
                if (!reqExpectsEmpty) {
                    // 兼容写了 {"item": "minecraft:air"} 的情况
                    for (ItemStack item : req.getItems()) {
                        if (item.is(net.minecraft.world.item.Items.AIR)) {
                            reqExpectsEmpty = true;
                            break;
                        }
                    }
                }

                Rituals.LOGGER.info("  -> 槽位 [{}]: 配方期望为空={}, 实际物品={}", i, reqExpectsEmpty, act);

                if (reqExpectsEmpty) {
                    if (!act.isEmpty()) {
                        Rituals.LOGGER.info("匹配失败: 方向 {} 槽位 [{}] 应该是空的，但实际有物品 {}", dirNames[dir], i, act);
                        return false;
                    }
                } else {
                    if (act.isEmpty() || !req.test(act)) {
                        Rituals.LOGGER.info("匹配失败: 方向 {} 槽位 [{}] 物品不匹配。期望={}, 实际={}",
                                dirNames[dir], i, Arrays.toString(req.getItems()), act);
                        return false;
                    }
                }
            }
        }

        Rituals.LOGGER.info("=== 配方完全匹配成功！ ===");
        return true;
    }

    public EarthAltarRecipeContext.StartScriptResult runStartScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, int processingTime, EarthAltarRecipeContext.Callback callback) {
        if (craftStartScriptUri.isEmpty()) {
            return new EarthAltarRecipeContext.StartScriptResult(processingTime, callback);
        }

        ResourceLocation finalScriptTarget = craftStartScriptUri.get();
        Rituals.LOGGER.debug("Loading and executing start script from DataPack URI: {}", finalScriptTarget);

        try {
            int[] ptContainer = {processingTime};
            EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);
            EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();

            if (persistentContext != null) {
                Value jsBindings = persistentContext.getBindings("js");
                jsBindings.putMember("context", ctxContainer);
                jsBindings.putMember("processingTime", ptContainer);
                jsBindings.putMember("callback", cbkContainer);

                RitualsScriptEngine.evalUriInContext(persistentContext, finalScriptTarget);
            } else {
                Map<String, Object> bindings = new java.util.HashMap<>();
                bindings.put("context", ctxContainer);
                bindings.put("processingTime", ptContainer);
                bindings.put("callback", cbkContainer);

                RitualsScriptEngine.executeUriCached(finalScriptTarget, bindings);
            }

            return new EarthAltarRecipeContext.StartScriptResult(ptContainer[0], cbkContainer.value);
        } catch (Exception e) {
            Rituals.LOGGER.error("==== RITUALS SCRIPT CRASH REPORT ====");
            Rituals.LOGGER.error("Script URI Pointer: {}", finalScriptTarget);
            Rituals.LOGGER.error("=====================================");
            Rituals.LOGGER.error("Failed to execute start script from resource", e);
        }
        return new EarthAltarRecipeContext.StartScriptResult(processingTime, callback);
    }

    public EarthAltarRecipeContext.FinishScriptResult runFinishScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, EarthAltarRecipeContext.Callback callback) {
        if (craftFinishScriptUri.isEmpty()) {
            List<List<ItemStack>> consumed = new ArrayList<>();
            for (int dir = 0; dir < 8; dir++) {
                List<ItemStack> consumedDirection = new ArrayList<>();
                for (int i = 0; i < this.inputs.get(dir).size(); i++) {
                    consumedDirection.add(ItemStack.EMPTY);
                }
                consumed.add(consumedDirection);
            }
            EarthAltarRecipeContext finalContext = context.withCenter(output.copy()).withDirectionItems(consumed);
            return new EarthAltarRecipeContext.FinishScriptResult(finalContext, callback);
        }

        ResourceLocation finalScriptTarget = craftFinishScriptUri.get();
        Rituals.LOGGER.debug("Loading and executing finish script from DataPack URI: {}", finalScriptTarget);

        try {
            EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();
            EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);

            if (persistentContext != null) {
                Value jsBindings = persistentContext.getBindings("js");
                jsBindings.putMember("context", ctxContainer);
                jsBindings.putMember("callback", cbkContainer);

                RitualsScriptEngine.evalUriInContext(persistentContext, finalScriptTarget);
            } else {
                Map<String, Object> bindings = new java.util.HashMap<>();
                bindings.put("context", ctxContainer);
                bindings.put("callback", cbkContainer);

                RitualsScriptEngine.executeUriCached(finalScriptTarget, bindings);
            }

            EarthAltarRecipeContext.FinishScriptResult scriptResult = new EarthAltarRecipeContext.FinishScriptResult(ctxContainer.value.copy(), cbkContainer.value);
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

    // 创建一个中转用的 Codec，能够同时兼容原版 Ingredient 和 rituals_air 标记
    private static final Codec<Ingredient> ADVANCED_INGREDIENT_CODEC = Codec.either(
            Codec.BOOL.fieldOf("air").codec(), // 优先匹配 {"rituals_air": true}
            Ingredient.CODEC                           // 匹配不到则走原版 Ingredient
    ).xmap(
            either -> either.map(
                    isAir -> Ingredient.EMPTY, // 如果匹配到 rituals_air，在内存中直接变成 Ingredient.EMPTY
                    ingredient -> ingredient   // 如果是原版，保持原样
            ),
            ingredient -> {
                if (ingredient.isEmpty()) {
                    // 这里只是 Getter 用，实际上序列化回去可以变成普通的或者是原版
                    return com.mojang.datafixers.util.Either.left(true);
                }
                return com.mojang.datafixers.util.Either.right(ingredient);
            }
    );

    public static final MapCodec<EarthAltarRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Ingredient.CODEC.fieldOf("center").forGetter(r -> r.center),
                    // 使用我们升级后的双重判断 Codec 来读取 inputs 列表
                    Codec.list(Codec.list(ADVANCED_INGREDIENT_CODEC)).fieldOf("inputs").forGetter(r -> r.inputs),
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