package com.example.sampleview.reddot.usecase

import com.example.sampleview.AppLogger
import com.example.sampleview.reddot.core.RedDotStateRepository
import com.example.sampleview.reddot.datasource.RedDotSourceRegistry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 红点数据刷新器，负责从已注册的数据源中拉取最新红点状态数据，
 * 并统一更新到本地红点状态仓库中。
 *
 * 通过 [refreshAll] 可批量刷新所有已注册数据源，
 * 通过 [refreshModule] 可针对单个模块进行刷新。
 *
 * 内部通过互斥锁（[Mutex]）保证刷新过程的线程安全，防止并发刷新导致状态混乱。
 *
 * @property registry 红点数据源注册中心，管理所有模块对应的数据源
 * @property repository 红点状态仓库，缓存和维护当前红点状态
 */
class RedDotRefresher(
    private val registry: RedDotSourceRegistry,
    private val repository: RedDotStateRepository,
) {
    /** 互斥锁，用于同步刷新操作，防止并发刷新冲突 */
    private val mutex = Mutex()

    /**
     * 批量刷新所有已注册数据源的红点状态。
     *
     * 依次从每个数据源异步拉取最新红点数据，忽略拉取失败的源，
     * 并将所有成功拉取的数据合并后更新到本地仓库。
     */
    suspend fun refreshAll() = mutex.withLock {
        val mergedStates = registry.allSources()
            .mapNotNull { source ->
                runCatching { source.fetch() }
                    .onFailure { AppLogger.w("RedDotRefresher", "Failed to refresh source for module: ${source.javaClass.simpleName}") }
                    .getOrNull()
            }
            // 将多模块红点列表合并为单个集合
            .flatten()
            // 使用红点 Key 去重，保留最后一次出现的数据
            .associateBy { it.key }
        repository.updateStates(mergedStates)
    }

    /**
     * 刷新指定模块对应的数据源红点状态。
     *
     * @param module 模块名，用于定位对应的数据源
     */
    suspend fun refreshModule(module: String) = mutex.withLock {
        val source = registry.getSource(module) ?: return
        // 拉取指定模块数据，拉取失败时不更新
        val states = runCatching { source.fetch() }
            .onFailure { AppLogger.w("RedDotRefresher", "Fetch failed for module: $module") }
            .getOrNull() ?: return
        // 更新本地状态仓库
        repository.updateStates(states.associateBy { it.key })
    }
}
