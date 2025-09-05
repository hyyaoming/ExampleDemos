package com.example.sampleview.voc.data.datasource

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene

/**
 * 问卷数据源接口。
 *
 * 负责提供问卷数据访问能力，包括获取问题和提交答案。
 * 具体实现可以是本地存储、网络接口或混合来源。
 */
interface VocDataSource {

    /**
     * 获取指定场景的问卷问题。
     *
     * @param scene 问卷场景 [VocScene]
     * @return 对应场景的问题 [Question]，如果不存在返回 null
     */
    suspend fun getQuestions(scene: VocScene): Question?

    /**
     * 提交用户填写的问卷答案。
     *
     * @param answer 用户答案 [Answer]
     * @return true 表示提交成功，false 表示失败
     */
    suspend fun submitVoc(answer: Answer): Boolean
}
