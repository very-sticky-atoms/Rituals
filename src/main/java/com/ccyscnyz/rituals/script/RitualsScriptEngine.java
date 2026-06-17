package com.ccyscnyz.rituals.script;

import com.ccyscnyz.rituals.Rituals;
import net.minecraft.resources.ResourceLocation;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RitualsScriptEngine {

    private static final Engine sharedEngine;
    private static final Map<ResourceLocation, Source> scriptCache = new ConcurrentHashMap<>();

    static {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Engine eng = null;
        try {
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());
            eng = Engine.newBuilder().allowExperimentalOptions(true).build();
            Rituals.LOGGER.info("RitualsScriptEngine: GraalVM Engine initialized.");
        } catch (Exception e) {
            Rituals.LOGGER.error("RitualsScriptEngine: Failed to initialize GraalVM Engine!", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        sharedEngine = eng;
    }

    // 抽取公共的 Java 类与 JS 原型链绑定逻辑
    private static void injectPrototypes(Context context) {
        Value jsBindings = context.getBindings("js");

        jsBindings.putMember("ItemStack", net.minecraft.world.item.ItemStack.class);
        jsBindings.putMember("Items", net.minecraft.world.item.Items.class);
        jsBindings.putMember("MutableComponent", net.minecraft.network.chat.MutableComponent.class); // 关键
        jsBindings.putMember("Style", net.minecraft.network.chat.Style.class);
        jsBindings.putMember("TextColor", net.minecraft.network.chat.TextColor.class);
        jsBindings.putMember("ChatFormatting", net.minecraft.ChatFormatting.class);
        jsBindings.putMember("CompoundTag", net.minecraft.nbt.CompoundTag.class);
        jsBindings.putMember("DataComponents", net.minecraft.core.component.DataComponents.class);
        jsBindings.putMember("CustomData", net.minecraft.world.item.component.CustomData.class);
        jsBindings.putMember("Enchantments", net.minecraft.world.item.enchantment.ItemEnchantments.class);
        jsBindings.putMember("Unbreakable", net.minecraft.world.item.component.Unbreakable.class);

        context.eval("js",
                "globalThis.Item = {" +
                        "    of: function(itemId, count) {" +
                        "        return com.ccyscnyz.rituals.script.ScriptItemUtils.getItem(itemId, count || 1);" +
                        "    }" +
                        "};"
        );

        context.eval("js",
                "globalThis.Utils = {" +
                        "    setComponent: function(stack, id, val) {" +
                        "        var regs = (typeof context !== 'undefined' && context.value && context.value.level) " +
                        "                   ? context.value.level().registryAccess() " +
                        "                   : net.minecraft.core.RegistryAccess.EMPTY;" +
                        "        com.ccyscnyz.rituals.script.ScriptItemUtils.setComponent(stack, id, val, regs);" +
                        "        return stack;" +
                        "    }," +
                        "    getComponent: function(stack, id) {" +
                        "        return com.ccyscnyz.rituals.script.ScriptItemUtils.getComponent(stack, id);" +
                        "    }," +
                        "    mergeCustomData: function(stack, tag) {" +
                        "        com.ccyscnyz.rituals.script.ScriptItemUtils.mergeCustomData(stack, tag);" +
                        "        return stack;" +
                        "    }" +
                        "};"
        );
    }

    public static Object executeCached(ResourceLocation scriptSource, String script,
                                       Map<String, Object> bindings) throws Exception {
        if (sharedEngine == null) return null;

        Rituals.LOGGER.warn("RitualsScriptEngine: Rituals Script [{}] is falling back to an ephemeral context (one-time sandbox)! " +
                "Prototypes will be re-injected dynamically. Please ensure ritualContext is active.", scriptSource);

        String wrappedScript = "(function() { " + script + " })()";
        Source source = scriptCache.computeIfAbsent(scriptSource, id -> {
            try {
                return Source.newBuilder("js", wrappedScript, id.toString()).build();
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to build GraalJS Source for " + id, e);
            }
        });

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            //一次性沙箱
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());

            Rituals.LOGGER.warn("RitualsScriptEngine: .");

            HostAccess customHostAccess = HostAccess.newBuilder(HostAccess.ALL)
                    .targetTypeMapping(Double.class, Object.class, (v) -> true, (v) -> v)
                    .build();

            try (Context context = Context.newBuilder("js")
                    .engine(sharedEngine)
                    .allowHostAccess(customHostAccess)
                    .allowHostClassLookup(className -> true)
                    .hostClassLoader(RitualsScriptEngine.class.getClassLoader())
                    .build()) {

                // 让一次性沙箱同样拥有相同的全局环境与组件操作能力
                injectPrototypes(context);

                Value jsBindings = context.getBindings("js");
                bindings.forEach(jsBindings::putMember);

                Value result = context.eval(source);
                if (result == null || result.isNull()) return null;
                return result.isHostObject() ? result.asHostObject() : result;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public static Context createPersistentContext() {
        if (sharedEngine == null) return null;

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());

            Context context = Context.newBuilder("js")
                    .engine(sharedEngine)
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .hostClassLoader(RitualsScriptEngine.class.getClassLoader())
                    .build();

            // 保持常驻沙箱的功能完整
            injectPrototypes(context);
            return context;
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public static Value evalInContext(Context context, ResourceLocation scriptSource, String script) {
        String wrappedScript = "(function() { " + script + " })()";
        Source source = scriptCache.computeIfAbsent(scriptSource, id -> {
            try {
                return Source.newBuilder("js", wrappedScript, id.toString()).build();
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to build GraalJS Source for " + id, e);
            }
        });

        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());
            return context.eval(source);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    public static void warmup() {
        Thread warmupThread = new Thread(() -> {
            Rituals.LOGGER.info("RitualsScriptEngine: Starting asynchronous warmup...");
            long startTime = System.currentTimeMillis();
            try {
                Context dummyContext = createPersistentContext();
                if (dummyContext != null) {
                    dummyContext.eval("js", "var a = 1 + 1; Item.of('minecraft:air');");
                    dummyContext.close(true);
                    long duration = System.currentTimeMillis() - startTime;
                    Rituals.LOGGER.info("RitualsScriptEngine: Warmup completed in {}ms. Ready for lag-free crafting!", duration);
                }
            } catch (Exception e) {
                Rituals.LOGGER.warn("RitualsScriptEngine: Warmup encountered an issue", e);
            }
        });
        warmupThread.setName("Rituals-Script-Warmup");
        warmupThread.setDaemon(true);
        warmupThread.setPriority(Thread.MIN_PRIORITY);
        warmupThread.start();
    }
}