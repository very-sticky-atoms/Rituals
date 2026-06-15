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

    // 1. 全局唯一的重量级引擎，负责缓存编译后的 JS 代码和底层运行环境
    private static final Engine sharedEngine;

    // 2. 缓存编译后的 Source 对象（使用线程安全的 ConcurrentHashMap）
    private static final Map<ResourceLocation, Source> scriptCache = new ConcurrentHashMap<>();

    static {
        // ---- 重定向类加载器，防止 ModLauncher 拦截导致找不到 JS 语言(过于安全所导致的) ----
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        Engine eng = null;
        try {
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());

            // 检查当前 JVM 是否真正启用了 JVMCI 编译器
            String jvmciCompiler = System.getProperty("jvmci.Compiler");
            boolean isJVMCIActive = jvmciCompiler != null && !jvmciCompiler.isEmpty();

            if (isJVMCIActive) {
                Rituals.LOGGER.info("RitualsScriptEngine: JVMCI is ACTIVE. Script performance will be optimized by Graal JIT");
            } else {
                Rituals.LOGGER.warn("========================================================================");
                Rituals.LOGGER.warn("RitualsScriptEngine: JVMCI is NOT enabled or not running on GraalVM JDK.");
                Rituals.LOGGER.warn("Scripts will run in INTERPRETER mode (slower performance).");
                Rituals.LOGGER.warn("To unlock full performance, please add these JVM flags to your launcher:");
                Rituals.LOGGER.warn("  -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler");
                Rituals.LOGGER.warn("========================================================================");
            }

            // 初始化全局引擎
            eng = Engine.newBuilder()
                    .allowExperimentalOptions(true)// 当某个配方的 Source 被覆盖或不再使用时，允许底层垃圾回收
                    .build();
            Rituals.LOGGER.info("RitualsScriptEngine: GraalVM Engine initialized.");
        } catch (Exception e) {
            Rituals.LOGGER.error("RitualsScriptEngine: Failed to initialize GraalVM Engine!", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
        sharedEngine = eng;
    }

    // 一次性安全执行脚本
    public static Object executeCached(ResourceLocation scriptSource, String script,
                                       Map<String, Object> bindings) throws Exception {
        if (sharedEngine == null) {
            Rituals.LOGGER.error("RitualsScriptEngine: Script engine unavailable, cannot execute script for recipe {}", scriptSource);
            return null;
        }

        // 包装成自执行函数，防止变量污染
        String wrappedScript = "(function() { " + script + " })()";

        // 从缓存获取或编译 Source
        Source source = scriptCache.computeIfAbsent(scriptSource, id -> {
            try {
                return Source.newBuilder("js", wrappedScript, id.toString()).build();
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to build GraalJS Source for " + id, e);
            }
        });

        // 为每次执行的轻量沙箱 Context 同样重定向类加载器
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(RitualsScriptEngine.class.getClassLoader());

            try (Context context = Context.newBuilder("js")
                    .engine(sharedEngine)
                    // 允许完全的宿主访问权限
                    .allowHostAccess(HostAccess.ALL)
                    .allowHostClassLookup(className -> true)
                    .hostClassLoader(RitualsScriptEngine.class.getClassLoader())
                    // 开启目标类型自动映射与消歧义
                    .allowHostAccess(HostAccess.newBuilder(HostAccess.ALL)
                            // 允许 JavaScript 的数值映射到 Java 的各类基本数字/布尔类型，极大提升重载方法匹配成功率
                            .targetTypeMapping(
                                    Double.class,
                                    Object.class,
                                    (v) -> true,
                                    (v) -> v
                            )
                            .build())
                    .build()) {

                // 获取当前独立沙盒的 bindings
                Value jsBindings = context.getBindings("js");

                // 注入本次配方所需的变量
                bindings.forEach(jsBindings::putMember);

                // 执行脚本
                Value result = context.eval(source);

                // 返回结果转换
                if (result == null || result.isNull()) {
                    return null;
                }
                return result.isHostObject() ? result.asHostObject() : result;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    // 允许创建一个常驻沙箱
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

            Value jsBindings = context.getBindings("js");
            jsBindings.putMember("ItemStack", net.minecraft.world.item.ItemStack.class);
            jsBindings.putMember("Items", net.minecraft.world.item.Items.class);
            jsBindings.putMember("Component", net.minecraft.network.chat.Component.class);
            jsBindings.putMember("CompoundTag", net.minecraft.nbt.CompoundTag.class);
            jsBindings.putMember("DataComponents", net.minecraft.core.component.DataComponents.class);
            jsBindings.putMember("CustomData", net.minecraft.world.item.component.CustomData.class);

            // 注入 JS 原型链
            context.eval("js",
                    // 给 ItemStack 原型链扩充三个方法
                    "if (typeof ItemStack !== 'undefined' && ItemStack.prototype) {" +
                            "    ItemStack.prototype.setComponent = function(id, val) {" +
                            "        com.ccyscnyz.rituals.script.ScriptItemUtils.setComponent(this, id, val);" +
                            "        return this;" + // 支持链式流式编程
                            "    };" +
                            "    ItemStack.prototype.getComponent = function(id) {" +
                            "        return com.ccyscnyz.rituals.script.ScriptItemUtils.getComponent(this, id);" +
                            "    };" +
                            "    ItemStack.prototype.mergeCustomData = function(tag) {" +
                            "        com.ccyscnyz.rituals.script.ScriptItemUtils.mergeCustomData(this, tag);" +
                            "        return this;" +
                            "    };" +
                            "}" +

                            // 创造全局 Item.of() 快速物品生成器
                            "globalThis.Item = {" +
                            "    of: function(itemId, count) {" +
                            "        return com.ccyscnyz.rituals.script.ScriptItemUtils.getItem(itemId, count || 1);" +
                            "    }" +
                            "};"
            );

            return context;
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    // 将预编译好的脚本直接放入指定沙箱执行，供常驻沙箱使用
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
        // 起一个低优先级的守护线程去跑，绝对不卡主线程
        Thread warmupThread = new Thread(() -> {
            Rituals.LOGGER.info("RitualsScriptEngine: Starting asynchronous warmup...");
            long startTime = System.currentTimeMillis();
            try {
                // 盲跑一次完整的创建、Bindings 注入和 eval 流程
                Context dummyContext = createPersistentContext();
                if (dummyContext != null) {
                    // 随便跑一行代码， 让GraalVM 把 JS 运行时和原型链全部初始化完
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