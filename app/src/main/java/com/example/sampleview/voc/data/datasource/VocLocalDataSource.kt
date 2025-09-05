package com.example.sampleview.voc.data.datasource

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene
import kotlinx.coroutines.delay

/**
 * 本地问卷数据源实现。
 *
 * 负责从本地存储提供问卷数据及提交答案的能力。
 * 当前示例为内存模拟实现，可根据需要扩展为数据库或文件存储。
 */
class VocLocalDataSource : VocDataSource {

    /**
     * 获取指定场景的问卷问题。
     *
     * @param scene 问卷场景 [VocScene]
     * @return 根据实际存储实现返回 [Question]
     */
    override suspend fun getQuestions(scene: VocScene): Question? {
        delay(500)
        return null
    }

    /**
     * 提交用户填写的问卷答案。
     *
     * @param answer 用户答案 [Answer]
     * @return true 表示提交成功
     */
    override suspend fun submitVoc(answer: Answer): Boolean {
        delay(600)
        return true
    }
}
