package com.example.sampleview.moduleinit

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

abstract class BaseModuleInitializer : IModuleInitializer {

    @Volatile
    var initialized = false               // 是否已完成初始化
        private set

    private val mutex = Mutex()           // 保证初始化只执行一次的互斥锁

    var startTime: Long = 0L              // 开始时间
    var endTime: Long = 0L                // 结束时间
    var throwable: Throwable? = null      // 初始化异常（如果有）

    // 默认调度器和超时，可在具体模块重写
    override val dispatcher: CoroutineDispatcher get() = Dispatchers.Default
    override val timeout: Duration get() = 10.toDuration(DurationUnit.SECONDS)

    /**
     * 挂起函数，等待依赖模块初始化完成后再初始化自身
     */
    suspend fun awaitInit(context: Context, moduleMap: Map<String, IModuleInitializer>) {
        if (initialized) return

        mutex.withLock {
            if (initialized) return

            // 先等待依赖模块初始化完成
            for (depName in dependencies) {
                val dep = moduleMap[depName] ?: error("Missing dependency: $depName")
                if (dep is BaseModuleInitializer) {
                    dep.awaitInit(context, moduleMap)
                } else {
                    error("Module $depName must extend BaseModuleInitializer")
                }
            }

            startTime = System.currentTimeMillis()
            try {
                withTimeout(timeout) {
                    withContext(dispatcher) {
                        init(context)
                    }
                }
                initialized = true
                Log.i("ModuleInit", "Module $name initialized in ${endTime - startTime} ms")
            } catch (t: Throwable) {
                throwable = t
                Log.e("ModuleInit", "Module $name init failed", t)
                throw t
            } finally {
                endTime = System.currentTimeMillis()
            }
        }
    }
}