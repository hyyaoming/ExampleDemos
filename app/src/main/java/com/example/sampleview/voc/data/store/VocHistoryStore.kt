package com.example.sampleview.voc.data.store

import com.example.sampleview.voc.data.model.VocScene

class VocHistoryStore : VocStore {
    private val _history = mutableListOf<VocScene>()

    override suspend fun getHistory(): List<VocScene> {
        return _history
    }

    override suspend fun saveHistory(history: List<VocScene>) {
        _history.addAll(history)
    }
}