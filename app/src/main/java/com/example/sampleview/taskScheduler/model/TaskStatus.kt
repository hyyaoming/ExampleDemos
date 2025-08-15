package com.example.sampleview.taskScheduler.model

/**
 * 任务状态枚举，表示任务在生命周期中的各个阶段状态。
 *
 * - PENDING: 任务已创建，但尚未开始执行。
 * - RUNNING: 任务正在执行中。
 * - SUCCESS: 任务成功完成。
 * - FAILED: 任务执行失败，且不再重试。
 * - RETRYING: 任务执行失败，正在等待重试。
 * - CANCELED: 任务被取消执行。
 */
enum class TaskStatus {
    /** 任务等待执行 */
    PENDING,

    /** 任务正在执行 */
    RUNNING,

    /** 任务执行成功 */
    SUCCESS,

    /** 任务执行失败且不会重试 */
    FAILED,

    /** 任务执行失败但准备重试 */
    RETRYING,

    /** 任务被取消 */
    CANCELED
}