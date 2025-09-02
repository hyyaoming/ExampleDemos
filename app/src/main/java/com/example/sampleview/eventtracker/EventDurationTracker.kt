package com.example.sampleview.eventtracker

import android.os.SystemClock
import com.example.sampleview.AppActivityManager
import com.example.sampleview.AppStatusListener
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件耗时跟踪器，用于埋点统计。
 *
 * 特性：
 * - 支持多事件同时计时
 * - 自动过滤应用进入后台的时间
 * - 线程安全
 * - 可注册回调处理事件结束（例如上报埋点）
 */
object EventDurationTracker : AppStatusListener {
    const val ID_COLD_LAUNCH_TIME = "coldLaunchTime"

    init {
        AppActivityManager.registerAppStatusListener(this)
    }

    /** 事件ID -> Timer 对象，线程安全 */
    private val timers = ConcurrentHashMap<String, Timer>()

    /**
     * 标记冷启动时间。
     * 用于计算基于冷启动的事件耗时。
     */
    fun markColdStart() {
        start(ID_COLD_LAUNCH_TIME)
    }

    /**
     * 开始计时。
     *
     * @param eventId 事件ID，用于区分不同事件
     */
    fun start(eventId: String) {
        val now = SystemClock.elapsedRealtime()
        timers[eventId] = Timer(now)
    }

    /**
     * 停止计时并返回耗时（普通事件）。
     *
     * @param eventId   事件ID
     * @param resetOnly 是否仅重置开始时间而不移除计时器
     * @return          耗时（毫秒），如果事件未开始返回0
     */
    fun stop(eventId: String, resetOnly: Boolean = false): Long {
        val now = SystemClock.elapsedRealtime()
        val timer = timers[eventId] ?: return 0L
        return if (resetOnly) {
            timer.settleAndRestart(now)
        } else {
            val elapsed = timer.settle(now)
            timers.remove(eventId)
            elapsed
        }
    }

    /**
     * 停止计时并返回耗时。
     * 优先使用子事件自身的 Timer，如果子事件不存在，则基于冷启动 Timer 计算耗时。
     * 可选择是否重置开始时间（resetOnly）。
     *
     * @param eventId   事件ID，用于区分不同事件
     * @param resetOnly 是否仅重置开始时间而不移除计时器，默认 false
     *                  - true: 返回当前耗时，同时将 Timer 重置为新起点，方便下一段计时
     *                  - false: 返回当前耗时，同时移除 Timer，不再计时
     * @return 从起始时间（子事件 Timer 或冷启动 Timer）到当前的耗时
     */
    fun stopOrFromCold(eventId: String, resetOnly: Boolean = false): Long {
        // 获取当前时间，所有计算基于这个时间
        val now = SystemClock.elapsedRealtime()

        // 尝试获取子事件 Timer
        val timer = timers[eventId]
        return if (timer != null) {
            // 子事件 Timer 存在
            if (resetOnly) {
                // resetOnly = true
                // 结算当前耗时并重置 Timer 起点，使下一段计时从 now 开始
                timer.settleAndRestart(now)
            } else {
                // resetOnly = false
                // 结算当前耗时并移除 Timer，不再继续计时
                val elapsed = timer.settle(now)
                timers.remove(eventId)
                elapsed
            }
        } else {
            // 子事件 Timer 不存在
            // 尝试使用冷启动 Timer 计算耗时
            val coldTimer = timers[ID_COLD_LAUNCH_TIME]
            val elapsed = coldTimer?.elapsedNow(now) ?: 0L

            if (resetOnly) {
                // resetOnly = true 时，需要为子事件创建一个新的 Timer
                // 这样下次 stopOrFromCold 调用时可以继续计时
                timers[eventId] = Timer(now)
            }
            // 返回耗时（基于冷启动或 0）
            elapsed
        }
    }

    /**
     * 获取当前计时耗时（毫秒）。
     *
     * @param eventId 事件ID
     * @return 当前耗时，如果事件未开始返回 0
     */
    fun elapsed(eventId: String): Long {
        return timers[eventId]?.elapsedNow() ?: 0L
    }

    /**
     * 取消计时。
     *
     * @param eventId 事件ID
     */
    fun cancel(eventId: String) {
        timers.remove(eventId)
    }

    /**
     * 判断事件是否正在计时。
     *
     * @param eventId 事件ID
     * @return true 表示正在计时，false 表示未计时
     */
    fun isRunning(eventId: String): Boolean = timers.containsKey(eventId)

    /**
     * 应用回到前台，恢复所有计时。
     * 自动过滤后台时间。
     */
    override fun onForeground() {
        val now = SystemClock.elapsedRealtime()
        timers.values.forEach { it.resume(now) }
    }

    /**
     * 应用进入后台，暂停所有计时。
     * 后台时间不会计入耗时。
     */
    override fun onBackground() {
        val now = SystemClock.elapsedRealtime()
        timers.values.forEach { it.pause(now) }
    }

    /**
     * 内部 Timer 类，封装前后台暂停/恢复逻辑。
     *
     * @property startTime 开始时间
     */
    private class Timer(private var startTime: Long) {
        /** 累计的前台有效时间（毫秒），在暂停时累加 */
        private var accumulated: Long = 0

        /** 是否正在计时（true = 前台计时中，false = 暂停/后台/结算） */
        private var running: Boolean = true

        /** 内部锁对象，用于保证 Timer 方法的线程安全，防止并发访问导致计时不准确 */
        private val lock = Any()

        /** 实时获取当前总耗时（不会修改状态） */
        fun elapsedNow(now: Long = SystemClock.elapsedRealtime()): Long = synchronized(lock) {
            return accumulated + if (running) now - startTime else 0L
        }

        /** 结算一次耗时并停止（固定值，不会再变） */
        fun settle(now: Long): Long = synchronized(lock) {
            if (running) {
                accumulated += now - startTime
                running = false
            }
            return accumulated
        }

        /** 结算一次耗时并立即作为新起点（用于 stop(resetOnly=true)） */
        fun settleAndRestart(now: Long): Long = synchronized(lock) {
            val elapsed = settle(now)
            startTime = now
            accumulated = 0
            running = true
            return elapsed
        }

        /** 暂停计时（例如应用进入后台） */
        fun pause(now: Long) = synchronized(lock) {
            if (running) {
                accumulated += now - startTime
                running = false
            }
        }

        /** 恢复计时（例如应用回到前台） */
        fun resume(now: Long) = synchronized(lock) {
            if (!running) {
                startTime = now
                running = true
            }
        }
    }
}
