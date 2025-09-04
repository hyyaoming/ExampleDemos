package com.example.sampleview.voc.data.store

import com.example.sampleview.voc.data.model.VocScene

interface VocStore {
    suspend fun getHistory(): List<VocScene>
    suspend fun saveHistory(history: List<VocScene>)
}
