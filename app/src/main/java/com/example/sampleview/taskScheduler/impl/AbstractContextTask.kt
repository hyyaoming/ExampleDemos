package com.example.sampleview.taskScheduler.impl

import android.content.Context
import com.example.sampleview.taskScheduler.api.ContextAwareTask

/**
 * Android 上下文感知型任务的基础抽象类。
 *
 * 用于需要访问 Android Context（如：文件、资源、服务等）的任务实现。
 * 它继承了 [AbstractTask]，并实现了 [ContextAwareTask] 接口，统一 Context 注入方式。
 *
 * 可通过继承此类，快速构建依赖 Android 环境的任务。
 *
 * @param R 执行结果的类型
 * @property context Android 上下文对象，通常为 ApplicationContext
 */
abstract class AbstractContextTask<R>(
    override val context: Context
) : AbstractTask<R>(), ContextAwareTask<R>
