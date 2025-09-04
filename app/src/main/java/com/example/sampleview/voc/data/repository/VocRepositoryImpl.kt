package com.example.sampleview.voc.data.repository

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene
import kotlinx.coroutines.delay

class VocRepositoryImpl : VocRepository {
    override suspend fun getQuestions(scene: VocScene): Question {
        delay(200)
        return Question(
            VocScene.CANCEL_ORDER.scene, "您将退出呼叫，有没有遇到问题", "您的反馈能让我们做的更好", true, listOf(
                "暂时不用车", "上下车点不准确", "车费太贵", "对费用有疑问"
            )
        )
    }

    override suspend fun submitVoc(answer: Answer): Boolean {
        return false
    }
}