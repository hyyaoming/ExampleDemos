package com.example.sampleview.voc.data.repository

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene

/**
 * 问卷数据仓库接口。
 *
 * 提供获取问卷问题和提交问卷答案的能力。
 */
interface VocRepository {

    /**
     * 根据场景获取问卷问题。
     *
     * @param scene 问卷所属场景 [VocScene]。
     * @return 对应的问卷问题 [Question]，若不存在则返回 null。
     */
    suspend fun getQuestions(scene: VocScene): Question?

    /**
     * 提交用户问卷答案。
     *
     * @param answer 用户填写的答案 [Answer]。
     * @return 提交是否成功，true 表示成功，false 表示失败。
     */
    suspend fun submitVoc(answer: Answer): Boolean
}
