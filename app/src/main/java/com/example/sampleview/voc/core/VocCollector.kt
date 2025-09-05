package com.example.sampleview.voc.core

import androidx.fragment.app.FragmentActivity
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocAction
import com.example.sampleview.voc.data.model.VocResult
import com.example.sampleview.voc.data.model.VocScene
import com.example.sampleview.voc.data.repository.VocRepositoryImpl
import com.example.sampleview.voc.strategy.VocStrategyRegistry
import com.example.sampleview.voc.ui.VocIntent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * 问卷收集器，用于根据指定场景展示问卷并收集用户操作结果。
 *
 * 主要流程：
 * 1. 获取当前场景对应的问题；
 * 2. 判断是否需要展示问卷；
 * 3. 调用 UI 显示问卷；
 * 4. 等待用户操作并返回最终结果。
 *
 * @property builder 用于访问问卷相关依赖，如仓库、历史记录存储、UI 控件和策略。
 */
internal class VocCollector(private val builder: VocMediator.Builder) {

    /** 策略注册器，用于根据场景获取对应的问卷展示策略 */
    private val strategyRegistry = VocStrategyRegistry()

    /** 问卷数据仓库，用于获取问题和提交答案 */
    private val vocRepository = VocRepositoryImpl(builder.dataSource)

    /** 问卷操作执行器，根据不同的 [VocAction] 执行相应逻辑 */
    private val actionHandler = ActionHandler(builder, vocRepository)

    /**
     * 根据指定场景收集问卷结果。
     *
     * 流程：
     * 1. 获取场景问题；
     * 2. 判断是否需要展示问卷，如果已展示过则直接返回 [VocResult.AlreadyShown]；
     * 3. 展示问卷 UI；
     * 4. 等待用户操作结果并返回。
     *
     * @param scene 当前场景 [VocScene]
     * @param activity 当前显示问卷的 [FragmentActivity]
     * @return [VocResult]，可能为：
     * - [VocResult.Success]：用户成功提交答案；
     * - [VocResult.Cancelled]：用户取消问卷；
     * - [VocResult.AlreadyShown]：问卷已展示过；
     * - [VocResult.None]：未获取到问题。
     */
    suspend fun collect(scene: VocScene, activity: FragmentActivity): VocResult {
        val question = getQuestion(scene) ?: return VocResult.None
        if (!shouldShowVoc(scene)) return VocResult.AlreadyShown
        builder.vocUI.show(activity, question)
        return waitForResult(scene)
    }

    /**
     * 获取指定场景的问卷问题。
     *
     * @param scene 当前场景 [VocScene]
     * @return 对应的 [Question]，若未找到返回 null
     */
    private suspend fun getQuestion(scene: VocScene): Question? {
        return vocRepository.getQuestions(scene)
    }

    /**
     * 判断指定场景的问卷是否需要展示。
     *
     * 结合历史记录和策略来决定是否展示。
     *
     * @param scene 当前场景 [VocScene]
     * @return `true` 表示需要展示，`false` 表示无需展示
     */
    private suspend fun shouldShowVoc(scene: VocScene): Boolean {
        val strategy = builder.vocStrategy ?: strategyRegistry.getStrategy(scene)
        return strategy.shouldShow(scene, builder.historyStore)
    }

    /**
     * 等待用户操作结果并返回最终 [VocResult]。
     *
     * 监听 UI 的意图流，转换为 [VocAction] 并交给 [ActionHandler] 处理，
     * 直到收到 [VocResult.Success] 或 [VocResult.Cancelled]。
     *
     * @param scene 当前场景 [VocScene]
     * @return 最终的问卷结果 [VocResult]
     */
    private suspend fun waitForResult(scene: VocScene): VocResult {
        return builder.vocUI.intents
            .map { intent -> toAction(intent, scene) }
            .mapNotNull { action -> actionHandler.handle(action) }
            .first { it is VocResult.Success || it is VocResult.Cancelled }
    }

    /**
     * 将 UI 意图转换为问卷操作。
     *
     * @param intent UI 发送的 [VocIntent]
     * @param vocScene 当前场景 [VocScene]
     * @return 转换后的 [VocAction]
     */
    private fun toAction(intent: VocIntent, vocScene: VocScene): VocAction = when (intent) {
        is VocIntent.Submit -> VocAction.SubmitAnswer(intent.answer, vocScene)
        is VocIntent.Cancel -> VocAction.CancelSurvey(intent.reason)
    }
}
