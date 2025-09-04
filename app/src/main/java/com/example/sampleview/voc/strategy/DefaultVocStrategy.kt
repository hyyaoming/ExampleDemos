package com.example.sampleview.voc.strategy

import com.example.sampleview.voc.data.model.VocScene

object DefaultVocStrategy : VocShowStrategy {
    override fun shouldShow(scene: VocScene, history: List<VocScene>) = !history.contains(scene)
}