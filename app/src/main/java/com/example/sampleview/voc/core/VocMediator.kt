package com.example.sampleview.voc.core

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.voc.core.VocMediator.Companion.with
import com.example.sampleview.voc.data.datasource.VocDataSource
import com.example.sampleview.voc.data.datasource.VocLocalDataSource
import com.example.sampleview.voc.data.model.VocResult
import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.data.store.VocHistoryStore
import com.example.sampleview.voc.data.store.VocStore
import com.example.sampleview.voc.strategy.VocShowStrategy
import com.example.sampleview.voc.ui.VocUI
import com.example.sampleview.voc.ui.dialog.DialogVocUI

/**
 * 问卷中介器（VocMediator）。
 *
 * 作用：提供统一入口触发问卷展示并收集用户操作结果。
 *
 * 流程：
 * 1. 使用 [with] 绑定 Activity 并创建 Mediator。
 * 2. 调用 [trigger] 指定场景和可选配置块。
 * 3. 根据场景和策略决定是否展示问卷，展示 UI 后等待用户操作。
 * 4. 返回 [VocResult]，包括 Success、Cancelled、AlreadyShown 或 None。
 */
class VocMediator private constructor(private val activity: FragmentActivity) {

    /**
     * 触发问卷收集流程。
     *
     * @param scene 当前场景 [VocScene]。
     * @param block 可选配置块，用于修改 [Builder] 属性。
     * @return 最终的 [VocResult]。
     */
    suspend fun trigger(scene: VocScene, block: Builder.() -> Unit = {}): VocResult {
        val builder = Builder().apply(block)
        return VocCollector(builder).collect(scene, activity)
    }

    /**
     * Builder 配置类，用于自定义问卷展示策略、UI、数据源和历史记录存储。
     */
    class Builder(
        var vocStrategy: VocShowStrategy? = null,
        var vocUI: VocUI = DialogVocUI(),
        var dataSource: VocDataSource = VocLocalDataSource(),
        var historyStore: VocStore = VocHistoryStore(),
    )

    companion object {
        /**
         * 创建 [VocMediator] 并绑定 Activity。
         *
         * @param activity 当前用于展示问卷的 [FragmentActivity]。
         * @return 配置完成的 [VocMediator]。
         */
        fun with(activity: FragmentActivity): VocMediator {
            return VocMediator(activity)
        }
    }
}
