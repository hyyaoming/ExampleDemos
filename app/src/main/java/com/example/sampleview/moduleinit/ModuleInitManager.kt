package com.example.sampleview.moduleinit

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object ModuleInitManager {
    private val moduleMap = mutableMapOf<String, IModuleInitializer>()

    /**
     * 注册模块，重复注册会输出警告并忽略
     */
    fun register(vararg modules: IModuleInitializer) {
        modules.forEach {
            if (moduleMap.containsKey(it.name)) {
                Log.w("ModuleInit", "Module ${it.name} already registered, skipping")
            } else {
                moduleMap[it.name] = it
            }
        }
    }

    /**
     * 挂起函数，等待指定阶段所有模块初始化完成
     */
    suspend fun initializeAwait(context: Context) {
        val toInit = filterModules(context)
        checkNoCycles(toInit)
        val sorted = topoSort(toInit)

        coroutineScope {
            sorted.map { module ->
                async {
                    try {
                        module.init(context)
                    } catch (e: Throwable) {
                        Log.e("ModuleInitManager", "Init failed", e)
                    }
                }
            }.awaitAll()
        }
    }

    private fun filterModules(context: Context): List<IModuleInitializer> {
        val currentProcess = getCurrentProcessName(context)
        return moduleMap.values.filter {
            (it.processName == null || it.processName == currentProcess)
        }
    }

    private fun checkNoCycles(tasks: Collection<IModuleInitializer>) {
        val visited = mutableSetOf<String>()
        val onStack = mutableSetOf<String>()

        fun dfs(module: IModuleInitializer) {
            if (module.name in onStack) error("Cycle detected at ${module.name}")
            if (module.name in visited) return
            onStack += module.name
            module.dependencies.forEach {
                val dep = moduleMap[it] ?: error("Missing dependency $it")
                dfs(dep)
            }
            onStack -= module.name
            visited += module.name
        }

        tasks.forEach { dfs(it) }
    }

    private fun topoSort(tasks: Collection<IModuleInitializer>): List<IModuleInitializer> {
        val visited = mutableSetOf<String>()
        val stack = mutableSetOf<String>()
        val result = mutableListOf<IModuleInitializer>()

        fun dfs(module: IModuleInitializer) {
            if (module.name in stack) error("Cycle detected at ${module.name}")
            if (module.name in visited) return
            stack += module.name
            module.dependencies.forEach {
                val dep = moduleMap[it] ?: error("Missing dependency $it")
                dfs(dep)
            }
            stack -= module.name
            visited += module.name
            result += module
        }

        tasks.forEach { dfs(it) }
        return result
    }

    private fun getCurrentProcessName(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val pid = android.os.Process.myPid()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName.orEmpty()
        }
    }
}