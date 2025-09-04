package com.example.sampleview.voc.data.repository

import com.example.sampleview.voc.data.model.Answer
import com.example.sampleview.voc.data.model.Question
import com.example.sampleview.voc.data.model.VocScene

interface VocRepository {
    suspend fun getQuestions(scene: VocScene): Question

    suspend fun submitVoc(answer: Answer): Boolean
}