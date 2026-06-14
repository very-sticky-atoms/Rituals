package com.ccyscnyz.rituals.script;

import com.ccyscnyz.rituals.Rituals;
import com.ccyscnyz.rituals.recipe.EarthAltarRecipeContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;

import javax.script.*;
import java.util.HashMap;
import java.util.Map;

public class RitualsScriptEngine {


    private static final ScriptEngine engine;
    private static final Map<ResourceLocation, CompiledScript> scriptCache = new HashMap<>();

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine graal = manager.getEngineByName("graal.js");
        if (graal != null) {
            engine = graal;
            Rituals.LOGGER.info("RitualsScriptEngine: using GraalJS");
        } else {
            ScriptEngine nashorn = manager.getEngineByName("nashorn");
            if (nashorn != null) {
                engine = nashorn;
                Rituals.LOGGER.info("RitualsScriptEngine: using Nashorn");
            } else {
                engine = null;
                Rituals.LOGGER.error("RitualsScriptEngine: NO JavaScript engine found! Scripts will be ignored.");
            }
        }
    }


    public static Object executeCached(ResourceLocation scriptSource, String script,
                                                        Map<String, Object> bindings) throws ScriptException {
        if (engine == null) {
            Rituals.LOGGER.warn("Script engine unavailable, cannot execute script for recipe {}", scriptSource);
            return null;
        }

        // 包装脚本以支持 return 语句
        String wrappedScript = "(function() { " + script + " })()";
        Rituals.LOGGER.debug("Wrapped script: {}", wrappedScript);

        CompiledScript compiled = scriptCache.get(scriptSource);
        if (compiled == null) {
            if (engine instanceof Compilable compilable) {
                compiled = compilable.compile(wrappedScript);
                scriptCache.put(scriptSource, compiled);
            } else {
                // 引擎不支持编译，直接eval
                Bindings scriptBindings = engine.createBindings();
                scriptBindings.putAll(bindings);
                return engine.eval(wrappedScript, scriptBindings);
            }
        }

        Bindings scriptBindings = engine.createBindings();
        scriptBindings.putAll(bindings);
        return compiled.eval(scriptBindings);
    }
}