package com.example.sampleview.taskScheduler.impl

import com.example.sampleview.taskScheduler.api.RetryStrategy
import kotlin.math.pow

/**
 * 默认的重试策略实现，采用最多 3 次重试 + 指数退避。
 *
 * 使用指数退避策略：重试延迟为 BASE_DELAY_MS * 2^(attempt - 1)
 * 例如 BASE_DELAY_MS = 1000ms 时：
 *  - 第 1 次重试：100ms
 *  - 第 2 次重试：200ms
 *  - 第 3 次重试：400ms
 */
class DefaultRetryStrategy : RetryStrategy {

    /**
     * 判断是否允许继续重试。
     *
     * @param throwable 失败时抛出的异常对象，供策略判断异常类型（本策略未使用该参数区分异常）
     * @return true 表示仍允许重试，false 表示重试次数已达上限，不再重试
     */
    override fun shouldRetry(throwable: Throwable): Boolean {
        return true
    }

    /**
     * 最大重试次数
     *
     * @param Int
     */
    override val maxRetryCount: Int = RetryStrategy.Companion.MAX_RETRY

    /**
     * 获取当前重试尝试对应的延迟时间，单位毫秒。
     * 延迟时间采用指数退避算法，计算方式为：
     * delay = baseDelayTime() * 2^(attempt - 1)
     *
     * 例如，baseDelayTime() = 1000ms，attempt = 2，则 delay = 2000ms。
     *
     * @param attempt 当前重试尝试次数，从1开始计数
     * @return 本次重试应等待的时间（毫秒）
     */
    override fun getRetryDelay(attempt: Int): Long {
        return baseDelayTime() * (2.0.pow(attempt - 1)).toLong()
    }

    /**
     * 获取基础重试延迟时间，单位毫秒。
     * 该方法分离出来，方便子类覆盖该方法实现动态或不同的基础延迟时间。
     *
     * @return 基础延迟时长（毫秒）
     */
    override fun baseDelayTime(): Long {
        return RetryStrategy.Companion.BASE_DELAY_MS
    }
}