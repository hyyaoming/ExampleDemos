package com.example.sampleview.taskScheduler.api

import android.content.Context

/**
 * 表示依赖 Android [Context] 的任务。
 *
 * 用于需要访问 Android 资源、文件、服务等功能的异步任务。调度器或框架可通过该接口判断任务是否需要注入上下文环境。
 *
 * 示例：读取文件、访问数据库、调用系统服务等需要 Context 的任务应实现本接口。
 *
 * @param R 任务执行结果类型
 */
interface ContextAwareTask<R> : Task<R> {
    val context: Context
}
