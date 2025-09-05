package com.example.sampleview.voc.core

import com.example.sampleview.AppLogger
import com.example.sampleview.voc.data.model.VocAction
import com.example.sampleview.voc.data.model.VocResult
import com.example.sampleview.voc.data.repository.VocRepository
import com.example.sampleview.voc.data.store.VocStore
import com.example.sampleview.voc.ui.VocUI

/**
 * 问卷操作执行器，根据不同的 [VocAction] 执行相应逻辑。
 *
 * 负责处理提交答案、取消问卷等操作，并协调仓库、历史记录和 UI。
 *
 * @property builder 用于访问问卷相关依赖，如 [VocRepository]、历史记录存储 [VocStore] 和 UI 控件 [VocUI]。
 * @property repository 数据仓库，用于提交问卷答案。
 */
internal class ActionHandler(
    private val builder: VocMediator.Builder,
    private val repository: VocRepository,
) {

    /**
     * 执行指定的问卷操作。
     *
     * 支持的操作：
     * - [VocAction.SubmitAnswer]：提交答案到仓库，成功后保存历史记录并关闭问卷 UI。
     * - [VocAction.CancelSurvey]：取消问卷并关闭问卷 UI。
     *
     * 提交答案时，若发生异常会被捕获并打印日志，同时返回 `null` 表示提交失败。
     *
     * @param action 要处理的 [VocAction]
     * @return [VocResult.Success] 如果提交成功，
     *         [VocResult.Cancelled] 如果操作是取消问卷，
     *         `null` 如果提交失败或发生异常。
     */
    suspend fun handle(action: VocAction): VocResult? {
        return when (action) {
            is VocAction.SubmitAnswer -> runCatching {
                val success = repository.submitVoc(action.answer)
                if (success) {
                    builder.historyStore.saveHistory(listOf(action.vocScene))
                    builder.vocUI.dismiss()
                    VocResult.Success(action.answer)
                } else {
                    AppLogger.w("ActionHandler", "submitVoc failed, retry allowed")
                    null
                }
            }.onFailure { e ->
                AppLogger.e("ActionHandler", "submitVoc exception: ${e.message}", e)
            }.getOrNull()
            is VocAction.CancelSurvey -> {
                builder.vocUI.dismiss()
                VocResult.Cancelled(action.reason)
            }
        }
    }
}
