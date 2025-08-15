package com.example.sampleview.taskScheduler.model

import com.example.sampleview.taskScheduler.api.Task

/**
 * 任务调度结果，封装任务调度执行完成后的整体信息。
 *
 * 核心数据：
 * - totalTimeMillis：整个调度过程的总耗时，单位毫秒，方便性能统计和分析。
 * - taskExceptions：任务执行过程中发生的异常集合，Key为任务对象，Value为对应的异常信息。
 *
 * 说明：
 * - totalTimeMillis 反映从开始执行到所有任务完成（或取消）的时间。
 * - taskExceptions 用于记录执行失败或异常的任务，便于后续错误处理或重试机制。
 * - 若任务全部成功执行，则 taskExceptions 为空。
 *
 * @property totalTimeMillis 任务调度执行总耗时（单位：毫秒）。
 * @property taskExceptions 任务执行异常映射，包含所有执行失败的任务及其异常。
 */
data class TaskSchedulerResult(
    val totalTimeMillis: Long, val taskExceptions: Map<Task<*>, Throwable>
)