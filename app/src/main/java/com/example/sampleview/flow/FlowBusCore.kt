package com.example.sampleview.flow

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * FlowBusCore 是 FlowBus 的底层核心管理器，负责维护所有事件通道、事件版本、观察者数量等信息。
 *
 * ## 功能职责：
 * 1. 提供 [MutableSharedFlow]（用于一次性事件）与 [MutableStateFlow]（用于粘性事件）缓存池；
 * 2. 每个事件通道通过 [EventKey] 标识，支持同一 key 不同类型并存；
 * 3. 记录每个事件的版本号，配合 [FlowEvent] 实现事件去重和粘性事件版本校验；
 * 4. 跟踪订阅者数量，实现无人订阅时自动清理相关资源；
 * 5. 提供统一的移除和清空机制，便于全局管理。
 *
 * ## 使用场景：
 * 不建议业务代码直接调用该类，推荐通过 [FlowEventPublisher]、[FlowEventObserver] 等封装入口访问事件流。
 *
 * @see FlowEvent
 * @see EventKey
 * @see EventSlot
 * @see FlowEventPublisher
 * @see FlowEventObserver
 */
@PublishedApi
internal object FlowBusCore {

    /**
     * 每个 SharedFlow 的额外缓冲区大小。
     *
     * 这个缓冲区允许在订阅者消费事件稍有延迟时，事件发送者依然能顺畅发射事件，
     * 避免因背压（backpressure）导致事件丢失或挂起。
     *
     * 这里设置为 8，意为最多允许缓存 8 条事件以保证流畅性。
     *
     * @see event
     */
    private const val EXTRA_BUFFER_CAPACITY = 8

    /**
     * 非粘性事件流池，存储所有非粘性事件对应的流。
     *
     * key: [EventKey]，唯一标识某类事件（通常包含业务标识与事件类型）
     * value: [MutableSharedFlow]<[FlowEvent]>，用于分发事件给所有订阅者。
     *
     * 非粘性事件不会缓存历史事件，订阅者只会收到订阅后发生的新事件，
     * 适合瞬时广播事件，例如按钮点击、通知触发等不需要保留历史状态的事件。
     *
     * @see event
     * @see getEventSlot
     */
    private val flows = ConcurrentHashMap<EventKey<*>, MutableSharedFlow<FlowEvent<*>>>()

    /**
     * 版本控制器，针对每个非粘性事件流维护当前事件的版本号。
     *
     * 版本号用于标记事件的唯一递增标识，确保消费者能识别事件是否已处理，
     * 防止因事件重发或重复订阅导致的重复处理。
     *
     * key: [EventKey]
     * value: [AtomicLong]，当前版本计数器
     *
     * @see getEventSlot
     */
    private val flowsVersions = ConcurrentHashMap<EventKey<*>, AtomicLong>()

    /**
     * 粘性事件流池，存储所有粘性事件对应的流。
     *
     * 粘性事件会缓存最新事件数据，即使订阅者晚于事件发送时间订阅，也能立刻收到最新状态。
     * 适合状态同步类事件，比如用户状态、网络连接状态、配置变化等。
     *
     * key: [EventKey]
     * value: [MutableStateFlow]<[FlowEvent]>，始终持有最新事件值
     *
     * @see sticky
     * @see getEventSlot
     */
    private val stickyFlows = ConcurrentHashMap<EventKey<*>, MutableStateFlow<FlowEvent<*>>>()

    /**
     * 版本控制器，针对每个粘性事件流维护当前事件的版本号。
     *
     * 作用同 [flowsVersions]，保证事件版本递增，支持事件唯一标识与去重。
     *
     * key: [EventKey]
     * value: [AtomicLong]，当前版本计数器
     *
     * @see getEventSlot
     */
    private val stickyVersions = ConcurrentHashMap<EventKey<*>, AtomicLong>()

    /**
     * 订阅者计数器，统计每个事件流当前活跃的订阅者数量。
     *
     * 订阅者数量为 0 时，说明无人监听该事件，流可以安全释放，避免内存泄漏。
     *
     * key: [EventKey]
     * value: [AtomicInteger]，当前订阅者数量
     *
     * @see incrementObserver
     * @see decrementObserver
     */
    private val observerCounts = ConcurrentHashMap<EventKey<*>, AtomicInteger>()

    /**
     * 获取或创建一个非粘性事件流 [MutableSharedFlow]。
     *
     * - 非粘性事件不会缓存历史事件，只有活跃订阅者才能收到事件。
     * - 流有有限缓冲区，避免背压导致发送端阻塞或事件丢失。
     *
     * @param eventKey 唯一事件标识，区分不同事件通道
     * @return 事件流实例，用于事件发布与订阅
     *
     * @see flows
     * @see getEventSlot
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> event(eventKey: EventKey<T>): MutableSharedFlow<FlowEvent<T>> {
        return flows.computeIfAbsent(eventKey) {
            MutableSharedFlow(replay = 0, extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
        } as MutableSharedFlow<FlowEvent<T>>
    }

    /**
     * 获取或创建一个粘性事件流 [MutableStateFlow]。
     *
     * - 粘性事件会保存最新事件值，即使订阅者晚于事件发布时间，也能收到最新事件。
     * - 适用于状态同步场景。
     *
     * @param eventKey 唯一事件标识
     * @param default 粘性事件的默认初始值（无事件时可为 null）
     * @return 粘性事件流，始终持有最新事件数据
     *
     * @see stickyFlows
     * @see getEventSlot
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> sticky(eventKey: EventKey<T>, default: T?): MutableStateFlow<FlowEvent<T>> {
        return stickyFlows.computeIfAbsent(eventKey) {
            MutableStateFlow(FlowEvent(data = default))
        } as MutableStateFlow<FlowEvent<T>>
    }

    /**
     * 获取事件槽位信息，包含对应事件流和当前版本号。
     *
     * - 根据是否为粘性事件选择不同事件流及版本计数器。
     * - 每次调用版本号自动递增，确保事件版本唯一且递增。
     * - 版本号用于订阅端去重及处理逻辑判断。
     *
     * @param eventKey 唯一事件标识
     * @param value 当前事件值，供粘性事件初始化使用
     * @param isSticky 是否为粘性事件
     * @return [EventSlot] 包含事件流与版本号
     *
     * @see flowsVersions
     * @see stickyVersions
     * @see event
     * @see sticky
     */
    fun <T : Any> getEventSlot(eventKey: EventKey<T>, value: T, isSticky: Boolean): EventSlot<T> {
        return if (isSticky) {
            val stickyVersion = stickyVersions.computeIfAbsent(eventKey) { AtomicLong(0) }.incrementAndGet()
            EventSlot(sticky(eventKey, value), stickyVersion)
        } else {
            val eventVersion = flowsVersions.computeIfAbsent(eventKey) { AtomicLong(0) }.incrementAndGet()
            EventSlot(event(eventKey), eventVersion)
        }
    }

    /**
     * 增加某事件流的订阅者数量计数。
     *
     * 订阅者数量用于管理事件流生命周期，当无人订阅时可释放资源。
     *
     * @param eventKey 事件流标识
     *
     * @see observerCounts
     * @see decrementObserver
     */
    fun <T : Any> incrementObserver(eventKey: EventKey<T>) {
        observerCounts.compute(eventKey) { _, count ->
            (count ?: AtomicInteger(0)).apply { incrementAndGet() }
        }
    }

    /**
     * 减少某事件流的订阅者数量计数。
     *
     * 当订阅者数量减少至 0 时，自动移除该事件流及相关缓存数据，防止内存泄漏。
     *
     * @param eventKey 事件流标识
     *
     * @see observerCounts
     * @see incrementObserver
     * @see remove
     */
    fun <T : Any> decrementObserver(eventKey: EventKey<T>) {
        observerCounts.computeIfPresent(eventKey) { _, count ->
            val newCount = count.decrementAndGet()
            if (newCount <= 0) {
                remove(eventKey)
                null
            } else {
                count
            }
        }
    }

    /**
     * 移除指定事件通道及其所有缓存数据，包括事件流、版本号和订阅计数。
     *
     * 用于彻底清理某事件的所有资源，通常在事件不再需要广播时调用。
     *
     * @param eventKey 唯一事件标识
     *
     * @see flows
     * @see stickyFlows
     * @see flowsVersions
     * @see stickyVersions
     * @see observerCounts
     */
    fun <T : Any> remove(eventKey: EventKey<T>) {
        flows.remove(eventKey)
        stickyFlows.remove(eventKey)
        flowsVersions.remove(eventKey)
        stickyVersions.remove(eventKey)
        observerCounts.remove(eventKey)
    }

    /**
     * 清空所有事件通道及缓存数据。
     *
     * 警告：此操作会清除全部事件流和订阅数据，
     * 一般仅在全局退出或测试场景使用。
     *
     * @see remove
     */
    fun clearAll() {
        flows.clear()
        stickyFlows.clear()
        flowsVersions.clear()
        stickyVersions.clear()
        observerCounts.clear()
    }
}
