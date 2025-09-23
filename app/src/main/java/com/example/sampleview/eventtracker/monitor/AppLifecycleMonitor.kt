package com.example.sampleview.eventtracker.monitor

import android.app.Application
import android.os.Bundle
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.monitor.AppLifecycleMonitor.init
import com.example.sampleview.eventtracker.monitor.AppLifecycleMonitor.isForeground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * **AppLifecycleMonitor** - 应用前后台状态监控器
 *
 * 该对象用于监听应用是否处于前台或后台状态，可作为事件上传策略或其他业务逻辑的依据。
 *
 * ## 功能
 * 1. 通过 [Application.ActivityLifecycleCallbacks] 自动监听应用前后台状态；
 * 2. 提供 [isForeground] 共享流，可在协程中订阅前后台状态变化；
 * 3. 支持首次启动不触发前台回调，避免误判应用从后台切换到前台。
 *
 * ## 注意事项
 * - 需在 [Application.onCreate] 中调用 [init] 进行初始化；
 * - [isForeground] 使用 [MutableSharedFlow]，订阅时不会立即发射上一次状态；
 * - 多个 Activity 前后台切换会正确计算应用整体前后台状态；
 * - 首次启动应用不会触发进入前台事件，避免重复上传或错误逻辑。
 */
object AppLifecycleMonitor {

    /** 内部可变流，用于发射前后台状态变化 */
    private val _isForeground = MutableSharedFlow<Boolean>()

    /** 对外只读共享流，协程订阅前后台状态变化 */
    val isForeground = _isForeground.asSharedFlow()

    /**
     * 初始化应用前后台监控
     *
     * - 需在 Application 中调用一次
     * - 会注册 [Application.ActivityLifecycleCallbacks] 来监听应用前后台状态
     *
     * @param application [Application] 实例
     * @param scope 协程作用域，用于异步发射前后台状态变化
     */
    fun init(application: Application, scope: CoroutineScope) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {

            /** 当前活跃的 Activity 数量，用于计算应用前后台状态 */
            private var activityCount = 0

            /** 标记应用首次启动，首次启动不触发进入前台事件 */
            private var firstLaunch = true

            override fun onActivityStarted(activity: android.app.Activity) {
                activityCount++
                if (activityCount == 1 && !firstLaunch) {
                    TrackerLogger.logger.log("应用进入前台")
                    scope.launch { _isForeground.emit(true) }
                }
                if (firstLaunch) {
                    firstLaunch = false
                }
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                activityCount--
                if (activityCount == 0) {
                    TrackerLogger.logger.log("应用进入后台")
                    scope.launch { _isForeground.emit(false) }
                }
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
}
