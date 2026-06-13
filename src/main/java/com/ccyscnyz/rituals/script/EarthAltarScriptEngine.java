package com.ccyscnyz.rituals.script;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;

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

    /**
     * 执行脚本并返回结果 ItemStack，或 null 使用默认产物。
     */
    public static ItemStack executeCached(ResourceLocation recipeId, String script,
                                          Map<String, Object> bindings) throws ScriptException {
        if (engine == null) {
            Rituals.LOGGER.warn("Script engine unavailable, cannot execute script for recipe {}", recipeId);
            return null;
        }

        // 包装脚本以支持 return 语句
        String wrappedScript = "(function() { " + script + " })()";
        Rituals.LOGGER.debug("Wrapped script: {}", wrappedScript);

        CompiledScript compiled = scriptCache.get(recipeId);
        if (compiled == null) {
            if (engine instanceof Compilable compilable) {
                compiled = compilable.compile(wrappedScript);
                scriptCache.put(recipeId, compiled);
            } else {
                // 引擎不支持编译，直接eval
                Bindings scriptBindings = engine.createBindings();
                scriptBindings.putAll(bindings);
                Object result = engine.eval(wrappedScript, scriptBindings);
                return parseResult(result);
            }
        }

        Bindings scriptBindings = engine.createBindings();
        scriptBindings.putAll(bindings);
        Object result = compiled.eval(scriptBindings);
        return parseResult(result);
    }

    private static ItemStack parseResult(Object result) {
        if (result instanceof String itemStr) {
            ResourceLocation itemId = ResourceLocation.tryParse(itemStr);
            if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                return new ItemStack(BuiltInRegistries.ITEM.get(itemId));
            }
        }
        return null;
    }
}