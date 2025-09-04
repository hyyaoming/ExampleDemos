package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene

interface VocShowStrategy {
    fun shouldShow(scene: VocScene, history: List<VocScene>): Boolean
}