package com.example.sampleview.eventtracker

import android.app.Application
import com.example.sampleview.AppLogger
import com.example.sampleview.eventtracker.EventTracker.flushAll
import com.example.sampleview.eventtracker.EventTracker.init
import com.example.sampleview.eventtracker.EventTracker.restoreAll
import com.example.sampleview.eventtracker.EventTracker.shutdown
import com.example.sampleview.eventtracker.EventTracker.track
import com.example.sampleview.eventtracker.dispatcher.EventDispatcher
import com.example.sampleview.eventtracker.dispatcher.EventDispatcherImpl
import com.example.sampleview.eventtracker.interceptor.EventInterceptor
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult
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
 * EventTracker 是全局事件跟踪器，负责整个事件采集与上报流程的统一管理。
 *
 * ### 核心职责
 * 1. 管理事件拦截器链（EventInterceptor）和插件链（EventPlugin），支持事件处理前后的扩展逻辑。
 * 2. 调度事件到上传策略（Immediate / Batch）并管理事件队列。
 * 3. 提供异步跟踪接口，支持事件批量刷新和恢复未上报事件。
 * 4. 提供统一的协程作用域，保证事件处理不会阻塞主线程。
 *
 * ### 使用流程
 * 1. 应用启动时调用 [init] 初始化 EventTracker，并传入配置对象。
 * 2. 可选注册拦截器 [EventInterceptor] 或插件 [EventPlugin]，扩展事件处理逻辑。
 * 3. 调用 [track] 跟踪事件，事件会经过拦截器链、调度器和插件链处理。
 * 4. 可通过 [flushAll] 手动刷新事件队列，通过 [restoreAll] 恢复未上报事件。
 * 5. 应用退出或测试结束时调用 [shutdown] 释放协程资源。
 */
object EventTracker {

    /** 已注册的事件拦截器列表 */
    private val interceptors = mutableListOf<EventInterceptor>()

    /** 已注册的事件插件列表 */
    private val plugins = mutableListOf<EventPlugin>()

    /** Application 上下文 */
    private lateinit var context: Application

    /** Pipeline，用于串联拦截器、Dispatcher 和插件，处理事件全流程 */
    private lateinit var pipeline: EventPipeline

    /** 事件调度器，负责分发事件到对应的上传策略 */
    private lateinit var dispatcher: EventDispatcher

    /** 上传策略注册表，管理不同模式的上传策略 */
    private lateinit var strategyRegistry: UploadStrategyRegistry

    /** 协程作用域，用于异步执行事件处理逻辑 */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 初始化 EventTracker。
     *
     * 根据配置对象初始化上传策略、Dispatcher、Pipeline 等核心组件。
     * 需要在应用启动阶段调用。
     *
     * @param context Application 上下文，用于组件初始化
     * @param config 全局配置对象，若为空则使用默认配置 [EventTrackerConfig.DEFAULT]
     */
    fun init(context: Application, config: EventTrackerConfig = EventTrackerConfig.DEFAULT) {
        this.context = context
        this.strategyRegistry = EventUploadStrategyRegistry(config.uploaderConfig)
        this.dispatcher = EventDispatcherImpl(strategyRegistry)
        this.interceptors.addAll(config.interceptors)
        this.plugins.addAll(config.plugins)
        this.plugins.add(object : EventPlugin {
            override suspend fun onEventBeforeTrack(event: Event) {
                super.onEventBeforeTrack(event)
                AppLogger.d("EventTracker", "EventTracker.onEventBeforeTrack :$event")
            }

            override suspend fun onEventAfterTrack(event: Event, result: EventUploadResult) {
                super.onEventAfterTrack(event, result)
                AppLogger.d("EventTracker", "EventTracker.onEventAfterTrack :$result")
            }
        })

        this.pipeline = EventPipeline(interceptors, plugins, dispatcher)
    }

    /**
     * 跟踪并处理事件。
     *
     * 事件会经过：
     * 1. 拦截器链处理（可修改或过滤事件）
     * 2. Dispatcher 调度到对应上传策略（Immediate 或 Batch）
     * 3. 插件链前后逻辑处理（如日志、埋点扩展）
     *
     * @param event 待跟踪的事件
     */
    fun track(event: Event) {
        scope.launch {
            pipeline.process(event)
        }
    }

    /**
     * 强制刷新所有事件队列。
     *
     * 遍历所有上传策略并触发批量上报，适用于手动触发上传或应用退出前清理队列。
     */
    fun flushAll() {
        scope.launch {
            dispatcher.flushAll()
        }
    }

    /**
     * 从持久化存储恢复事件到各上传策略队列。
     *
     * 一般在应用启动时调用，用于恢复未上报的事件，保证事件不丢失。
     */
    fun restoreAll() {
        scope.launch {
            dispatcher.restoreAll()
        }
    }

    /**
     * 关闭 EventTracker，取消所有协程任务。
     *
     * 一般在应用退出或测试时调用，以释放资源。
     */
    fun shutdown() {
        scope.cancel("EventTracker shutdown")
    }
}
