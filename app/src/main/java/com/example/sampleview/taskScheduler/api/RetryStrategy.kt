package com.example.sampleview.taskScheduler.api

/**
 * 重试策略接口，定义任务失败后是否重试及重试延迟时长的规则。
 *
 * 实现类可根据业务需求定制重试逻辑，如最大重试次数、异常类型判断、
 * 以及不同的重试延迟策略（固定延迟、指数退避等）。
 */
interface RetryStrategy {

    /**
     * 判断是否需要进行重试。
     *
     * @param throwable 任务执行失败时抛出的异常对象，可用于判断异常类型是否允许重试。
     * @return 返回 true 表示允许继续重试，false 表示不再重试。
     */
    fun shouldRetry(throwable: Throwable): Boolean

    /**
     * 最大重试次数
     *
     * @param Int
     */
    val maxRetryCount: Int

    /**
     * 获取当前重试尝试对应的等待时长（毫秒）。
     *
     * 用于控制任务失败后等待多久再进行下一次重试，
     * 不同实现可采用固定延迟或指数退避等策略。
     *
     * @param attempt 当前重试尝试次数（从1开始）
     * @return 返回等待的毫秒数，任务调度器会在该时间后重试任务。
     */
    fun getRetryDelay(attempt: Int): Long

    /**
     * 获取基础延迟时间（毫秒），
     * 用于重试延迟计算的基准时间，便于子类覆盖实现不同的延迟策略。
     *
     * @return 返回基础延迟时长（毫秒）
     */
    fun baseDelayTime(): Long

    companion object {
        /**
         * 最大重试次数，表示最多允许重试 3 次。
         * 实际总尝试次数 = 1（初次执行）+ 3（重试）= 4 次。
         * 这个值限制了最大允许的重试次数。
         */
        const val MAX_RETRY = 3

        /**
         * 初始重试延迟时间（毫秒），用于计算指数退避的基准时间。
         * 该时间是第1次重试等待的时长，后续重试按指数倍增。
         * 根据具体业务需求，可调整该值以控制重试节奏。
         */
        const val BASE_DELAY_MS = 100L
    }
}