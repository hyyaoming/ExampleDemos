package com.example.sampleview.flow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * FlowEventObserver 是 FlowBus 事件订阅的核心实现对象，
 * 负责封装事件流的监听逻辑，包括粘性和非粘性事件两种订阅方式。
 *
 * 主要职责包括：
 * - 支持非粘性事件订阅，基于 [MutableSharedFlow]，事件只会分发给活跃订阅者，未订阅时不会缓存事件。
 * - 支持粘性事件订阅，基于 [MutableStateFlow]，订阅时可以立即收到最近一次事件。
 * - 结合 [LifecycleOwner] 的生命周期感知，在 [Lifecycle.State.STARTED] 状态时自动开始收集事件，
 *   低于该状态自动暂停收集，防止内存泄漏和重复事件消费。
 * - 自动管理订阅计数，调用 [FlowBusCore] 进行订阅者增加和减少的统计。
 * - 捕获协程异常，避免事件监听异常崩溃，并通过 [FlowBusLogger] 打印错误日志。
 *
 * @see MutableSharedFlow
 * @see MutableStateFlow
 * @see androidx.lifecycle.repeatOnLifecycle
 */
@PublishedApi
internal object FlowEventObserver {

    /**
     * 初始事件版本号，表示尚未接收到任何事件。
     */
    private const val INITIAL_VERSION = -1L

    /**
     * 订阅非粘性事件，即通过 SharedFlow 发布的事件。
     *
     * 该事件只会分发给当前活跃的订阅者，
     * 订阅前发送的事件不会被缓存或补发。
     *
     * 通过 [LifecycleOwner] 管理订阅生命周期，订阅在生命周期处于 STARTED 时生效。
     *
     * @param T 事件数据类型，必须为非空类型
     * @param owner 生命周期拥有者，决定订阅的自动启动和取消
     * @param eventKey 事件的唯一标识，包括事件名和类型信息
     * @param onEvent 事件回调函数，当接收到事件时触发
     * @return [Job] 用于手动取消订阅
     *
     * @see MutableSharedFlow
     * @see FlowBusCore.event
     * @see observeInternal
     */
    fun <T : Any> observeEvent(
        owner: LifecycleOwner,
        eventKey: EventKey<T>,
        onEvent: (T) -> Unit
    ): Job = owner.observeInternal(eventKey, onEvent, FlowBusCore.event(eventKey))

    /**
     * 订阅粘性事件，即通过 StateFlow 发布的事件。
     *
     * 订阅时会立即收到最近一次事件，即使该事件是在订阅前发送的。
     *
     * 通过 [LifecycleOwner] 管理订阅生命周期，订阅在生命周期处于 STARTED 时生效。
     *
     * @param T 事件数据类型，必须为非空类型
     * @param owner 生命周期拥有者，决定订阅的自动启动和取消
     * @param eventKey 事件的唯一标识，包括事件名和类型信息
     * @param default 粘性事件的默认初始值，订阅时如果无历史事件则触发该默认值
     * @param onEvent 事件回调函数，当接收到事件时触发
     * @return [Job] 用于手动取消订阅
     *
     * @see MutableStateFlow
     * @see FlowBusCore.sticky
     * @see observeInternal
     */
    fun <T : Any> observeStickyEvent(
        owner: LifecycleOwner,
        eventKey: EventKey<T>,
        default: T?,
        onEvent: (T) -> Unit
    ): Job = owner.observeInternal(eventKey, onEvent, FlowBusCore.sticky(eventKey, default))

    /**
     * 订阅事件的通用内部实现方法。
     *
     * 该方法整合了生命周期感知启动、异常处理、事件版本判重、订阅者计数管理等功能。
     *
     * 订阅时，会使用 [repeatOnLifecycle] 保证只有在 [Lifecycle.State.STARTED] 及以上状态才收集事件，
     * 防止因界面不可见时的重复消费和内存泄漏。
     *
     * 通过版本号过滤，避免相同版本的事件被多次消费。
     *
     * 协程异常时会记录日志但不会崩溃应用。
     *
     * 订阅开始时调用 [FlowBusCore.incrementObserver] 计数，
     * 订阅结束时调用 [FlowBusCore.decrementObserver] 释放计数。
     *
     * @param T 事件数据类型，非空
     * @receiver LifecycleOwner 用于生命周期感知
     * @param eventKey 事件唯一标识
     * @param onEvent 收到事件时的回调
     * @param flow 对应事件的数据流，支持 SharedFlow 或 StateFlow
     * @return [Job] 用于取消订阅
     *
     * @see androidx.lifecycle.repeatOnLifecycle
     * @see FlowBusCore.incrementObserver
     * @see FlowBusCore.decrementObserver
     * @see FlowEvent
     */
    private fun <T : Any> LifecycleOwner.observeInternal(
        eventKey: EventKey<T>,
        onEvent: (T) -> Unit,
        flow: Flow<FlowEvent<T>>
    ): Job {
        val handler = CoroutineExceptionHandler { _, throwable ->
            FlowBusLogger.logError("observeInternal", eventKey, throwable)
        }
        var lastVersion = INITIAL_VERSION
        val job = lifecycleScope.launch(handler) {
            FlowBusCore.incrementObserver(eventKey)
            FlowBusLogger.log("Subscribed", eventKey)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect { event ->
                    if (event.version > lastVersion) {
                        event.data?.let(onEvent)
                        lastVersion = event.version
                    }
                }
            }
        }
        job.invokeOnCompletion {
            FlowBusCore.decrementObserver(eventKey)
        }
        return job
    }
}
