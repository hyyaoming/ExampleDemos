package com.example.sampleview.voc.data.repository

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene
import kotlinx.coroutines.delay

/**
 * 问卷数据仓库默认实现（VocRepositoryImpl）。
 *
 * 当前为示例实现：
 * - [getQuestions] 根据当前 VocScene 获取问卷，可能该业务当前没有配置，那么就返回 null
 * - [submitVoc] 问卷提交结果
 *
 * 在实际项目中应替换为真实的数据源实现。
 */
class VocRepositoryImpl : VocRepository {

    /**
     * 获取问卷问题
     *
     * @param scene 问卷所属场景 [VocScene]。
     * @return 总是返回 null。
     */
    override suspend fun getQuestions(scene: VocScene): Question? {
        delay(200)
        return null
    }

    /**
     * 提交问卷答案
     *
     * @param answer 用户填写的答案 [Answer]。
     * @return 总是返回 false。
     */
    override suspend fun submitVoc(answer: Answer): Boolean {
        return false
    }
}
