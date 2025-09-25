package com.example.sampleview.ad

import android.widget.FrameLayout

interface AdDisplay {
    fun display(container: FrameLayout, listener: AdListener)
}