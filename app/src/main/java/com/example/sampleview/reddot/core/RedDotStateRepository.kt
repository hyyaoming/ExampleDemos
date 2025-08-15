package com.example.sampleview.reddot.core

import com.example.sampleview.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.collections.iterator

/**
 * 红点状态仓库，负责维护应用内所有红点的最新状态。
 *
 * 使用 [MutableStateFlow] 持有当前所有红点状态的映射，
 * 以便支持多处对红点状态的实时监听和数据更新。
 *
 * 数据结构：
 * - key：唯一标识红点的 [RedDotKey]
 * - value：对应的红点状态数据 [RedDotData]
 *
 * 提供基于键的单个红点状态查询和观察接口，
 * 以及批量状态更新和仓库清空功能。
 */
class RedDotStateRepository {

    /** 私有的可变状态流，内部存储红点键值对映射，初始为空映射 */
    private val _stateFlow = MutableStateFlow<Map<RedDotKey, RedDotData>>(emptyMap())

    /**
     * 公开的不可变状态流，外部订阅此流可实时监听所有红点状态变化。
     * 当仓库中任意红点状态发生变更时，都会触发此流的更新。
     */
    val stateFlow: StateFlow<Map<RedDotKey, RedDotData>> get() = _stateFlow

    /**
     * 查询指定红点键的当前状态数据。
     *
     * @param key 红点唯一标识符
     * @return 返回对应的 [RedDotData]，若仓库中无此键对应数据则返回 null
     *
     * 注意：返回值为仓库当前快照数据，不具备响应式监听能力。
     */
    fun getState(key: RedDotKey): RedDotData? = _stateFlow.value[key]

    /**
     * 订阅指定红点键对应的状态变化流。
     * 每当该红点的数据发生变化时，会通过 [Flow] 发送最新的数据。
     *
     * 如果该键在仓库中不存在，则流会发送 null。
     *
     * @param key 需要观察的红点唯一键
     * @return [Flow]，发射指定键对应的红点状态或 null
     *
     * 该流经过 distinctUntilChanged() 处理，
     * 避免发送重复的相同数据，保证下游只响应状态变更。
     */
    fun observeRedDot(key: RedDotKey): Flow<RedDotData?> =
        stateFlow.map { stateMap -> stateMap[key] }
            .distinctUntilChanged()

    /**
     * 批量更新红点状态，合并传入的新状态，覆盖旧值。
     *
     * 仅当有实际变化时才更新状态流，避免无效刷新。
     * 空状态或无变化时不做任何操作。
     *
     * @param newStates 待更新的红点键值对
     */
    fun updateStates(newStates: Map<RedDotKey, RedDotData>) {
        if (newStates.isEmpty()) return
        runCatching {
            _stateFlow.update { currentState ->
                var changed = false
                // 复制当前状态，避免直接修改原始不可变数据
                val merged = currentState.toMutableMap()
                // 仅替换实际有变化的条目，减少无谓的状态更新和流发射
                for ((key, newValue) in newStates) {
                    val oldValue = currentState[key]
                    if (oldValue != newValue) {
                        merged[key] = newValue
                        changed = true
                    }
                }
                // 有变化则返回合并的新状态，否则返回旧状态，避免多余的状态通知
                if (changed) merged else currentState
            }
        }.onFailure {
            AppLogger.e("RedDotStateRepository", "更新红点失败:$newStates", it)
        }
    }

    /**
     * 清空仓库中的所有红点状态数据，
     * 恢复为初始空状态。
     *
     * 通常在用户登出、数据重置或清理时调用。
     */
    fun clear() {
        _stateFlow.value = emptyMap()
    }
}
