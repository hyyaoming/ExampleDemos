package com.example.sampleview.reddot.usecase

import com.example.sampleview.AppLogger
import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey
import com.example.sampleview.reddot.core.RedDotStateRepository
import com.example.sampleview.reddot.core.RedDotValue
import com.xnhz.libbase.reddot.usecase.RedDotReporter

/**
 * 红点“已读”状态提交用例（Use Case）。
 *
 * 封装了从业务角度处理红点“标记为已读”的完整流程：包括过滤、上报、状态更新三个阶段。
 *
 * 通常用于点击红点 UI 元素后，将红点状态更新为“已读”，并同步该状态至远程服务端。
 * 该类遵循领域驱动设计（DDD）中 UseCase 的职责边界：不关心状态的来源或 UI 展示，
 * 仅处理一次完整的“标记为已读”业务操作。
 *
 * @param repository 本地红点状态仓库，用于获取当前红点状态与更新状态
 * @param reporter 红点状态上报器，负责将“已读”状态同步给服务端或相关模块
 */
class RedDotMarkReadUseCase(
    private val repository: RedDotStateRepository,
    private val reporter: RedDotReporter,
) {

    /**
     * 提交一组红点 Key 的“已读”状态。
     *
     * 此函数会自动执行以下流程：
     * 1. 过滤出仍处于“需要展示红点”状态的 Key（调用 repository 判断）
     * 2. 对这些有效红点进行上报（reporter.reportRead）
     * 3. 上报成功的红点，在本地标记为“已读”（即将状态置为 RedDotValue.Empty）
     *
     * 通常在用户点击红点标识或访问某个模块/页面后调用，用于清除相关红点提示。
     *
     * @param keys 待提交为已读的红点 Key 列表
     * @param extraInfo 可选附加信息（用于上报，如用户行为上下文、日志标记等）
     * @return 成功上报并更新本地状态的 Key 列表
     */
    suspend fun submitReadStatus(keys: List<RedDotKey>, extraInfo: Map<String, Any>? = null): List<RedDotKey> {
        // 过滤出当前状态下可见且需要显示的红点
        val visibleKeys = keys.filter { repository.getState(it).isVisible() }
        if (visibleKeys.isEmpty()) return emptyList()
        AppLogger.d("RedDotMarkReadUseCase", "提交已读状态：${visibleKeys}")

        // 调用上报组件上报已读状态，获取成功上报的红点列表
        val successKeys = reporter.reportRead(visibleKeys, extraInfo)

        // 将成功上报的红点状态更新为已读（空红点），通知本地状态仓库更新
        if (successKeys.isNotEmpty()) {
            val updates = successKeys.associateWith { RedDotData(it, RedDotValue.Empty) }
            repository.updateStates(updates)
        }
        AppLogger.d("RedDotMarkReadUseCase", "成功上报已读：${successKeys}")
        return successKeys
    }

    private fun RedDotData?.isVisible(): Boolean =
        this?.redDotValue?.shouldShow() == true

}
