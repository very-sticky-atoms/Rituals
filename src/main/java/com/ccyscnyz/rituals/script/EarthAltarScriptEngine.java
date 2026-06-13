package com.ccyscnyz.rituals.script;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.block.entity.RitualPillarBlockEntity;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipeOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;

import javax.script.*;
import java.util.*;
import java.util.stream.Collectors;

public class EarthAltarScriptEngine {

    private static final ScriptEngine engine;
    private static final Map<ResourceLocation, CompiledScript> scriptCache = new HashMap<>();

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine graal = manager.getEngineByName("graal.js");
        if (graal != null) {
            engine = graal;
            Rituals.LOGGER.info("EarthAltarScriptEngine: using GraalJS");
        } else {
            ScriptEngine nashorn = manager.getEngineByName("nashorn");
            if (nashorn != null) {
                engine = nashorn;
                Rituals.LOGGER.info("EarthAltarScriptEngine: using Nashorn");
            } else {
                engine = null;
                Rituals.LOGGER.error("EarthAltarScriptEngine: NO JavaScript engine found! Scripts will be ignored.");
            }
        }
    }

    public static EarthAltarRecipeOutput executeCached(ResourceLocation recipeId, String script,
                                                       Map<String, Object> bindings) throws ScriptException {
        if (engine == null) {
            Rituals.LOGGER.warn("Script engine unavailable, cannot execute script for recipe {}", recipeId);
            return null;
        }

        // 将 center 和 directions 转换为 MutableItemStack
        ItemStack centerStack = (ItemStack) bindings.get("center");
        List<List<ItemStack>> directions = (List<List<ItemStack>>) bindings.get("directions");

        Map<String, Object> effectiveBindings = new HashMap<>(bindings);
        effectiveBindings.put("center", MutableItemStack.fromItemStack(centerStack));
        effectiveBindings.put("directions", directions.stream()
                .map(list -> list.stream()
                        .map(MutableItemStack::fromItemStack)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));

        ScriptApi api = new ScriptApi();
        effectiveBindings.put("rituals", api);

        String wrappedScript = "(function() { " + script + " })()";

        CompiledScript compiled = scriptCache.get(recipeId);
        if (compiled == null) {
            if (engine instanceof Compilable compilable) {
                compiled = compilable.compile(wrappedScript);
                scriptCache.put(recipeId, compiled);
            } else {
                Bindings sb = engine.createBindings();
                sb.putAll(effectiveBindings);
                engine.eval(wrappedScript, sb);
                return buildResult(api);
            }
        }

        Bindings sb = engine.createBindings();
        sb.putAll(effectiveBindings);
        compiled.eval(sb);
        return buildResult(api);
    }

    private static EarthAltarRecipeOutput buildResult(ScriptApi api) {
        MutableItemStack outputMutable = api.getOutputOverride();
        ItemStack output = (outputMutable != null) ? outputMutable.toItemStack() : null;

        List<ScriptApi.InputModification> mods = api.getInputModifications();
        EarthAltarRecipeOutput.InputModifier inputModifier = null;
        if (!mods.isEmpty()) {
            inputModifier = (consumedItems, pillarPositions, level, pos) -> {
                for (ScriptApi.InputModification mod : mods) {
                    int dir = mod.direction();
                    int index = mod.index();
                    if (dir >= 0 && dir < pillarPositions.size()) {
                        List<BlockPos> positions = pillarPositions.get(dir);
                        if (index >= 0 && index < positions.size()) {
                            BlockPos pillarPos = positions.get(index);
                            if (level.getBlockEntity(pillarPos) instanceof RitualPillarBlockEntity pillar) {
                                ItemStack stack = mod.stack().toItemStack();
                                pillar.inventory.insertItem(0, stack, false);
                            }
                        }
                    }
                }
            };
        }
        return new EarthAltarRecipeOutput(output, inputModifier);
    }
}