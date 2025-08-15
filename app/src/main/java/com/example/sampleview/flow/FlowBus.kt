package com.example.sampleview.flow

import androidx.lifecycle.LifecycleOwner

/**
 * FlowBus 是一个基于 Kotlin Flow 的轻量级事件总线（EventBus）实现，提供统一的事件发布与订阅接口。
 *
 * 通过 FlowBus，开发者可以实现应用内部不同模块之间的异步事件通信，支持数据类型安全和生命周期安全，
 * 方便在 MVVM、组件化等架构中进行解耦通信。
 *
 * ## 主要功能
 * - 支持普通事件与粘性事件（Sticky Event）
 *   - 普通事件仅当前订阅者可接收
 *   - 粘性事件即使在事件发送后订阅，也能接收到最近一次事件
 * - 支持绑定 Android 组件生命周期（LifecycleOwner），自动管理事件订阅的生命周期，避免内存泄漏
 * - 支持事件的移除与全部事件清理，方便事件管理和资源释放
 * - 事件类型安全，通过 Kotlin 的 reified 泛型和事件类型封装保证数据类型一致性
 * - 支持协程挂起函数版本的事件发布，方便在 suspend 环境下调用
 *
 * ## 核心组件
 * - [FlowBusCore]：底层事件通道管理及事件版本控制
 * - [FlowEventObserver]：事件订阅者的封装，支持 LifecycleOwner 自动解绑
 * - [FlowEventPublisher]：统一的事件发送接口，负责事件的分发和缓存处理
 * - [FlowEvent]：事件封装，包含事件数据和版本信息
 * - [FlowBusLogger]：日志打印和调试支持
 * - [EventSlot]：事件槽，用于事件的存储与传递
 * - [EventKey]：事件唯一标识，由事件名和数据类型组成，确保类型安全和唯一性
 *
 * ## 使用说明
 *
 * ### 事件发布
 * - `post(key, value)`：发布普通事件，只有当前已订阅者能接收到
 * - `postSuspend(key, value)`：挂起函数版本的普通事件发布，适合协程中调用
 * - `postSticky(key, value)`：发布粘性事件，订阅时能接收到最近一次事件
 * - `postStickySuspend(key, value)`：挂起函数版本的粘性事件发布，适合协程中调用
 *
 * ### 事件订阅
 * - `observeEvent(owner, key, onEvent)`：订阅普通事件，绑定生命周期自动取消订阅，避免内存泄漏
 * - `observeStickyEvent(owner, key, default, onEvent)`：订阅粘性事件，支持设置默认初始值（用于第一次没有历史事件时的情况）
 *
 * ### 事件管理
 * - `remove(key)`：移除指定事件及其缓存，释放资源
 * - `clearAll()`：清除所有事件（包括普通事件和粘性事件），一般在应用退出或重置时调用
 *
 * ## 注意事项
 * - 事件唯一标识由事件名（String）和数据类型（KClass）组成，避免类型冲突
 * - 订阅时请务必绑定 LifecycleOwner，防止内存泄漏
 * - 粘性事件缓存最新事件，注意避免因事件缓存导致内存压力
 *
 * ## 示例代码
 * ```kotlin
 * // 发送普通事件
 * FlowBus.post("LoginSuccess", userData)
 *
 * // 发送普通事件（挂起函数版本）
 * suspend fun sendLogin() {
 *     FlowBus.postSuspend("LoginSuccess", userData)
 * }
 *
 * // 发送粘性事件
 * FlowBus.postSticky("ConfigUpdated", config)
 *
 * // 发送粘性事件（挂起函数版本）
 * suspend fun sendConfigUpdate() {
 *     FlowBus.postStickySuspend("ConfigUpdated", config)
 * }
 *
 * // 订阅普通事件
 * FlowBus.observeEvent(lifecycleOwner, "LoginSuccess") { user ->
 *     // 处理登录成功事件
 * }
 *
 * // 订阅粘性事件，设置默认值
 * FlowBus.observeStickyEvent(lifecycleOwner, "ConfigUpdated", defaultConfig) { config ->
 *     // 处理配置更新事件
 * }
 *
 * // 移除事件
 * FlowBus.remove<User>("LoginSuccess")
 *
 * // 清除所有事件
 * FlowBus.clearAll()
 * ```
 */
object FlowBus {

    /**
     * 发布普通事件（非粘性），仅当前已订阅者会接收到该事件。
     *
     * 该方法为非挂起函数，适用于普通同步场景下的事件发送。
     * 若当前没有订阅者，则事件会被丢弃，不会进行缓存。
     *
     * @param key 事件唯一标识符（可用于区分同类型不同用途事件）
     * @param value 要发送的事件数据，类型为 T
     *
     * @see postSuspend 挂起版本
     * @see postSticky 粘性版本
     */
    inline fun <reified T : Any> post(key: String, value: T) = FlowEventPublisher.postInternal(EventKey(key, T::class), value)

    /**
     * 发布普通事件（非粘性），适用于协程环境中的事件发送。
     *
     * 与 [post] 功能相同，但为挂起函数，适合在协程中调用。
     * 若当前没有订阅者，则事件会被丢弃，不会进行缓存。
     *
     * @param key 事件唯一标识符
     * @param value 要发送的事件数据
     *
     * @see post 同步版本
     * @see postStickySuspend 粘性挂起版本
     */
    suspend inline fun <reified T : Any> postSuspend(key: String, value: T) = FlowEventPublisher.postInternalSuspend(EventKey(key, T::class), value)

    /**
     * 发布粘性事件，事件将被缓存，下次订阅时可立即收到最近一次的事件数据。
     *
     * 该方法适用于需要保留事件最新状态、让后订阅者也能接收到的场景。
     * 如：配置更新、登录状态变更、主题切换等。
     *
     * 为非挂起函数，可在普通线程中调用。
     *
     * @param key 事件唯一标识符
     * @param value 要发送的事件数据
     *
     * @see post 普通事件版本
     * @see postStickySuspend 挂起版本
     */
    inline fun <reified T : Any> postSticky(key: String, value: T) = FlowEventPublisher.postStickyInternal(EventKey(key, T::class), value)

    /**
     * 发布粘性事件（挂起版本），适用于协程中发送粘性事件。
     *
     * 粘性事件将缓存最近一次的数据，允许后续订阅者立即接收该事件。
     * 适用于异步加载配置、网络状态广播等粘性数据分发场景。
     *
     * @param key 事件唯一标识符
     * @param value 要发送的事件数据
     *
     * @see postSticky 同步版本
     * @see postSuspend 普通挂起版本
     */
    suspend inline fun <reified T : Any> postStickySuspend(key: String, value: T) =
        FlowEventPublisher.postStickyInternalSuspend(EventKey(key, T::class), value)

    /**
     * 订阅普通事件（非粘性），只会接收订阅之后发送的事件。
     *
     * 当 [owner] 生命周期结束时，会自动取消订阅，避免内存泄漏。
     * 若事件在订阅前已经发送，则不会收到该事件。
     *
     * @param owner 生命周期绑定组件，如 Activity、Fragment
     * @param key 事件唯一标识符
     * @param onEvent 收到事件后的回调函数
     *
     * @see observeStickyEvent 粘性事件订阅
     */
    inline fun <reified T : Any> observeEvent(
        owner: LifecycleOwner, key: String, noinline onEvent: (T) -> Unit
    ) = FlowEventObserver.observeEvent(owner, EventKey(key, T::class), onEvent)

    /**
     * 订阅粘性事件，可立即收到最近一次事件（若已发布），并可指定默认值。
     *
     * 适用于需要关注“当前状态”的事件订阅场景，如主题、登录状态、配置更新等。
     * 当事件历史中不存在值时，若提供了 [default]，则会立即触发回调。
     *
     * @param owner 生命周期绑定组件
     * @param key 事件唯一标识符
     * @param default 默认值（可选），在历史事件不存在时使用
     * @param onEvent 收到事件后的回调函数
     *
     * @see observeEvent 普通事件订阅
     */
    inline fun <reified T : Any> observeStickyEvent(
        owner: LifecycleOwner, key: String, default: T? = null, noinline onEvent: (T) -> Unit
    ) = FlowEventObserver.observeStickyEvent(owner, EventKey(key, T::class), default, onEvent)

    /**
     * 设置 FlowBus 的日志策略，替换默认日志实现。
     *
     * 该方法建议在应用启动时调用，方便集成自定义日志框架或调整日志输出。
     *
     * @param customStrategy 自定义日志策略实现，用于统一日志打印和调试支持
     *
     * @see FlowBusLoggerStrategy
     */
    fun setLoggerStrategy(customStrategy: FlowBusLoggerStrategy) {
        FlowBusLogger.setLoggerStrategy(customStrategy)
    }

    /**
     * 移除某个特定类型的事件，包括其对应的事件流和缓存数据。
     *
     * 建议在事件不再使用时调用此方法，避免内存占用。
     * 可用于主动释放粘性事件或手动管理事件生命周期。
     *
     * @param key 事件唯一标识符
     */
    inline fun <reified T : Any> remove(key: String) = FlowBusCore.remove(EventKey(key, T::class))

    /**
     * 清除所有已注册的事件，包括普通事件和粘性事件。
     *
     * 适用于用户退出登录、应用重置等全局清理场景。
     * 清除后所有事件都将失效，需重新订阅。
     */
    fun clearAll() = FlowBusCore.clearAll()
}
