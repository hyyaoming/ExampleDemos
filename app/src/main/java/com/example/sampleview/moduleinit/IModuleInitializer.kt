package com.example.sampleview.moduleinit

import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.time.Duration

interface IModuleInitializer {
    val name: String                         // 模块名，唯一标识
    val dependencies: List<String>          // 依赖模块名列表
    val processName: String?                 // 允许初始化的进程名，null 表示全部进程
    val dispatcher: CoroutineDispatcher     // 初始化执行的调度器
    val timeout: Duration                    // 初始化超时时间

    suspend fun init(context: Context)     // 初始化方法，挂起函数
}
