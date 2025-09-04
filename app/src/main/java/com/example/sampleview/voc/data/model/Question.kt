package com.example.sampleview.voc.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Question(
    val scene: String,
    val title: String,
    val subTitle: String,
    val canClose: Boolean,
    val options: List<String>,
) : Parcelable