package com.example.sampleview.reddot.aggregator

import com.example.sampleview.reddot.core.RedDotStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapNotNull

/**
 * 聚合器注册中心，负责管理多个 RedDotAggregator，并协调其聚合逻辑。
 *
 * 提供注册、注销聚合器的方法，并支持基于路径的聚合观察（Flow）。
 * 每个 Aggregator 可以定义自己的匹配规则（matcher）和聚合逻辑（aggregate）。
 */
class AggregatorRegistry(private val repository: RedDotStateRepository) {

    /**
     * 存储所有注册的红点聚合器。
     * 使用泛型 `RedDotAggregator<*>`，支持不同类型的聚合结果。
     */
    private val aggregators = mutableListOf<RedDotAggregator<*>>()

    /**
     * 注册一个红点聚合器。
     *
     * @param aggregator 聚合器实例，用于处理特定路径的红点聚合逻辑。
     *                   支持对不同路径和不同类型的红点数据进行聚合。
     */
    fun register(aggregator: RedDotAggregator<*>) {
        aggregators.add(aggregator)
    }

    /**
     * 注销一个已注册的红点聚合器。
     *
     * @param aggregator 需要移除的聚合器实例。
     *                   移除后该聚合器不再参与状态聚合和观察。
     */
    fun unregister(aggregator: RedDotAggregator<*>) {
        aggregators.remove(aggregator)
    }

    /**
     * 观察第一个匹配指定路径的聚合器产生的聚合状态 Flow。
     *
     * 该方法会从所有注册的聚合器中查找第一个路径匹配的聚合器，
     * 并返回其基于本地红点状态仓库的聚合结果 Flow。
     *
     * 若无匹配聚合器，则返回空的 Flow。
     *
     * 注意：此处因 Kotlin 类型擦除，内部进行了不安全的类型转换，
     * 使用时应确保泛型 T 与聚合器实际返回类型匹配。
     *
     * @param path 用于匹配聚合器的路径（通常是 RedDotKey 的 fullKey）。
     * @return 匹配聚合器的聚合结果 Flow，类型为 T，或空 Flow。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> observeAggregateFlow(path: String): Flow<T> =
        aggregators.firstOrNull { it.matcher.matches(path) }
            ?.let { aggregator -> createAggregatorFlow(aggregator) as Flow<T> }
            ?: emptyFlow()

    /**
     * 观察所有匹配指定路径的聚合器产生的聚合状态 Flow 列表。
     *
     * 该方法返回所有路径匹配的聚合器对应的聚合状态 Flow 列表，
     * 可用于同时监听多个聚合维度的红点变化。
     *
     * 若无匹配聚合器，则返回空列表。
     *
     * 同样，调用时需要确保泛型 T 与聚合器的聚合结果类型对应。
     *
     * @param path 用于匹配聚合器的路径。
     * @return 匹配聚合器对应的聚合状态 Flow 列表，类型为 T。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> observeAggregateFlows(path: String): List<Flow<T>> =
        aggregators.filter { it.matcher.matches(path) }
            .map { aggregator ->
                createAggregatorFlow(aggregator) as Flow<T>
            }

    /**
     * 清空所有已注册的聚合器。
     *
     * 通常在组件销毁或重置时调用，释放资源，避免内存泄漏。
     */
    fun clear() {
        aggregators.clear()
    }

    /**
     * 基于指定聚合器和本地红点状态仓库，创建对应的聚合结果 Flow。
     *
     * 该 Flow 会根据仓库中红点状态变更实时触发，
     * 对匹配路径的红点数据进行过滤并执行聚合计算，
     * 并去重相邻相同值，保证订阅者只响应实际状态变更。
     *
     * @param aggregator 用于执行聚合计算的聚合器实例。
     * @return 聚合结果的 Flow，类型与聚合器泛型一致。
     */
    private fun <T> createAggregatorFlow(aggregator: RedDotAggregator<T>): Flow<T> {
        return repository.stateFlow
            .mapNotNull { redDotMap ->
                // 过滤符合聚合器路径匹配规则的红点数据
                val filtered = redDotMap.filter { (key, _) ->
                    aggregator.matcher.matches(key.fullKey)
                }
                // 执行聚合计算，返回聚合结果
                aggregator.aggregate(filtered)
            }
            // 仅在结果变化时触发下游，避免重复通知
            .distinctUntilChanged()
    }
}
