package org.example.item_retrieval.client.runtime;

/**
 * 持续检索调度器。
 * 仅负责开关和定时节奏，不负责具体检索逻辑。
 */
public final class ContinuousSearchScheduler {

    /** 轮询间隔（毫秒）。 */
    private final long intervalMs;

    /** 当前是否启用持续检索。 */
    private boolean enabled;

    /** 下次允许执行检索的时间戳（毫秒）。 */
    private long nextExecutionAtMs;

    /**
     * @param intervalMs 持续检索间隔（毫秒），最小会被钳制到 1。
     */
    public ContinuousSearchScheduler(long intervalMs) {
        this.intervalMs = Math.max(1L, intervalMs);
        this.enabled = false;
        this.nextExecutionAtMs = 0L;
    }

    /**
     * 切换持续检索状态。
     * @return 切换后的状态，true 表示开启。
     */
    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    /** 关闭持续检索并清空下一次执行时间。 */
    public void disable() {
        enabled = false;
        nextExecutionAtMs = 0L;
    }

    /** @return 是否开启持续检索。 */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 让下一次 tick 立即满足执行条件。
     * 一般在"刚开启持续检索"时调用。
     */
    public void triggerImmediateNextTick() {
        nextExecutionAtMs = 0L;
    }

    /**
     * @param nowMs 当前时间戳（毫秒）。
     * @return 当前时刻是否应该执行一次轮询检索。
     */
    public boolean shouldRunNow(long nowMs) {
        return enabled && nowMs >= nextExecutionAtMs;
    }

    /**
     * 记录"本次已经执行"，并计算下一次执行时间。
     * @param nowMs 当前时间戳（毫秒）。
     */
    public void markExecuted(long nowMs) {
        nextExecutionAtMs = nowMs + intervalMs;
    }
}
