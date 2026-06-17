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
    private final Optional<String> craftStartScript;
    private final Optional<String> craftFinishScript;

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

    // 重载方法：支持在传入的常驻沙箱（Persistent Context）中运行启动脚本
    public EarthAltarRecipeContext.StartScriptResult runStartScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, int processingTime, EarthAltarRecipeContext.Callback callback) {
        if (craftStartScript.isPresent() && !craftStartScript.get().isEmpty()) {
            Rituals.LOGGER.debug("Executing start script {}: {}", scriptSource, craftStartScript.get());
            try {
                int[] ptContainer = {processingTime};
                EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);

                // Start 阶段也统一使用复制后的 Container 包装结构！
                EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();

                if (persistentContext != null) {
                    // 如果传入了常驻上下文，直接在其中绑定并执行
                    Value jsBindings = persistentContext.getBindings("js");
                    // 变量名规定为 "context"
                    jsBindings.putMember("context", ctxContainer);
                    jsBindings.putMember("processingTime", ptContainer);
                    jsBindings.putMember("callback", cbkContainer);

                    // 使用底层封装的安全评估，防止线程上下文丢失
                    RitualsScriptEngine.evalInContext(persistentContext, scriptSource, craftStartScript.get());
                } else {
                    // 降级使用一次性沙箱（兼容不常驻的普通脚本）
                    Map<String, Object> bindings = new java.util.HashMap<>();
                    // 变量名统一为 "context"
                    bindings.put("context", ctxContainer);
                    bindings.put("processingTime", ptContainer);
                    bindings.put("callback", cbkContainer);
                    RitualsScriptEngine.executeCached(scriptSource, craftStartScript.get(), bindings);
                }

                // 如果 start 脚本中修改了 context.value 里的不可变对象，也能被正确捕获返回
                return new EarthAltarRecipeContext.StartScriptResult(ptContainer[0], cbkContainer.value);
            } catch (Exception e) {
                Rituals.LOGGER.error("==== RITUALS SCRIPT CRASH REPORT ====");
                Rituals.LOGGER.error("Script URI: {}", scriptSource);
                Rituals.LOGGER.error("Script Content In Memory:\n{}", craftStartScript.orElse("EMPTY"));
                Rituals.LOGGER.error("Is Persistent Context Null?: {}", (persistentContext == null));
                Rituals.LOGGER.error("Actual Center Item Class: {}", (context.center() != null ? context.center().getClass().getName() : "null"));
                Rituals.LOGGER.error("=====================================");
                Rituals.LOGGER.error("Failed to execute start script", e);
            }
        }
        return new EarthAltarRecipeContext.StartScriptResult(processingTime, callback);
    }

    // 获取最终产物（在传入的常驻沙箱中运行结束脚本）
    public EarthAltarRecipeContext.FinishScriptResult runFinishScript(Context persistentContext, ResourceLocation scriptSource, EarthAltarRecipeContext context, EarthAltarRecipeContext.Callback callback) {
        if (craftFinishScript.isPresent() && !craftFinishScript.get().isEmpty()) {
            Rituals.LOGGER.debug("Executing finish script {}: {}", scriptSource, craftFinishScript.get());
            try {
                EarthAltarRecipeContext.Container ctxContainer = context.copy().wrap();
                EarthAltarRecipeContext.CallbackContainer cbkContainer = new EarthAltarRecipeContext.CallbackContainer(callback);

                if (persistentContext != null) {
                    Value jsBindings = persistentContext.getBindings("js");
                    // 变量名在持久上下文中也是 "context"
                    jsBindings.putMember("context", ctxContainer);
                    jsBindings.putMember("callback", cbkContainer);

                    RitualsScriptEngine.evalInContext(persistentContext, scriptSource, craftFinishScript.get());
                } else {
                    Map<String, Object> bindings = new java.util.HashMap<>();
                    bindings.put("context", ctxContainer);
                    bindings.put("callback", cbkContainer);
                    RitualsScriptEngine.executeCached(scriptSource, craftFinishScript.get(), bindings);
                }

                EarthAltarRecipeContext.FinishScriptResult scriptResult = new EarthAltarRecipeContext.FinishScriptResult(ctxContainer.value.copy(), cbkContainer.value);
                Rituals.LOGGER.debug("Script returned: {}", scriptResult);
                return scriptResult;
            } catch (Exception e) {
                Rituals.LOGGER.error("==== RITUALS SCRIPT CRASH REPORT ====");
                Rituals.LOGGER.error("Script URI: {}", scriptSource);
                Rituals.LOGGER.error("Script Content In Memory:\n{}", craftFinishScript.orElse("EMPTY"));
                Rituals.LOGGER.error("Is Persistent Context Null?: {}", (persistentContext == null));
                Rituals.LOGGER.error("Actual Center Item Class: {}", (context.center() != null ? context.center().getClass().getName() : "null"));
                Rituals.LOGGER.error("=====================================");
                Rituals.LOGGER.error("Failed to execute finish script", e);
            }
        }

        List<List<ItemStack>> consumed = new ArrayList<>();
        for (int dir = 0; dir < 8; dir++) {
            List<ItemStack> consumedDirection = new ArrayList<>();
            for (int i = 0; i < this.inputs.get(dir).size(); i++) {
                consumedDirection.add(ItemStack.EMPTY);
            }
            consumed.add(consumedDirection);
        }
        // 当没有脚本时，回退调用新的显式方法名防止编译或运行期错误
        EarthAltarRecipeContext finalContext = context.withCenter(output.copy()).withDirectionItems(consumed);
        return new EarthAltarRecipeContext.FinishScriptResult(finalContext, callback);
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
                        Optional<String> craftStartScript = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
                        Optional<String> craftFinishScript = buf.readBoolean() ? Optional.of(buf.readUtf()) : Optional.empty();
                        return new EarthAltarRecipe(center, inputs, output, time, craftStartScript, craftFinishScript);
                    }
            );

    public static class Type implements RecipeType<EarthAltarRecipe> {
        public static final Type INSTANCE = new Type();
        private Type() {}
    }
}