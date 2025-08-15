package com.example.sampleview.reddot

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.reddot.usecase.RedDotMarkReadUseCase
import com.example.sampleview.reddot.usecase.RedDotRefresher
import com.example.sampleview.reddot.aggregator.AggregatorRegistry
import com.example.sampleview.reddot.aggregator.RedDotAggregator
import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey
import com.example.sampleview.reddot.core.RedDotStateRepository
import com.example.sampleview.reddot.datasource.IRedDotDataSource
import com.example.sampleview.reddot.datasource.RedDotSourceRegistry
import com.xnhz.libbase.reddot.usecase.RedDotReporter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 全局红点管理器，作为整个红点框架的统一入口。
 *
 * 负责管理红点数据源（数据的获取与上报）、红点状态仓库（数据的缓存和观察）、
 * 红点聚合器（对红点数据进行逻辑聚合）等核心组件。
 *
 * 同时提供数据刷新、红点状态观察、已读状态提交、注册/注销数据源及聚合器的功能。
 *
 * 通过该管理器，可以方便地完成红点系统的集成和扩展。
 */
object RedDotManager {

    /** 注册的红点数据源管理器，负责维护模块与对应数据源的关系 */
    private val registry = RedDotSourceRegistry()

    /** 红点状态仓库，缓存当前所有红点数据，并支持观察数据变更 */
    private val repository = RedDotStateRepository()

    /** 红点聚合器注册表，管理多个聚合器，实现不同规则的红点聚合计算 */
    private val aggregatorRegistry = AggregatorRegistry(repository)

    /** 负责触发所有数据源刷新请求，保证红点状态及时更新 */
    private val refresher = RedDotRefresher(registry, repository)

    /** 负责将已读红点状态上报至远端服务器或业务系统 */
    private val reporter = RedDotReporter(registry)

    /** 业务层用例，封装已读红点提交的流程与逻辑 */
    private val markReadUseCase = RedDotMarkReadUseCase(repository, reporter)

    /**
     * 注册一个红点数据源，供指定模块使用。
     *
     * @param moduleName 模块名称，作为该数据源的唯一标识
     * @param source 实现了数据获取和上报的红点数据源实例
     */
    fun registerDataSource(moduleName: String, source: IRedDotDataSource) = registry.register(moduleName, source)

    /**
     * 注销指定模块的红点数据源，释放资源及相关监听。
     *
     * @param moduleName 模块名称
     */
    fun unregisterDataSource(moduleName: String) = registry.unregister(moduleName)

    /**
     * 注册一个红点聚合器，用于对特定路径或规则下的红点数据进行聚合处理。
     *
     * 聚合器会根据自身的匹配规则筛选对应的红点，并执行自定义的聚合逻辑。
     *
     * @param aggregator 实现聚合逻辑的聚合器对象
     */
    fun registerAggregator(aggregator: RedDotAggregator<*>) = aggregatorRegistry.register(aggregator)

    /**
     * 注销已注册的聚合器，停止相关聚合处理。
     *
     * @param aggregator 要注销的聚合器实例
     */
    fun unregisterAggregator(aggregator: RedDotAggregator<*>) = aggregatorRegistry.unregister(aggregator)

    /**
     * 异步刷新所有注册的数据源，触发网络请求或其他异步操作，保证红点状态最新。
     *
     * 调用后，数据源会拉取最新的红点状态，更新状态仓库。
     */
    suspend fun refreshAllDataSources() = refresher.refreshAll()

    /**
     * 异步刷新指定模块的数据源，仅更新该模块的红点状态。
     *
     * @param moduleName 需要刷新的模块名称
     */
    suspend fun refreshModuleDataSource(moduleName: String) = refresher.refreshModule(moduleName)

    /**
     * 异步刷新指定模块的数据源，仅更新该模块的红点状态。
     *
     * @param moduleName 需要刷新的模块名称
     */
    fun refreshModuleDataSource(lifecycleOwner: LifecycleOwner, moduleName: String) {
        lifecycleOwner.lifecycleScope.launch {
            refresher.refreshModule(moduleName)
        }
    }

    /**
     * 观察指定红点 Key 的状态变化，返回一个可订阅的 Flow。
     *
     * 订阅该 Flow 后，可以实时接收到对应红点的状态更新（可能为 null 表示无数据）。
     *
     * @param key 红点的唯一标识 Key
     * @return 对应红点数据的 Flow，支持协程异步订阅
     */
    fun observeRedDotFlow(key: RedDotKey): Flow<RedDotData?> = repository.observeRedDot(key)

    /**
     * 观察第一个匹配指定路径的聚合红点状态流。
     *
     * 会根据传入的路径匹配聚合器，并订阅该聚合器计算得到的聚合结果。
     *
     * @param path 作为匹配依据的路径字符串
     * @return 聚合后的红点状态流，泛型由聚合器定义
     */
    fun <T> observeAggregateFlow(path: String) = aggregatorRegistry.observeAggregateFlow<T>(path)

    /**
     * 观察所有匹配指定路径的聚合红点状态流列表。
     *
     * 用于同时订阅多个符合匹配条件的聚合器，获取多个聚合结果。
     *
     * @param path 匹配聚合器的路径字符串
     * @return 聚合结果的 Flow 列表，泛型由聚合器定义
     */
    fun <T> observeAggregateFlows(path: String) = aggregatorRegistry.observeAggregateFlows<T>(path)

    /**
     * 提交单个红点为“已读”状态。
     *
     * 该方法会将单个红点 Key 包装为列表并调用批量提交接口。
     *
     * @param key 要提交为已读的红点 Key
     * @param extraInfo 可选的额外上下文参数，如设备信息、用户行为等，用于服务端或日志统计
     * @param List 上报服务器成功的红点数据
     */
    suspend fun submitReadStatus(key: RedDotKey, extraInfo: Map<String, Any>? = null): List<RedDotKey> {
        return submitReadStatus(listOf(key), extraInfo)
    }

    /**
     * 提交一组红点为“已读”状态。
     *
     * 支持传入额外的上下文信息，以便服务端或日志系统接收更多业务相关数据。
     *
     * @param keys 要提交的红点 Key 列表
     * @param extraInfo 额外的上下文参数（如设备信息、用户操作等）
     * @param List 上报服务器成功的红点数据
     */
    suspend fun submitReadStatus(keys: List<RedDotKey>, extraInfo: Map<String, Any>? = null): List<RedDotKey> {
        return markReadUseCase.submitReadStatus(keys, extraInfo)
    }

    /**
     * 清理所有红点状态数据、已注册的聚合器和数据源。
     *
     * 通常用于应用退出、用户切换或彻底重置红点系统时调用。
     */
    fun clearAll() {
        repository.clear()
        aggregatorRegistry.clear()
        registry.clear()
    }
}
