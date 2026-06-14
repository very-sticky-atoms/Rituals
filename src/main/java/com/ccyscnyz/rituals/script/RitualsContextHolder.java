package com.ccyscnyz.rituals.script;

import org.graalvm.polyglot.Context;
import com.ccyscnyz.rituals.Rituals;

public class RitualsContextHolder implements AutoCloseable {
    private Context context;
    private boolean closed = false;

    private RitualsContextHolder(Context context) {
        this.context = context;
    }

    public static RitualsContextHolder create() {
        Context rawContext = RitualsScriptEngine.createPersistentContext();
        return new RitualsContextHolder(rawContext);
    }

    // 获取 GraalVM Context。如果已经关闭，则返回 null 阻止非法访问
    public Context get() {
        return this.closed ? null : this.context;
    }


    /** 因为GraalVM非常  氨  醛  ，
     *  因此如果两个地方同时、或者先后去关闭同一个原生 Context，GraalVM 会直接抛出 IllegalStateException，导致崩溃。
     *  所以最好搞一个安全销毁器。支持重复调用，不抛出异常，切断底层 Native 内存
     */
    @Override
    public void close() {
        if (!this.closed && this.context != null) {
            this.closed = true; // 挡住并发或重复关闭请求
            try {
                // cancelIfExecuting = true 强制终止内部可能死循环的 JS 脚本并物理关闭
                this.context.close(true);
            } catch (Exception e) {
                Rituals.LOGGER.warn("RitualsContextHolder closed with warning: {}", e.getMessage());
            } finally {
                this.context = null;
            }
        }
    }

    public boolean isClosed() {
        return this.closed;
    }
}