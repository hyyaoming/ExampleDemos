package com.example.sampleview.voc.data.model

/**
 * 问卷操作类型，用于表示用户在问卷中的行为。
 *
 * 包含两种操作：
 * 1. [SubmitAnswer]：提交问卷答案。
 * 2. [CancelSurvey]：取消问卷。
 */
sealed interface VocAction {

    /**
     * 用户提交答案的操作。
     *
     * @property answer 用户填写的 [Answer]。
     * @property vocScene 问卷所属场景 [VocScene]。
     */
    data class SubmitAnswer(val answer: Answer, val vocScene: VocScene) : VocAction

    /**
     * 用户取消问卷的操作。
     *
     * @property reason 用户取消的原因说明。
     */
    data class CancelSurvey(val reason: String) : VocAction
}
