//package com.example.sampleview.flow
//
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import com.example.sampleview.AppLogger
//import kotlinx.coroutines.CoroutineExceptionHandler
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.flow.MutableSharedFlow
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.launch
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicLong
//import kotlin.reflect.KClass
//
///**
// * FlowBus —— 基于 Kotlin Flow 的事件总线，支持普通事件和粘性事件。
// *
// * 特点：
// * - 普通事件使用 MutableSharedFlow，发送后只有当前活跃订阅者能收到。
// * - 粘性事件使用 MutableStateFlow，新的订阅者会立即收到最近一次事件。
// * - 事件通过双键（事件 key + 事件类型 KClass）管理，避免类型冲突。
// * - 支持 LifecycleOwner 绑定，自动管理生命周期，避免内存泄漏。
// * - 内置协程异常捕获，防止事件收集时崩溃。
// *
// * 核心对外方法说明：
// *
// * 1. 发送普通事件
// *    ```kotlin
// *    FlowBus.post(key: String, value: T)
// *    ```
// *    - key：事件的唯一标识符，同一事件必须保持一致。
// *    - value：事件携带的数据，类型安全，由泛型决定。
// *    - 仅当前订阅者能收到，事件不会缓存。
// *
// *    示例：
// *    ```kotlin
// *    FlowBus.post("user_update", User("Tom"))
// *    ```
// *
// * 2. 发送粘性事件（Sticky Event）
// *    ```kotlin
// *    FlowBus.postSticky(key: String, value: T)
// *    ```
// *    - key：事件唯一标识符。
// *    - value：事件携带的数据。
// *    - 新的订阅者订阅时，会立即收到最近一次发送的事件。
// *
// *    示例：
// *    ```kotlin
// *    FlowBus.postSticky("login_status", true)
// *    ```
// *
// * 3. 订阅普通事件
// *    ```kotlin
// *    LifecycleOwner.observeEvent(key: String, onEvent: (T) -> Unit)
// *    ```
// *    - 在 LifecycleOwner 的生命周期内自动启动协程收集事件。
// *    - 只有在事件发生时收到通知，事件不会缓存。
// *    - 生命周期结束后自动取消收集，避免内存泄漏。
// *
// *    示例：
// *    ```kotlin
// *    lifecycleOwner.observeEvent<User>("user_update") { user ->
// *        // 处理更新的 user
// *    }
// *    ```
// *
// * 4. 订阅粘性事件
// *    ```kotlin
// *    LifecycleOwner.observeStickyEvent(key: String, onEvent: (T) -> Unit)
// *    ```
// *    - 订阅时会立即收到最近一次事件值，如果无历史事件，则使用提供的默认值。
// *    - 事件持续更新时会持续收到通知。
// *    - 生命周期结束后自动取消收集。
// *
// *    示例：
// *    ```kotlin
// *    lifecycleOwner.observeStickyEvent("login_status", false) { isLoggedIn ->
// *        // 根据登录状态处理 UI
// *    }
// *    ```
// * 注意事项：
// * - 泛型方法使用 inline + reified 简化调用，无需显式传入 KClass。
// * - 事件类型必须保证和发送时类型一致，否则无法正确接收。
// * - 使用 LifecycleOwner 绑定自动管理生命周期，避免内存泄漏。
// */
//object FlowBus {
//    /**
//     * 存储普通事件流，Key 为 Pair<事件唯一标识, 事件类型>
//     * MutableSharedFlow 特性：无初始值，replay=0，事件只发给订阅者
//     */
//    private val flows = ConcurrentHashMap<Pair<String, KClass<*>>, MutableSharedFlow<FlowEvent<*>>>()
//
//    /**
//     * 记录每次事件发送的版本,避免因为 [repeatOnLifecycle(Lifecycle.State.STARTED)]导致collect重新回调问题
//     */
//    private val flowsVersions = ConcurrentHashMap<Pair<String, KClass<*>>, AtomicLong>()
//
//    /**
//     * 存储粘性事件流，Key 为 Pair<事件唯一标识, 事件类型>
//     * MutableSharedFlow 特性：有初始值，新的订阅者会立即收到最近一次事件
//     */
//    private val stickyFlows = ConcurrentHashMap<Pair<String, KClass<*>>, MutableStateFlow<FlowEvent<*>>>()
//
//    /**
//     * 记录每次粘性事件发送的版本,避免因为 [repeatOnLifecycle(Lifecycle.State.STARTED)]导致collect重新回调问题
//     */
//    private val stickyVersions = ConcurrentHashMap<Pair<String, KClass<*>>, AtomicLong>()
//
//    /**
//     * 订阅者引用计数器,当没有订阅者的时候则会移除相应的被订阅者
//     */
//    private val observerCounts = ConcurrentHashMap<Pair<String, KClass<*>>, AtomicInteger>()
//
//    /**
//     * 额外缓冲事件数，实际总容量 = replay + EXTRA_BUFFER_CAPACITY
//     */
//    private const val EXTRA_BUFFER_CAPACITY = 10
//
//    /**
//     * 获取普通事件流（MutableSharedFlow）
//     *
//     * @param realKey 事件唯一标识
//     * @return MutableSharedFlow<T> 该事件对应的共享流
//     */
//    @Suppress("UNCHECKED_CAST")
//    private fun <T : Any> event(realKey: Pair<String, KClass<T>>): MutableSharedFlow<FlowEvent<T>> {
//        return flows.getOrPut(realKey) {
//            MutableSharedFlow(replay = 0, extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
//        } as MutableSharedFlow<FlowEvent<T>>
//    }
//
//    /**
//     * 获取粘性事件流（MutableStateFlow）
//     *
//     * @param realKey 事件唯一标识
//     * @param default 默认值
//     * @return MutableStateFlow<T> 该事件对应的状态流
//     */
//    @Suppress("UNCHECKED_CAST")
//    private fun <T : Any> sticky(realKey: Pair<String, KClass<T>>, default: T?): MutableStateFlow<FlowEvent<T>> {
//        return stickyFlows.getOrPut(realKey) {
//            MutableStateFlow(FlowEvent(data = default))
//        } as MutableStateFlow<FlowEvent<T>>
//    }
//
//    /**
//     * 判断是否存在指定 key 和类型的事件流（普通事件或粘性事件）
//     *
//     * @param realKey 事件唯一标识
//     */
//    @PublishedApi
//    internal fun <T : Any> hasEvent(realKey: Pair<String, KClass<T>>): Boolean {
//        return flows.containsKey(realKey)
//    }
//
//    /**
//     * 判断是否存在指定 key 和类型的事件流（普通事件或粘性事件）
//     *
//     * @param realKey 事件唯一标识
//     */
//    @PublishedApi
//    internal fun <T : Any> hasStickyEvent(realKey: Pair<String, KClass<T>>): Boolean {
//        return stickyFlows.containsKey(realKey)
//    }
//
//    /**
//     * LifecycleOwner 便捷扩展函数，用于在生命周期内收集普通事件
//     * 自动启动协程并捕获异常
//     *
//     * @param realKey 事件唯一标识
//     * @param onEvent 收到事件回调
//     */
//    @PublishedApi
//    internal fun <T : Any> LifecycleOwner.observeEvent(realKey: Pair<String, KClass<T>>, onEvent: (T) -> Unit): Job {
//        val handler = CoroutineExceptionHandler { _, throwable ->
//            logErrorFlowBus("FlowBus-observeEvent", realKey, throwable)
//        }
//        val job = lifecycleScope.launch(handler) {
//            incrementObserver(realKey)
//            var lastVersion = -1L
//            logFlowBus("Subscribed event", realKey)
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                event(realKey).collect { event ->
//                    logFlowBus("Received Event", realKey, event)
//                    if (event.version > lastVersion) {
//                        event.data?.let(onEvent)
//                        lastVersion = event.version
//                    } else {
//                        logFlowBus("Filter StickyEvent", realKey, event)
//                    }
//                }
//            }
//        }
//        job.invokeOnCompletion {
//            decrementObserver(realKey)
//        }
//        return job
//    }
//
//    /**
//     * LifecycleOwner 便捷扩展函数，用于在生命周期内收集粘性事件
//     * 自动启动协程并捕获异常
//     *
//     * @param realKey 事件唯一标识
//     * @param onEvent 收到事件回调
//     */
//    @PublishedApi
//    internal fun <T : Any> LifecycleOwner.observeStickyEvent(realKey: Pair<String, KClass<T>>, default: T?, onEvent: (T) -> Unit): Job {
//        val handler = CoroutineExceptionHandler { _, throwable ->
//            logErrorFlowBus("FlowBus-observeStickyEvent", realKey, throwable)
//        }
//        val job = lifecycleScope.launch(handler) {
//            incrementObserver(realKey)
//            logFlowBus("Subscribed StickyEvent", realKey)
//            repeatOnLifecycle(Lifecycle.State.STARTED) {
//                var lastVersion = -1L
//                sticky(realKey, default).collect { event ->
//                    if (event.version > lastVersion) {
//                        logFlowBus("Received StickyEvent", realKey, event)
//                        event.data?.let(onEvent)
//                        lastVersion = event.version
//                    } else {
//                        logFlowBus("Filter StickyEvent", realKey, event)
//                    }
//                }
//            }
//        }
//        job.invokeOnCompletion {
//            decrementObserver(realKey)
//        }
//        return job
//    }
//
//    /**
//     * 发送普通事件（异步发射）
//     * 尝试使用 tryEmit 避免挂起，失败时新建事件流并发送
//     *
//     * @param realKey 事件唯一标识
//     * @param value 事件数据
//     */
//    @Suppress("UNCHECKED_CAST")
//    @PublishedApi
//    internal fun <T : Any> post(realKey: Pair<String, KClass<T>>, value: T) {
//        runCatching {
//            val flow = flows.getOrPut(realKey) {
//                MutableSharedFlow(replay = 0, extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
//            } as MutableSharedFlow<FlowEvent<T>>
//            val version = flowsVersions.getOrPut(realKey) { AtomicLong(0) }.incrementAndGet()
//            val emitted = flow.tryEmit(FlowEvent(version, value))
//            if (!emitted) {
//                logErrorFlowBus("FlowBus-post", realKey, IllegalStateException("FlowBus tryEmit failed"))
//            } else {
//                logFlowBus("Posted Event", realKey, value)
//            }
//        }.onFailure {
//            logErrorFlowBus("FlowBus-post", realKey, it)
//        }
//    }
//
//    /**
//     * 发送粘性事件，更新事件状态
//     *
//     * @param realKey 事件唯一标识
//     * @param value 事件数据
//     */
//    @PublishedApi
//    internal fun <T : Any> postSticky(realKey: Pair<String, KClass<T>>, value: T) {
//        runCatching {
//            val flow = stickyFlows.getOrPut(realKey) { MutableStateFlow(FlowEvent(data = value)) }
//            val version = stickyVersions.getOrPut(realKey) { AtomicLong(0) }.incrementAndGet()
//            flow.value = FlowEvent(version, value)
//            logFlowBus("Posted StickyEvent", realKey, value)
//        }.onFailure {
//            logErrorFlowBus("FLowBus-postSticky", realKey, it)
//        }
//    }
//
//    /**
//     * 移除某个事件（普通和粘性）
//     *
//     * @param key 事件唯一标识
//     * @param clazz 事件类型 KClass
//     */
//    fun remove(key: String, clazz: KClass<*>) {
//        flows.remove(key to clazz)
//        stickyFlows.remove(key to clazz)
//        stickyFlows.remove(key to clazz)
//        flowsVersions.remove(key to clazz)
//        observerCounts.remove(key to clazz)
//    }
//
//    /**
//     * 清理所有事件（慎用）
//     */
//    fun clearAll() {
//        flows.clear()
//        stickyFlows.clear()
//        stickyVersions.clear()
//        flowsVersions.clear()
//        observerCounts.clear()
//    }
//
//    /**
//     * 对外暴露的简化调用接口 —— 发送普通事件
//     *
//     * @param key 事件的唯一标识符
//     * @param value 事件携带的数据
//     */
//    inline fun <reified T : Any> FlowBus.post(key: String, value: T) = post(key to T::class, value)
//
//    /**
//     * 对外暴露的简化调用接口 —— 发送粘性事件（Sticky Event）
//     *
//     * 使用 reified 简化调用，无需显式传 KClass。
//     *
//     * @param key 事件的唯一标识符
//     * @param value 事件携带的数据
//     */
//    inline fun <reified T : Any> FlowBus.postSticky(key: String, value: T) = postSticky(key to T::class, value)
//
//    /**
//     * 对外暴露的简化调用接口 —— 订阅普通事件（非粘性）
//     *
//     * @param key 事件标识符，必须与 post 时一致
//     * @param onEvent 收到事件后的处理回调
//     */
//    inline fun <reified T : Any> LifecycleOwner.observeEvent(
//        key: String, noinline onEvent: (T) -> Unit
//    ) = this.observeEvent(key to T::class, onEvent)
//
//    /**
//     * 对外暴露的简化调用接口 —— 订阅粘性事件，订阅时会立即收到上一次的事件数据（如果有），否则调用默认值。
//     *
//     * @param key 事件标识符，必须与 postSticky 时一致
//     * @param onEvent 收到事件后的处理回调
//     */
//    inline fun <reified T : Any> LifecycleOwner.observeStickyEvent(
//        key: String, noinline onEvent: (T) -> Unit
//    ) = this.observeStickyEvent(key to T::class, null, onEvent)
//
//    /**
//     * 对外暴露的简化调用接口 —— 订阅粘性事件，订阅时会立即收到上一次的事件数据（如果有），否则调用默认值。
//     *
//     * @param key 事件标识符，必须与 postSticky 时一致
//     * @param default 可指定默认值
//     * @param onEvent 收到事件后的处理回调
//     */
//    inline fun <reified T : Any> LifecycleOwner.observeStickyEvent(
//        key: String, default: T?, noinline onEvent: (T) -> Unit
//    ) = this.observeStickyEvent(key to T::class, default, onEvent)
//
//    /**
//     * 简化调用，使用 reified 泛型判断是否存在事件流
//     * @param key 事件标识符
//     */
//    inline fun <reified T : Any> hasEvent(key: String): Boolean = hasEvent(key to T::class)
//
//    /**
//     * 简化调用，使用 reified 泛型判断是否存在事件流
//     * @param key 事件标识符
//     */
//    inline fun <reified T : Any> hasStickyEvent(key: String): Boolean = hasStickyEvent(key to T::class)
//
//    /**
//     * 增加指定事件的订阅者计数。
//     *
//     * 每次有新订阅者订阅某个事件（由事件名和数据类型共同标识）时调用此方法。
//     * 如果该事件之前没有订阅者，会自动初始化计数器。
//     *
//     * @param key Pair<String, KClass<*>> 事件唯一标识，由事件名和数据类型构成。
//     */
//    private fun incrementObserver(key: Pair<String, KClass<*>>) {
//        observerCounts.getOrPut(key) { AtomicInteger(0) }.incrementAndGet()
//    }
//
//    /**
//     * 减少指定事件的订阅者计数。
//     *
//     * 每次有订阅者取消订阅事件时调用此方法。
//     * 如果计数减少后为 0 或更小，则自动清除与该事件相关的所有 Flow 缓存和版本记录，
//     * 以节省内存并避免资源泄漏。
//     *
//     * @param key Pair<String, KClass<*>> 事件唯一标识，由事件名和数据类型构成。
//     */
//    private fun decrementObserver(key: Pair<String, KClass<*>>) {
//        val count = observerCounts[key]?.decrementAndGet()
//        if (count != null && count <= 0) {
//            remove(key.first, key.second)
//            logFlowBus("Auto-cleared due to zero observer", key)
//        }
//    }
//
//    /**
//     * 记录普通的 FlowBus 事件日志
//     *
//     * @param action 事件动作描述，例如 "Subscribed"、"Received"
//     * @param realKey 事件的唯一标识 key
//     * @param value 事件携带的数据，可选
//     */
//    private fun logFlowBus(action: String, realKey: Pair<String, KClass<*>>, value: Any? = null) {
//        AppLogger.d("FlowBus", buildLog(action, realKey, value))
//    }
//
//    /**
//     * 记录 FlowBus 事件中的异常错误日志
//     *
//     * @param action 事件动作描述，例如 "SubscribeFailed"
//     * @param realKey 事件的唯一标识 key
//     * @param error 捕获的异常对象
//     */
//    private fun logErrorFlowBus(action: String, realKey: Pair<String, KClass<*>>, error: Throwable) {
//        AppLogger.e("FlowBus", buildLog(action, realKey), error, true)
//    }
//
//    /**
//     * 构建统一的 FlowBus 日志字符串
//     *
//     * 包含事件动作、key、类型信息，以及可选的事件数据和异常信息。
//     * 对于数据长度超过200字符的，做截断处理。
//     * 异常信息包含异常消息和完整堆栈信息。
//     *
//     * @param action 事件动作描述
//     * @param realKey 事件的唯一标识 key
//     * @param value 事件携带的数据，可选
//     * @return 组装完成的日志字符串
//     */
//    private fun buildLog(action: String, realKey: Pair<String, KClass<*>>, value: Any? = null): String {
//        return buildString {
//            append("[FlowBus] ").append(action)
//            append(" | key=").append(realKey.first)
//            append(" | type=").append(realKey.second.simpleName)
//            value?.let { valueObj ->
//                val str = valueObj.toString().takeIf { it.length <= 200 } ?: "${valueObj.toString().take(200)}...（超长）"
//                append(" | value=").append(str)
//            }
//        }
//    }
//}
