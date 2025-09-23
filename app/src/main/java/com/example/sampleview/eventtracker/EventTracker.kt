package com.example.sampleview.eventtracker

import android.app.Application
import com.example.sampleview.eventtracker.EventTracker.flushAll
import com.example.sampleview.eventtracker.EventTracker.init
import com.example.sampleview.eventtracker.EventTracker.recoverAndUpload
import com.example.sampleview.eventtracker.EventTracker.scope
import com.example.sampleview.eventtracker.EventTracker.shutdown
import com.example.sampleview.eventtracker.EventTracker.track
import com.example.sampleview.eventtracker.dispatcher.EventDispatcher
import com.example.sampleview.eventtracker.dispatcher.EventDispatcherImpl
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.monitor.AppLifecycleMonitor
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor
import com.example.sampleview.eventtracker.pipeline.EventPipeline
import com.example.sampleview.eventtracker.plugin.EventPlugin
import com.example.sampleview.eventtracker.strategy.EventUploadStrategyRegistry
import com.example.sampleview.eventtracker.strategy.UploadStrategyRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * **EventTracker**
 *
 * 全局事件跟踪器，负责管理事件从采集到上报的完整生命周期。
 *
 * ### 核心职责
 * 1. **拦截器链**：支持事件在上报前经过 [EventInterceptor] 处理（如修改、过滤事件）。
 * 2. **插件链**：支持事件在上报前后经过 [EventPlugin] 扩展逻辑（如打点、日志、埋点扩展）。
 * 3. **事件调度**：将事件分发到对应的上传策略（Immediate / Batch），并管理事件队列。
 * 4. **异步处理**：通过协程作用域执行，保证事件处理不会阻塞主线程。
 * 5. **网络感知**：自动监听网络状态，在网络恢复时触发队列上传。
 *
 * ### 使用流程
 * 1. **初始化**：应用启动时调用 [init]，传入 [EventTrackerConfig] 配置。
 * 2. **注册扩展**：可选注册拦截器 [EventInterceptor] 或插件 [EventPlugin]。
 * 3. **事件上报**：调用 [track] 方法，事件经过拦截器、调度器和插件处理后进入上报流程。
 * 4. **队列管理**：可调用 [flushAll] 手动刷新队列，或调用 [recoverAndUpload] 恢复历史事件。
 * 5. **资源释放**：在应用退出或测试结束时调用 [shutdown] 取消协程并清理资源。
 */
object EventTracker {

    /** 已注册的事件拦截器列表（按顺序执行） */
    private val interceptors = mutableListOf<EventInterceptor>()

    /** 已注册的事件插件列表（按顺序执行） */
    private val plugins = mutableListOf<EventPlugin>()

    /** Application 上下文 */
    internal lateinit var context: Application

    /** 事件处理流水线：串联拦截器、调度器和插件，负责事件的完整处理流程 */
    private lateinit var pipeline: EventPipeline

    /** 事件调度器：负责将事件分发到对应的上传策略（Immediate / Batch） */
    private lateinit var dispatcher: EventDispatcher

    /** 上传策略注册表：管理不同模式的上传策略实例 */
    private lateinit var strategyRegistry: UploadStrategyRegistry

    /** 协程作用域：用于执行异步事件处理，避免阻塞主线程 */
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化 EventTracker。
     *
     * - 构建上传策略、调度器和处理流水线
     * - 注册配置中的拦截器和插件
     * - 启动网络状态监听，支持自动触发上传
     *
     * @param context Application 上下文，用于初始化依赖组件
     * @param config 全局配置对象，没有设置的时候默认使用 [EventTrackerConfig.DEFAULT]
     */
    fun init(context: Application, config: EventTrackerConfig? = null) {
        this.context = context

        val trackerConfig = config ?: EventTrackerConfig.DEFAULT
        this.strategyRegistry = EventUploadStrategyRegistry(trackerConfig)
        this.dispatcher = EventDispatcherImpl(strategyRegistry)

        this.interceptors.addAll(trackerConfig.interceptors)
        this.plugins.addAll(trackerConfig.plugins)

        initMonitor(context)

        this.pipeline = EventPipeline(interceptors, plugins, dispatcher)
    }

    /**
     * 初始化应用级监控器。
     *
     * - 启动网络状态监听和应用前后台状态监听；
     * - 在网络恢复可用时，会自动触发 [EventDispatcher.flushAll] 上报缓存事件；
     * - 可用于上传策略中根据网络和前后台状态触发事件上传。
     *
     * ## 功能
     * 1. 调用 [NetworkStateMonitor.init] 注册网络回调，并在网络可用时自动 flush 事件；
     * 2. 调用 [AppLifecycleMonitor.init] 注册前后台回调，并在应用进入前台时可触发日志或上传；
     * 3. 使用协程 [scope] 收集 SharedFlow 发射的状态变化。
     *
     * @param context [Application] 实例，用于注册生命周期与网络回调
     */
    private fun initMonitor(context: Application) {
        NetworkStateMonitor.init(context, scope)
        scope.launch {
            NetworkStateMonitor.networkFlow.collect { available ->
                if (available) {
                    TrackerLogger.logger.log("网络恢复连接, 尝试上报内存中的事件")
                    dispatcher.flushAll()
                }
            }
        }

        AppLifecycleMonitor.init(context, scope)
        scope.launch {
            AppLifecycleMonitor.isForeground.collect { isForeground ->
                if (isForeground) {
                    TrackerLogger.logger.log("应用进入前台, 尝试上报内存中的事件")
                }
            }
        }
    }

    /**
     * 跟踪并处理一个事件。
     *
     * 处理流程：
     * 1. 事件进入拦截器链（可修改或丢弃事件）
     * 2. 事件分发到上传策略（Immediate / Batch）
     * 3. 插件链在事件处理前后执行扩展逻辑
     *
     * @param event 待跟踪的事件对象
     */
    fun track(event: Event) {
        scope.launch {
            pipeline.process(event)
        }
    }

    /**
     * 强制刷新所有事件队列。
     *
     * - 遍历所有上传策略并触发批量上传
     * - 适用于手动触发上传，或应用退出前清理事件队列
     */
    fun flushAll() {
        scope.launch {
            TrackerLogger.logger.log("手动触发 flushAll")
            dispatcher.flushAll()
        }
    }

    /**
     * 恢复并上传持久化存储中的事件。
     *
     * - 通常在应用启动时调用
     * - 用于恢复应用上次退出前未上报的事件，避免数据丢失
     */
    fun recoverAndUpload() {
        scope.launch {
            TrackerLogger.logger.log("恢复并上传持久化事件")
            dispatcher.recoverAndUpload()
        }
    }

    /**
     * 关闭 EventTracker。
     *
     * - 取消所有协程任务，释放资源
     * - 注销网络状态监听
     *
     * 建议在应用退出或测试结束时调用。
     */
    fun shutdown() {
        scope.cancel("EventTracker shutdown")
        NetworkStateMonitor.unregister(context)
        TrackerLogger.logger.log("EventTracker shutdown 完成")
    }
}
